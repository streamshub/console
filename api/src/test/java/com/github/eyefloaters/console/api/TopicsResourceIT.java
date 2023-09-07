package com.github.eyefloaters.console.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicCollection.TopicIdCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.stubbing.Answer;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.test.AdminClientSpy;
import com.github.eyefloaters.console.test.TestHelper;
import com.github.eyefloaters.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.Kafka;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;

@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
@TestHTTPEndpoint(TopicsResource.class)
@TestProfile(TestPlainProfile.class)
class TopicsResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;
    TopicHelper topicUtils;
    String clusterId1;
    String clusterId2;
    ServerSocket randomSocket;

    @BeforeEach
    void setup() throws IOException {
        URI bootstrapServers = URI.create(deployments.getExternalBootstrapServers());
        randomSocket = new ServerSocket(0);
        URI randomBootstrapServers = URI.create("dummy://localhost:" + randomSocket.getLocalPort());

        topicUtils = new TopicHelper(bootstrapServers, config, null);
        topicUtils.deleteAllTopics();

        utils = new TestHelper(bootstrapServers, config, null);

        clusterId1 = utils.getClusterId();
        clusterId2 = UUID.randomUUID().toString();

        client.resources(Kafka.class).delete();
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka1", clusterId1, bootstrapServers))
            .create();
        // Second cluster is offline/non-existent
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka2", clusterId2, randomBootstrapServers))
            .create();
    }

    @AfterEach
    void teardown() throws IOException {
        if (randomSocket != null) {
            randomSocket.close();
        }
    }

    @Test
    void testListTopicsAfterCreation() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(clusterId1, topicNames, 1);

        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(topicNames.size()))
            .body("data.attributes.name", containsInAnyOrder(topicNames.toArray(String[]::new)));
    }

    @Test
    void testListTopicsWithKafkaUnavailable() {
        whenRequesting(req -> req.get("", clusterId2))
            .assertThat()
            .statusCode(is(Status.GATEWAY_TIMEOUT.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("504"))
            .body("errors.code", contains("5041"));
    }

    @Test
    void testListTopicsWithNameAndConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "configs,name")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.configs[0]", not(anEmptyMap()))
            .body("data.attributes.configs[0].findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testListTopicsWithNameAndAuthorizedOperationsIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "authorizedOperations,name")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.authorizedOperations", contains(iterableWithSize(greaterThan(0))));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndLatestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.offset", is(2))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .queryParam("offsetSpec", "earliest")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.offset", is(0))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testListTopicsWithPartitionsIncludedAndOffsetWithTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .queryParam("offsetSpec", first.toString())
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.offset", is(0))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(first.toString()));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndOffsetWithMaxTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .queryParam("offsetSpec", "maxTimestamp")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.offset", is(1))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(second.toString()));
    }

    @ParameterizedTest
    @CsvSource({
        "'name', 't1,t2,t3,t4,t5'",
        "'-name', 't5,t4,t3,t2,t1'"
    })
    void testListTopicsSortedByName(String sortParam, String expectedNameList) {
        String randomSuffix = UUID.randomUUID().toString();

        List<String> topicNames = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> "t" + i + "-" + randomSuffix)
                .collect(Collectors.toList());

        topicUtils.createTopics(clusterId1, topicNames, 1);

        String[] expectedNames = Stream.of(expectedNameList.split(","))
                .map(name -> name + "-" + randomSuffix)
                .toArray(String[]::new);

        whenRequesting(req -> req.queryParam("sort", sortParam).get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(topicNames.size()))
            .body("data.attributes.name", contains(expectedNames));
    }

    @ParameterizedTest
    @CsvSource({
        "'',  'max.message.bytes', '1000000,2000000,3000000'",
        "'-', 'max.message.bytes', '3000000,2000000,1000000'",
        "'',  'min.cleanable.dirty.ratio', '0.4,0.5,0.6'",
        "'-', 'min.cleanable.dirty.ratio', '0.6,0.5,0.4'",
        "'',  'compression.type', 'gzip,producer,zstd'",
        "'-', 'compression.type', 'zstd,producer,gzip'",
    })
    void testListTopicsSortedByConfigValue(String sortPrefix, String sortParam, String expectedValueList) {
        String randomSuffix = UUID.randomUUID().toString();
        String[] expectedValues = expectedValueList.split(",");

        IntStream.rangeClosed(0, 2)
            .forEach(i -> {
                String name = "t" + i + "-" + randomSuffix;
                Map<String, String> configs = Map.of(sortParam, expectedValues[i]);
                topicUtils.createTopics(clusterId1, List.of(name), 1, configs);
            });

        whenRequesting(req -> req
                .queryParam("sort", sortPrefix + "configs[" + sortParam + "]")
                .queryParam("fields[topics]", "name,configs")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(expectedValues.length))
            .body("data.attributes.configs.\"" + sortParam + "\".value", contains(expectedValues));
    }

    @ParameterizedTest
    @CsvSource({
        "configs[random.unknown.config]",
        "unknown"
    })
    void testListTopicsSortedByUnknownField(String sortKey) {
        List<String> topicNames = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        var topicIds = topicUtils.createTopics(clusterId1, topicNames, 1);
        String[] sortedIds = topicIds.values().stream().sorted().toArray(String[]::new);

        whenRequesting(req -> req
                .queryParam("sort", sortKey)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(topicNames.size()))
            .body("data.id", contains(sortedIds));
    }

    @Test
    void testDescribeTopicWithNameAndConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,configs")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes", is(aMapWithSize(2)))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes.configs", not(anEmptyMap()))
            .body("data.attributes.configs.findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testDescribeTopicWithAuthorizedOperations() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req.get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.authorizedOperations", is(notNullValue()))
            .body("data.attributes.authorizedOperations", hasSize(greaterThan(0)));
    }

    @Test
    void testDescribeTopicWithLatestOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req.get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.offset", is(2))
            .body("data.attributes.partitions[0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testDescribeTopicWithEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "earliest")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.offset", is(0))
            .body("data.attributes.partitions[0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testDescribeTopicWithTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", first.toString())
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.offset", is(0))
            .body("data.attributes.partitions[0].offset.timestamp", is(first.toString()));
    }

    @Test
    void testDescribeTopicWithMaxTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "maxTimestamp")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.offset", is(1))
            .body("data.attributes.partitions[0].offset.timestamp", is(second.toString()));
    }

    @Test
    void testDescribeTopicWithBadOffsetTimestamp() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        whenRequesting(req -> req
                .queryParam("offsetSpec", "Invalid Timestamp")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.title", contains("Invalid query parameter"))
            .body("errors.source.parameter", contains("offsetSpec"));
    }

    @Test
    void testDescribeTopicWithListOffetsFailure() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Answer<ListOffsetsResult> listOffsetsFailed = args -> {
            @SuppressWarnings("unchecked")
            Set<TopicPartition> partitions = args.getArgument(0, Map.class).keySet();
            KafkaFutureImpl<ListOffsetsResultInfo> failure = new KafkaFutureImpl<>();
            failure.completeExceptionally(new ApiException("EXPECTED TEST EXCEPTION"));

            return new ListOffsetsResult(partitions
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), p -> failure)));
        };

        AdminClientSpy.install(client -> {
            // Mock listOffsets
            doAnswer(listOffsetsFailed).when(client).listOffsets(anyMap());
        });

        whenRequesting(req -> req
                .queryParam("offsetSpec", "latest")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.meta.type", is("error"))
            .body("data.attributes.partitions[0].offset.detail", is("EXPECTED TEST EXCEPTION"));
    }

    @Test
    void testListTopicsWithDescribeTopicsFailure() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Answer<DescribeTopicsResult> describeTopicsFailed = args -> {
            TopicIdCollection topicCollection = args.getArgument(0);
            KafkaFutureImpl<TopicDescription> failure = new KafkaFutureImpl<>();
            failure.completeExceptionally(new ApiException("EXPECTED TEST EXCEPTION"));

            Map<Uuid, KafkaFuture<TopicDescription>> futures = topicCollection.topicIds()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), id -> failure));

            class Result extends DescribeTopicsResult {
                Result() {
                    super(futures, null);
                }
            }

            return new Result();
        };

        AdminClientSpy.install(client -> {
            // Mock describeTopics
            doAnswer(describeTopicsFailed)
                .when(client)
                .describeTopics(any(TopicIdCollection.class), any(DescribeTopicsOptions.class));
        });

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,authorizedOperations,partitions")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes", contains(not(hasKey("configs"))))
            .body("data.attributes.partitions", hasSize(1))
            .body("data.attributes.partitions[0].meta.type", is("error"))
            .body("data.attributes.partitions[0].detail", is("EXPECTED TEST EXCEPTION"))
            .body("data.attributes.authorizedOperations", hasSize(1))
            .body("data.attributes.authorizedOperations[0].meta.type", is("error"))
            .body("data.attributes.authorizedOperations[0].detail", is("EXPECTED TEST EXCEPTION"));
    }

    @Test
    void testListTopicsWithDescribeConfigsFailure() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Answer<DescribeConfigsResult> describeConfigsFailed = args -> {
            Collection<ConfigResource> resources = args.getArgument(0);

            KafkaFutureImpl<org.apache.kafka.clients.admin.Config> failure = new KafkaFutureImpl<>();
            failure.completeExceptionally(new ApiException("EXPECTED TEST EXCEPTION"));

            final Map<ConfigResource, KafkaFuture<org.apache.kafka.clients.admin.Config>> futures =
                    resources
                        .stream()
                        .collect(Collectors.toMap(Function.identity(), resource -> failure));

            class Result extends DescribeConfigsResult {
                Result() {
                    super(futures);
                }
            }

            return new Result();
        };

        AdminClientSpy.install(client -> {
            // Mock describeTopics
            doAnswer(describeConfigsFailed).when(client).describeConfigs(anyCollection());
        });

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,configs")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.configs", hasSize(1))
            .body("data.attributes.configs[0].meta.type", is("error"))
            .body("data.attributes.configs[0].detail", is("EXPECTED TEST EXCEPTION"));
    }

    @Test
    void testDescribeTopicWithNoSuchTopic() {
        whenRequesting(req -> req.get("{topicName}", clusterId1, Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

}
