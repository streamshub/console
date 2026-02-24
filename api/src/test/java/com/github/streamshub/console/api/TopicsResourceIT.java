package com.github.streamshub.console.api;

import java.io.StringReader;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeLogDirsResult;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListOffsetsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.LogDirDescription;
import org.apache.kafka.clients.admin.ReplicaInfo;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicCollection.TopicIdCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.eclipse.microprofile.config.Config;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jboss.logging.Logger;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.github.streamshub.console.api.security.ConsoleAuthenticationMechanism;
import com.github.streamshub.console.api.service.TopicService;
import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.KafkaSecurityConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils;
import com.github.streamshub.console.support.Identifiers;
import com.github.streamshub.console.test.AdminClientSpy;
import com.github.streamshub.console.test.LogCapture;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;
import com.github.streamshub.console.test.VarargsAggregator;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.kafka.entityoperator.EntityOperatorSpec;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;

@QuarkusTest
@TestHTTPEndpoint(TopicsResource.class)
@TestProfile(TestPlainProfile.class)
class TopicsResourceIT {

    static final Logger LOGGER = Logger.getLogger(TopicsResourceIT.class);

    static LogCapture auditLogCapture = LogCapture.with(logRecord -> logRecord
            .getLoggerName()
            .equals(ConsoleAuthenticationMechanism.class.getName()),
            Level.INFO);

    @Inject
    Config config;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    KubernetesClient client;

    @Inject
    Holder<SharedIndexInformer<Kafka>> kafkaInformer;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    @Named("KafkaTopics")
    Map<String, Map<String, Map<String, KafkaTopic>>> managedTopics;

    TestHelper utils;
    TopicHelper topicUtils;
    ConsumerUtils groupUtils;
    final String clusterName1 = "test-kafka1";
    String clusterId1;
    URI bootstrapServers1;
    String clusterId2;

    @BeforeAll
    static void initialize() {
        auditLogCapture.register();
    }

    @AfterAll
    static void cleanup() {
        auditLogCapture.deregister();
    }

    @BeforeEach
    void setup(TestInfo testInfo) {
        LOGGER.infof("Before test %s.%s (%s)",
            testInfo.getTestClass().orElseThrow().getName(),
            testInfo.getTestMethod().orElseThrow().getName(),
            testInfo.getDisplayName()
        );
        bootstrapServers1 = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        URI randomBootstrapServers = URI.create(consoleConfig.getKafka()
                .getCluster("default/test-kafka2")
                .map(k -> k.getProperties().get("bootstrap.servers"))
                .orElseThrow());

        topicUtils = new TopicHelper(bootstrapServers1, config);
        topicUtils.deleteAllTopics();

        groupUtils = new ConsumerUtils(config);

        utils = new TestHelper(bootstrapServers1, config);
        utils.resetSecurity(consoleConfig, false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        client.resources(KafkaTopic.class).inAnyNamespace().delete();

        auditLogCapture.records().clear();

        utils.apply(client, utils.buildKafkaResource(clusterName1, utils.getClusterId(), bootstrapServers1));
        // Second cluster is offline/non-existent
        utils.apply(client, utils.buildKafkaResource("test-kafka2", UUID.randomUUID().toString(), randomBootstrapServers));

        // Wait for the informer cache to be populated with all Kafka CRs
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Objects.equals(kafkaInformer.get().getStore().list().size(), 2));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
    }

    @AfterEach
    void teardown(TestInfo testInfo) {
        LOGGER.infof("After test %s.%s (%s)",
            testInfo.getTestClass().orElseThrow().getName(),
            testInfo.getTestMethod().orElseThrow().getName(),
            testInfo.getDisplayName()
        );
    }

    @Test
    void testListTopicsAfterCreation() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toList();

        topicUtils.createTopics(topicNames, 1);

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
        topicUtils.createTopics(List.of(topicName), 1);

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
        topicUtils.createTopics(List.of(topicName), 1);

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
        topicUtils.createTopics(List.of(topicName), 2);
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
            .body("data.attributes.partitions[0][0].offsets.latest.offset", is(2))
            .body("data.attributes.partitions[0][0].offsets.latest", not(hasKey("timestamp")));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2);
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
            .body("data.attributes.partitions[0][0].offsets.earliest.offset", is(0))
            .body("data.attributes.partitions[0][0].offsets.earliest", not(hasKey("timestamp")));
    }

    @Test
    void testListTopicsWithPartitionsIncludedAndOffsetWithTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2);

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
            .body("data.attributes.partitions[0][0].offsets.timestamp.offset", is(0))
            .body("data.attributes.partitions[0][0].offsets.timestamp.timestamp", is(first.toString()));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndOffsetWithMaxTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2);

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
            .body("data.attributes.partitions[0][0].offsets.maxTimestamp.offset", is(1))
            .body("data.attributes.partitions[0][0].offsets.maxTimestamp.timestamp", is(second.toString()));
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
                .toList();

        topicUtils.createTopics(topicNames, 1);

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
        "'totalLeaderLogBytes', 't2,t3,t4,t5,t1'",
        "'-totalLeaderLogBytes', 't1,t5,t4,t3,t2'"
    })
    void testListTopicsSortedByStorageWithPagination(String sortParam, String expectedNameList) {
        String randomSuffix = UUID.randomUUID().toString();

        List<String> topicNames = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> "t" + i + "-" + randomSuffix)
                .toList();

        topicUtils.createTopics(topicNames, 1);

        AdminClientSpy.install(adminClient -> {
            doAnswer(inv -> {
                DescribeLogDirsResult realResult = (DescribeLogDirsResult) inv.callRealMethod();
                Map<Integer, KafkaFuture<Map<String, LogDirDescription>>> promises = new HashMap<>();

                realResult.descriptions().forEach((nodeId, pendingDescriptions) -> {
                    promises.put(nodeId, pendingDescriptions.thenApply(descriptions -> descriptions
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                                var descr = e.getValue();
                                var replicaInfos = new HashMap<>(descr.replicaInfos());

                                // remove storage info for t1 to verify null can be sorted
                                replicaInfos.remove(new TopicPartition("t1-" + randomSuffix, 0));

                                replicaInfos.entrySet().stream()
                                    .filter(r -> r.getKey().topic().matches("^t[1-5]-.*"))
                                    .forEach(r -> {
                                        var replica = r.getValue();
                                        // use the topic number (t2, t3, etc) to generate a size
                                        var size = 1000 * Integer.parseInt(r.getKey().topic().substring(1, 2));

                                        r.setValue(new ReplicaInfo(
                                                size,
                                                replica.offsetLag(),
                                                replica.isFuture()));
                                    });

                                return new LogDirDescription(
                                        descr.error(),
                                        replicaInfos,
                                        descr.totalBytes().orElse(-1),
                                        descr.usableBytes().orElse(-1));
                            }))));
                });

                var result = Mockito.mock(DescribeLogDirsResult.class);
                Mockito.when(result.descriptions()).thenReturn(promises);
                return result;
            }).when(adminClient).describeLogDirs(Mockito.anyCollection(), any(DescribeLogDirsOptions.class));
        });

        List<String> expectedNames = Stream.of(expectedNameList.split(","))
                .map(name -> name + "-" + randomSuffix)
                .toList();

        // Page 1
        String response1 = whenRequesting(req -> req
                .queryParam("sort", sortParam)
                .queryParam("page[size]", "2")
                .queryParam("fields[topics]", "name,totalLeaderLogBytes")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(2))
            .body("data.attributes.name", contains(expectedNames.subList(0, 2).toArray(String[]::new)))
            .extract()
            .asString();

        JsonObject links1 = linkExtract(response1);

        // Advance to page 3, using `last` link from page 1
        String response2 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(URI.create(links1.getString("last"))))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1.getString("first"))),
                    hasEntry(is("prev"), notNullValue()),
                    hasEntry(is("next"), nullValue()),
                    hasEntry(is("last"), is(links1.getString("last")))))
            .body("data.size()", is(1))
            .body("data.attributes.name", contains(expectedNames.subList(4, 5).toArray(String[]::new)))
            .extract()
            .asString();

        JsonObject links2 = linkExtract(response2);
        whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(URI.create(links2.getString("prev"))))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1.getString("first"))),
                    hasEntry(is("prev"), notNullValue()),
                    hasEntry(is("next"), notNullValue()),
                    hasEntry(is("last"), is(links1.getString("last")))))
            .body("data.size()", is(2))
            .body("data.attributes.name", contains(expectedNames.subList(2, 4).toArray(String[]::new)))
            .extract()
            .asString();
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
                topicUtils.createTopics(List.of(name), 1, configs);
            });

        whenRequesting(req -> req
                .queryParam("sort", sortPrefix + "configs.\"" + sortParam + "\"")
                .queryParam("fields[topics]", "name,configs")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(expectedValues.length))
            .body("data.attributes.configs.\"" + sortParam + "\".value", contains(expectedValues));
    }

    @Test
    void testListTopicsSortedByRetentionMsWithPagination() {
        final int week = 604800000;
        String randomSuffix = UUID.randomUUID().toString();
        Map<String, String> topicIds = new HashMap<>();

        IntStream.range(0, 10).forEach(i -> {
            var names = List.of("t" + i + "-" + randomSuffix);
            var configs = Map.of("retention.ms", Integer.toString(week + i));
            topicIds.putAll(topicUtils.createTopics(names, 1, configs));
        });

        var fullResponse = whenRequesting(req -> req
                .queryParam("sort", "configs.\"retention.ms\"")
                .queryParam("fields[topics]", "name,configs")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(topicIds.size()))
            .body("data.size()", is(topicIds.size()))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))))
            .extract()
            .asInputStream();

        JsonObject responseJson;

        try (var reader = Json.createReader(fullResponse)) {
            responseJson = reader.readObject();
        }

        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put("sort", "configs.\"retention.ms\"");
        parametersMap.put("fields[topics]", "name,configs");

        // range request excluding first and last from full result set
        utils.getCursor(responseJson, 0)
            .ifPresent(cursor -> parametersMap.put("page[after]", cursor));
        utils.getCursor(responseJson, topicIds.size() - 1)
            .ifPresent(cursor -> parametersMap.put("page[before]", cursor));

        var rangeResponse = whenRequesting(req -> req
                .queryParams(parametersMap)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(topicIds.size())) // total is the count for the full/unpaged result set
            .body("data.size()", is(topicIds.size() - 2))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))))
            .extract()
            .asInputStream();

        JsonObject rangeResponseJson;

        try (var reader = Json.createReader(rangeResponse)) {
            rangeResponseJson = reader.readObject();
        }

        IntStream.range(1, 9).forEach(i -> {
            int responseIdx = i - 1;
            String actualName = utils.getValue(rangeResponseJson, JsonString.class, "/data/%d/attributes/name".formatted(responseIdx)).getString();
            assertThat(actualName, startsWith("t" + i + "-"));
            String retentionMs = utils.getValue(rangeResponseJson, JsonString.class, "/data/%d/attributes/configs/retention.ms/value".formatted(responseIdx)).getString();
            assertThat(retentionMs, is(Integer.toString(week + i)));
        });
    }

    @ParameterizedTest
    @CsvSource({
        "configs.\"random.unknown.config\"",
        "unknown"
    })
    void testListTopicsSortedByUnknownField(String sortKey) {
        List<String> topicNames = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toList();

        var topicIds = topicUtils.createTopics(topicNames, 1);
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
    void testListTopicsPaginationLinksWithDefaultPageSize() {
        List<String> topicNames = IntStream.range(0, 102)
                .mapToObj("%03d-"::formatted)
                .map(prefix -> prefix + UUID.randomUUID().toString())
                .toList();

        topicUtils.createTopics(topicNames, 1);

        // Page 1
        String response1 = whenRequesting(req -> req
                .queryParam("fields[topics]", "name")
                .queryParam("sort", "-name")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), notNullValue()),
                    hasEntry(is("prev"), nullValue()),
                    hasEntry(is("next"), notNullValue()),
                    hasEntry(is("last"), notNullValue())))
            .body("meta.page.total", is(102))
            .body("meta.page.pageNumber", is(1))
            .body("data.size()", is(10))
            .body("data[0].attributes.name", startsWith("101-"))
            .body("data[9].attributes.name", startsWith("092-"))
            .extract()
            .asString();

        JsonObject links1 = linkExtract(response1);
        String links1First = links1.getString("first");
        String links1Last = links1.getString("last");

        // Advance to page 2, using `next` link from page 1
        URI request2 = URI.create(links1.getString("next"));
        String response2 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request2))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1First)),
                    hasEntry(is("prev"), notNullValue()),
                    hasEntry(is("next"), notNullValue()),
                    hasEntry(is("last"), is(links1Last))))
            .body("meta.page.total", is(102))
            .body("meta.page.pageNumber", is(2))
            .body("data.size()", is(10))
            .body("data[0].attributes.name", startsWith("091-"))
            .body("data[9].attributes.name", startsWith("082-"))
            .extract()
            .asString();

        // Jump to final page 11 using `last` link from page 2
        JsonObject links2 = linkExtract(response2);
        URI request3 = URI.create(links2.getString("last"));
        String response3 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request3))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1First)),
                    hasEntry(is("prev"), notNullValue()),
                    hasEntry(is("next"), nullValue()),
                    hasEntry(is("last"), is(links1Last))))
            .body("meta.page.total", is(102))
            .body("meta.page.pageNumber", is(11))
            .body("data.size()", is(2))
            .body("data[0].attributes.name", startsWith("001-"))
            .body("data[1].attributes.name", startsWith("000-"))
            .extract()
            .asString();

        // Jump to previous page 10 using `prev` link from page 3
        JsonObject links3 = linkExtract(response3);
        URI request4 = URI.create(links3.getString("prev"));
        String response4 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request4))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1First)),
                    hasEntry(is("prev"), notNullValue()),
                    hasEntry(is("next"), is(links1Last)),
                    hasEntry(is("last"), is(links1Last))))
            .body("meta.page.total", is(102))
            .body("meta.page.pageNumber", is(10))
            .body("data.size()", is(10))
            .body("data[0].attributes.name", startsWith("011-"))
            .body("data[9].attributes.name", startsWith("002-"))
            .extract()
            .asString();

        // Return to page 1 using the `first` link provided by page 10
        JsonObject links4 = linkExtract(response4);
        URI request5 = URI.create(links4.getString("first"));
        String response5 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request5))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1First)),
                    hasEntry(is("prev"), nullValue()),
                    hasEntry(is("next"), notNullValue()),
                    hasEntry(is("last"), is(links1Last))))
            .body("meta.page.total", is(102))
            .body("meta.page.pageNumber", is(1))
            .body("data.size()", is(10))
            .body("data[0].attributes.name", startsWith("101-"))
            .body("data[9].attributes.name", startsWith("092-"))
            .extract()
            .asString();

        assertEquals(response1, response5);
    }

    @Test
    void testListTopicsPaginationLinksNullWithSinglePage() {
        List<String> topicNames = IntStream.range(0, 102)
                .mapToObj("%03d-"::formatted)
                .map(prefix -> prefix + UUID.randomUUID().toString())
                .toList();

        topicUtils.createTopics(topicNames, 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name")
                .queryParam("sort", "-name")
                .queryParam("page[size]", 103)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), nullValue()),
                    hasEntry(is("prev"), nullValue()),
                    hasEntry(is("next"), nullValue()),
                    hasEntry(is("last"), nullValue())))
            .body("meta.page.total", is(102))
            .body("meta.page.pageNumber", is(1))
            .body("data.size()", is(102))
            .body("data[0].attributes.name", startsWith("101-"))
            .body("data[101].attributes.name", startsWith("000-"));
    }

    @Test
    void testListTopicsWithConsumerGroupsLinkage() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic2 = "t2-" + UUID.randomUUID().toString();

        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String group2 = "g2-" + UUID.randomUUID().toString();
        String group2Id = Identifiers.encode(group2);

        String client1 = "c1-" + UUID.randomUUID().toString();
        String client2 = "c2-" + UUID.randomUUID().toString();

        try (var consumer1 = groupUtils.consume(group1, topic1, client1, 2, false);
             var consumer2 = groupUtils.consume(group2, topic2, client2, 2, false)) {
            whenRequesting(req -> req
                    .queryParam("fields[topics]", "name,groups")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(2))
                .body("data.find { it.attributes.name == '%s' }.relationships.groups.data[0]".formatted(topic1),
                    allOf(
                        hasEntry(equalTo("type"), equalTo("groups")),
                        hasEntry(equalTo("id"), equalTo(group1Id))))
                .body("data.find { it.attributes.name == '%s' }.relationships.groups.data[0]".formatted(topic2),
                    allOf(
                        hasEntry(equalTo("type"), equalTo("groups")),
                        hasEntry(equalTo("id"), equalTo(group2Id))));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "''           , 'wx.', 'external'",
        "'eq,external', 'wx.', 'external'",
        "'in,external', 'wx.', 'external'",
        "'eq,internal', '_x.', 'internal'",
        "'in,internal', '_x.', 'internal'"
    })
    void testListTopicsWithNameFilterVaryingVisibility(String visibilityFilter, String expectedPrefix, String expectedVisibility) {
        List<String> topicNames = IntStream.range(0, 6)
                .mapToObj(i -> (i % 2 == 0 ? "_x." : "wx.") + UUID.randomUUID().toString())
                .toList();

        topicUtils.createTopics(topicNames, 1);

        Map<String, String> params = new HashMap<>();
        params.put("filter[name]", "like,?x*"); // any single char, following by `x`, followed by anything
        if (!visibilityFilter.isBlank()) {
            params.put("filter[visibility]", visibilityFilter);
        }

        whenRequesting(req -> req
                .queryParams(params)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(3))
            .body("data.attributes.name", everyItem(startsWith(expectedPrefix)))
            .body("data.attributes.visibility", everyItem(is(expectedVisibility)));
    }

    @Test
    void testListTopicsWithIdFilter() {
        List<String> topicNames = IntStream.range(0, 6)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toList();

        var topicIds = topicUtils.createTopics(topicNames, 1).values().stream().sorted().toList();
        var selectedIds = topicIds.subList(0, 4);

        whenRequesting(req -> req
                .queryParam("filter[id]", "in," + String.join(",", selectedIds))
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(selectedIds.size()))
            .body("data.id", contains(selectedIds.toArray(String[]::new)));
    }

    @Test
    void testListTopicsWithNumPartitions() {
        var topicIds = IntStream.rangeClosed(1, 9)
                .mapToObj(i -> {
                    String topicName = i + "-" + UUID.randomUUID().toString();
                    return topicUtils.createTopics(List.of(topicName), i)
                        .get(topicName);
                })
                .toList();

        whenRequesting(req -> req
                .queryParam("filter[status]", "eq,FullyReplicated")
                .queryParam("fields[topics]", "numPartitions")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(topicIds.size()))
            .body("data.id", containsInAnyOrder(topicIds.toArray(String[]::new)))
            .body("data.attributes.numPartitions", containsInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @ParameterizedTest
    @CsvSource({
        "name, LIST",
        // numPartitions requires an additional describe
        "'name,numPartitions', LIST, GET"
    })
    void testListTopicsWithAuditLogging(String fields, @AggregateWith(VarargsAggregator.class) Privilege... privilegesAudited) {
        String topicName1 = UUID.randomUUID().toString();
        String topicName2 = UUID.randomUUID().toString();
        String topicName3 = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName1, topicName2, topicName3), 1);

        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(clusterConfig -> {
            clusterConfig.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewAudit()
                        .withDecision(Decision.ALL)
                        .withResources(ResourceTypes.Kafka.TOPICS.value())
                        .addToResourceNames(topicName1)
                        .withPrivileges(privilegesAudited)
                    .endAudit()
                    .addNewAudit()
                        .withDecision(Decision.ALL)
                        .withResources(ResourceTypes.Kafka.TOPICS.value())
                        .addToResourceNames(topicName2)
                        .withPrivileges(privilegesAudited)
                    .endAudit()
                    .build());
        });

        whenRequesting(req -> req
                .queryParam("fields[topics]", fields)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(3));

        var auditLogs = auditLogCapture.records();
        final String auditTmpl = "ANONYMOUS allowed console:kafkas/[default/test-kafka1]/topics:[%s]:[%s]";

        assertThat(auditLogs, not(hasItem(hasProperty("message", containsString("denied")))));
        assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(auditTmpl.formatted("", Privilege.LIST))))
                .and(hasProperty("level", equalTo(Level.INFO)))));

        for (var p : privilegesAudited) {
            assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(auditTmpl.formatted(topicName1, p))))
                    .and(hasProperty("level", equalTo(Level.INFO)))));
            assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(auditTmpl.formatted(topicName2, p))))
                    .and(hasProperty("level", equalTo(Level.INFO)))));
            assertThat(auditLogs, not(hasItem(hasProperty("message", containsString(auditTmpl.formatted(topicName3, p))))));
        }
    }

    @Test
    void testListTopicsWithManagedTopic() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic2 = "t2-" + UUID.randomUUID().toString();
        String topic3 = "t3-" + UUID.randomUUID().toString();
        String topic4 = "t4-" + UUID.randomUUID().toString();
        Map<String, String> topics = topicUtils.createTopics(List.of(topic1, topic2, topic3, topic4), 1);

        utils.apply(client, new KafkaTopicBuilder()
                .withNewMetadata()
                    .withName(topic1)
                    .withNamespace("default")
                    .withLabels(Map.of("strimzi.io/cluster", clusterName1))
                .endMetadata()
                .withNewSpec()
                    .withTopicName(topic1)
                    .withPartitions(1)
                .endSpec()
                .withNewStatus()
                    .withTopicName(topic1)
                    .withTopicId(topics.get(topic1))
                .endStatus()
            .build());

        utils.apply(client, new KafkaTopicBuilder()
                .withNewMetadata()
                    .withName(topic3)
                    .withNamespace("default")
                    .withLabels(Map.of("strimzi.io/cluster", clusterName1))
                .endMetadata()
                .withNewSpec()
                    // topicName (optional) is not set
                    .withPartitions(1)
                .endSpec()
                .withNewStatus()
                    .withTopicName(topic3)
                    .withTopicId(topics.get(topic3))
                .endStatus()
            .build());

        utils.apply(client, new KafkaTopicBuilder()
                .withNewMetadata()
                    .withName(topic4)
                    .withNamespace("default")
                    .withLabels(Map.of("strimzi.io/cluster", clusterName1))
                .endMetadata()
                .withNewSpec()
                    .withTopicName(topic4)
                    .withPartitions(1)
                .endSpec()
                // No status
            .build());

        // Wait for the managed topic list to include the topic
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.ofNullable(managedTopics.get("default"))
                    .map(clustersInNamespace -> clustersInNamespace.get(clusterName1))
                    .map(topicsInCluster -> topicsInCluster.get(topic1))
                    .isPresent());

        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(4))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic1), is(true))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic2), is(false))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic3), is(true))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic4), is(false));
    }

    @Test
    void testListTopicsWithManagedTopicMissingCluster() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic2 = "t2-" + UUID.randomUUID().toString();
        Map<String, String> topics = topicUtils.createTopics(List.of(topic1, topic2), 1);

        KafkaTopic topicCR = utils.apply(client, new KafkaTopicBuilder()
                .withNewMetadata()
                    .withName(topic1)
                    .withNamespace("default")
                    .withLabels(Map.of("strimzi.io/cluster", clusterName1))
                .endMetadata()
                .withNewSpec()
                    .withTopicName(topic1)
                    .withPartitions(1)
                .endSpec()
                .withNewStatus()
                    .withTopicName(topic1)
                    .withTopicId(topics.get(topic1))
                .endStatus()
            .build());

        // Wait for the managed topic list to include the topic
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.ofNullable(managedTopics.get("default"))
                    .map(clustersInNamespace -> clustersInNamespace.get(clusterName1))
                    .map(topicsInCluster -> topicsInCluster.get(topic1))
                    .isPresent());

        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(2))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic1), is(true))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic2), is(false));

        client.resource(new KafkaTopicBuilder(topicCR)
                    .editMetadata()
                        .withLabels(Collections.emptyMap())
                    .endMetadata()
                .build())
            .patch();

        // Wait for the managed topic list to contain the updated CR
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.ofNullable(managedTopics.get("default"))
                    .map(clustersInNamespace -> clustersInNamespace.get(clusterName1))
                    .map(topicsInCluster -> topicsInCluster.get(topic1))
                    .isEmpty());

        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(2))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic1), is(false))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic2), is(false));

    }

    @Test
    void testListTopicsWithManagedTopicBecomingUnmanaged() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic2 = "t2-" + UUID.randomUUID().toString();
        Map<String, String> topics = topicUtils.createTopics(List.of(topic1, topic2), 1);

        KafkaTopic topicCR = utils.apply(client, new KafkaTopicBuilder()
                .withNewMetadata()
                    .withName(topic1)
                    .withNamespace("default")
                    .withLabels(Map.of("strimzi.io/cluster", clusterName1))
                .endMetadata()
                .withNewSpec()
                    .withTopicName(topic1)
                    .withPartitions(1)
                .endSpec()
                .withNewStatus()
                    .withTopicName(topic1)
                    .withTopicId(topics.get(topic1))
                .endStatus()
            .build());

        // Wait for the managed topic list to include the topic
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.ofNullable(managedTopics.get("default"))
                    .map(clustersInNamespace -> clustersInNamespace.get(clusterName1))
                    .map(topicsInCluster -> topicsInCluster.get(topic1))
                    .isPresent());

        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(2))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic1), is(true))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic2), is(false));

        client.resource(new KafkaTopicBuilder(topicCR)
                    .editMetadata()
                        .withAnnotations(Map.of("strimzi.io/managed", "false"))
                    .endMetadata()
                .build())
            .patch();

        // Wait for the managed topic list to contain the updated CR
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.ofNullable(managedTopics.get("default"))
                    .map(clustersInNamespace -> clustersInNamespace.get(clusterName1))
                    .map(topicsInCluster -> topicsInCluster.get(topic1))
                    .map(KafkaTopic::getMetadata)
                    .map(ObjectMeta::getAnnotations)
                    .map(annotations -> annotations.getOrDefault("strimzi.io/managed", "true"))
                    .map("false"::equals)
                    .isPresent());

        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(2))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic1), is(false))
            .body("data.find { it.attributes.name == '%s' }.meta.managed".formatted(topic2), is(false));

    }

    @Test
    void testDescribeTopicWithNameAndConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,configs")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
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
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req.get("{topicId}", clusterId1, topicIds.get(topicName)))
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
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req.get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offsets.latest.offset", is(2))
            .body("data.attributes.partitions[0].offsets.latest", not(hasKey("timestamp")));
    }

    @Test
    void testDescribeTopicWithEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "earliest")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offsets.earliest.offset", is(0))
            .body("data.attributes.partitions[0].offsets.earliest", not(hasKey("timestamp")));
    }

    @Test
    void testDescribeTopicWithTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", first.toString())
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offsets.timestamp.offset", is(0))
            .body("data.attributes.partitions[0].offsets.timestamp.timestamp", is(first.toString()));
    }

    @Test
    void testDescribeTopicWithMaxTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "maxTimestamp")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offsets.maxTimestamp.offset", is(1))
            .body("data.attributes.partitions[0].offsets.maxTimestamp.timestamp", is(second.toString()));
    }

    @Test
    void testDescribeTopicWithBadOffsetTimestamp() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req
                .queryParam("offsetSpec", "Invalid Timestamp")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
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
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);

        Answer<ListOffsetsResult> listOffsetsFailed = args -> {
            @SuppressWarnings("unchecked")
            Set<TopicPartition> partitions = args.getArgument(0, Map.class).keySet();
            KafkaFutureImpl<ListOffsetsResultInfo> failure = new KafkaFutureImpl<>();
            failure.completeExceptionally(new ApiException("EXPECTED TEST EXCEPTION"));

            return new ListOffsetsResult(partitions
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), p -> failure)));
        };

        AdminClientSpy.install(adminClient -> {
            // Mock listOffsets
            doAnswer(listOffsetsFailed).when(adminClient).listOffsets(anyMap(), any(ListOffsetsOptions.class));
        });

        whenRequesting(req -> req
                .queryParam("offsetSpec", "latest")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offsets.latest.meta.type", is("error"))
            .body("data.attributes.partitions[0].offsets.latest.detail", is("EXPECTED TEST EXCEPTION"));
    }

    @Test
    void testListTopicsWithDescribeTopicsFailure() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2);

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

        AdminClientSpy.install(adminClient -> {
            // Mock describeTopics
            doAnswer(describeTopicsFailed)
                .when(adminClient)
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
        topicUtils.createTopics(List.of(topicName), 2);

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

        AdminClientSpy.install(adminClient -> {
            // Mock describeTopics
            doAnswer(describeConfigsFailed).when(adminClient).describeConfigs(anyCollection());
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
        whenRequesting(req -> req.get("{topicId}", clusterId1, Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

    @Test
    void testDescribeTopicWithOfflinePartition() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);

        //int partition, Node leader, List<Node> replicas, List<Node> isr
        Node node0 = new Node(0, "node0", bootstrapServers1.getPort());
        Node node1 = new Node(1, "node1", bootstrapServers1.getPort());

        Answer<DescribeTopicsResult> describeTopicsResult = args -> {
            List<TopicPartitionInfo> partitions = List.of(
                    // Online, 2 replicas, 1 ISR
                    new TopicPartitionInfo(0, node0, List.of(node0, node1), List.of(node0)),
                    // Offline, 2 replicas, no ISRs
                    new TopicPartitionInfo(1, null, List.of(node0, node1), List.of()));
            Set<AclOperation> authorizedOperations = Set.of(AclOperation.ALL);
            Uuid topicId = Uuid.fromString(topicIds.get(topicName));

            var description = KafkaFuture.completedFuture(
                    new TopicDescription(topicName, false, partitions, authorizedOperations, topicId));

            class Result extends DescribeTopicsResult {
                Result() {
                    super(Map.of(topicId, description), null);
                }
            }

            return new Result();
        };

        AdminClientSpy.install(adminClient -> {
            // Mock listOffsets
            doAnswer(describeTopicsResult)
                .when(adminClient)
                .describeTopics(any(TopicCollection.class), any(DescribeTopicsOptions.class));
        });

        whenRequesting(req -> req.get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes.status", is("PartiallyOffline"))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].status", is("UnderReplicated"))
            .body("data.attributes.partitions[0].replicas[0].inSync", is(true))
            .body("data.attributes.partitions[0].replicas[0].localStorage", notNullValue())
            // storage not fetched for followers
            .body("data.attributes.partitions[0].replicas[1].inSync", is(false))
            .body("data.attributes.partitions[0].replicas[1].localStorage", nullValue())
            // Partition 2, offline with no ISRs
            .body("data.attributes.partitions[1].status", is("Offline"))
            .body("data.attributes.partitions[1].replicas.inSync", everyItem(is(false)));
    }

    @Test
    void testCreateTopicSucceeds() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        .add("replicationFactor", 1)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes.numPartitions", is(3))
            .body("data.attributes.replicationFactor", is(1))
            .body("data.attributes.configs", is(aMapWithSize(greaterThan(0))));
    }

    @Test
    void testCreateTopicWithReplicaAssignmentsSucceeds() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("replicasAssignments", Json.createObjectBuilder()
                                                .add("0", Json.createArrayBuilder().add(0))
                                                .add("1", Json.createArrayBuilder().add(0))
                                                .add("2", Json.createArrayBuilder().add(0)))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes.numPartitions", is(3))
            .body("data.attributes.replicationFactor", is(1))
            .body("data.attributes.configs", is(aMapWithSize(greaterThan(0))));
    }

    @Test
    void testCreateTopicWithConfigsSucceeds() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("configs", Json.createObjectBuilder()
                                                .add("retention.ms", Json.createObjectBuilder()
                                                        .add("value", "604800001")))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes.configs.'retention.ms'.value", is("604800001"));
    }

    @Test
    void testCreateTopicWithValidateOnly() {
        String topicName = UUID.randomUUID().toString();

        String response = whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("meta", Json.createObjectBuilder()
                                .add("validateOnly", true))
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is(notNullValue()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes.configs", is(aMapWithSize(greaterThan(0))))
            .extract()
            .asString();

        // Confirm nothing created
        try (JsonReader reader = Json.createReader(new StringReader(response))) {
            String topicId = reader.readObject().getJsonObject("data").getString("id");
            whenRequesting(req -> req.get("{topicId}", clusterId1, topicId))
                .assertThat()
                .statusCode(is(Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    void testCreateTopicUnsupportedMediaType() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        .add("replicationFactor", 1)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("415"))
            .body("errors.code", contains("4151"));
    }

    @Test
    void testCreateTopicInvalidNumPartitionsType() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        // Send String instead of required integer
                                        .add("numPartitions", "three")
                                        .add("replicationFactor", 1)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"))
            .body("errors[0].source.pointer", is("/data/attributes/numPartitions"));
    }

    @Test
    void testCreateTopicInvalidReplicationFactor() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        // There is only one Kafka node for this test
                                        .add("replicationFactor", 2)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"))
            .body("errors[0].source.pointer", is("/data/attributes/replicationFactor"));
    }

    @Test
    void testCreateTopicInvalidReplicasAssignments() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("replicasAssignments", Json.createObjectBuilder()
                                                // API validation prevents negative node IDs
                                                .add("0", Json.createArrayBuilder().add(-1))
                                                .add("1", Json.createArrayBuilder().add(0))
                                                .add("2", Json.createArrayBuilder().add(0)))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"))
            .body("errors[0].source.pointer", is("/data/attributes/replicasAssignments/0/0"));
    }

    @Test
    void testCreateTopicInvalidReplicasAssignmentsDuplicated() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("replicasAssignments", Json.createObjectBuilder()
                                                // Kafka rejects multiple assignments to the same node
                                                .add("0", Json.createArrayBuilder().add(0).add(0))
                                                .add("1", Json.createArrayBuilder().add(0))
                                                .add("2", Json.createArrayBuilder().add(0)))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(3))
            .body("errors.status", contains("400", "400", "400"))
            .body("errors.code", contains("4003", "4003", "4003"))
            .body("errors.source.pointer", containsInAnyOrder(
                    // Second replica ID (position 1) for partition 0 is duplicate
                    "/data/attributes/replicasAssignments/0/1",
                    // Number of replicas disagress with partition 0
                    "/data/attributes/replicasAssignments/1",
                    // Number of replicas disagress with partition 0
                    "/data/attributes/replicasAssignments/2"
            ));
    }

    @Test
    void testCreateTopicWithDuplicateName() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        .add("replicationFactor", 1)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()));

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        .add("replicationFactor", 1)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.CONFLICT.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("409"))
            .body("errors[0].code", is("4091"))
            .body("errors[0].source.pointer", is("/data/attributes/name"));

        topicUtils.deleteTopics(topicName);
    }

    @Test
    void testCreateTopicWithConflictingType() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "widgets")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        .add("replicationFactor", 1)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.CONFLICT.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("409"))
            .body("errors[0].code", is("4091"))
            .body("errors[0].source.pointer", is("/data/type"));
    }

    @Test
    void testCreateTopicWithInvalidConfigs() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("configs", Json.createObjectBuilder()
                                                .add("retention.ms", Json.createObjectBuilder()
                                                        .add("value", "NaN")))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"));
    }

    @Test
    void testCreateTopicWithConflictingPartitionConfigs() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        .add("replicationFactor", 1)
                                        // numPartitions/replicationFactor may not be used with replicasAssignments
                                        .add("replicasAssignments", Json.createObjectBuilder()
                                                .add("0", Json.createArrayBuilder().add(0))
                                                .add("1", Json.createArrayBuilder().add(0))
                                                .add("2", Json.createArrayBuilder().add(0)))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(2))
            .body("errors.status", contains("400", "400"))
            .body("errors.code", contains("4003", "4003"))
            .body("errors.source.pointer", containsInAnyOrder(
                "/data/attributes/numPartitions",
                "/data/attributes/replicationFactor"
            ));
    }

    @Test
    void testCreateTopicWithUnknownReplicaId() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("replicasAssignments", Json.createObjectBuilder()
                                                // 1 is not a valid replica
                                                .add("0", Json.createArrayBuilder().add(1)))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"))
            .body("errors[0].source.pointer", is("/data/attributes/replicasAssignments/0/0"));
    }

    @Test
    void testCreateTopicWithMissingAssignments() {
        String topicName = UUID.randomUUID().toString();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("replicasAssignments", Json.createObjectBuilder()
                                                // Missing assignment for partition 0
                                                .add("1", Json.createArrayBuilder().add(0)))))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"))
            .body("errors[0].source.pointer", is("/data/attributes/replicasAssignments/0"));
    }

    @ParameterizedTest
    @CsvSource({
        "Null name,  ",
        "Empty name, ''",
        "Blank/whitespace name, ' '",
    })
    void testCreateTopicWithBlankName(String label, String topicName) {
        JsonValue nameValue = Optional.ofNullable(topicName)
                .<JsonValue>map(Json::createValue)
                .orElse(JsonValue.NULL);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", nameValue)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"));
    }

    @Test
    void testCreateTopicWithStrimziManagement() {
        client.resources(Kafka.class).inNamespace("default").withName(clusterName1).edit(k -> {
            // Enable the topic entity operator for kafka1
            return new KafkaBuilder(k)
                .editSpec()
                    .editOrNewEntityOperator()
                        .editOrNewTopicOperator()
                        .endTopicOperator()
                    .endEntityOperator()
                .endSpec()
                .build();
        });

        // Wait for the modified cluster to be updated in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.of(configuredContexts.get(clusterId1))
                    .map(KafkaContext::resource)
                    .map(Kafka::getSpec)
                    .map(KafkaSpec::getEntityOperator)
                    .map(EntityOperatorSpec::getTopicOperator)
                    .map(Objects::nonNull)
                    .orElse(Boolean.FALSE));

        String topicName = "t-" + UUID.randomUUID().toString();

        var response = CompletableFuture.supplyAsync(() ->
            whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)))
                        .build()
                        .toString())
                .post("", clusterId1)));

        /*
         * Acting as Strimzi Topic Operator.
         * 1. wait for the topic CR to be present
         * 2. create the topic
         * 3. update the CR's status
         */
        var topicClient = client.resources(KafkaTopic.class).inNamespace("default").withName(topicName);
        await()
            .atMost(TopicService.TOPIC_OPERATION_LIMIT, TimeUnit.SECONDS)
            .conditionEvaluationListener(new ConditionEvaluationListener<Object>() {
                @Override
                public void conditionEvaluated(EvaluatedCondition<Object> condition) {
                    // No-op
                }

                @Override
                public void onTimeout(org.awaitility.core.TimeoutEvent timeoutEvent) {
                    LOGGER.infof("Timeout creating topic %s", topicName);
                    response.join().log().all();
                }
            })
            .until(() -> topicClient.get() != null);

        String topicId = topicUtils.createTopics(List.of(topicName), 1).get(topicName);
        topicClient.editStatus(t -> {
            return new KafkaTopicBuilder(t)
                    .withNewStatus()
                        .withTopicId(topicId)
                        .addNewCondition()
                            .withType("Ready")
                            .withStatus("True")
                        .endCondition()
                    .endStatus()
                    .build();
        });

        response.join()
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.id", is(topicId));
    }

    @Test
    void testDeleteTopicSucceeds() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req.delete("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.NO_CONTENT.getStatusCode()));
    }

    @Test
    void testDeleteTopicNonexistentTopic() {
        String topicId = Uuid.randomUuid().toString();

        whenRequesting(req -> req.delete("{topicId}", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

    @Test
    void testDeleteTopicWithStrimziManagement() {
        String topicName = "t-" + UUID.randomUUID().toString();
        String topicId = topicUtils.createTopics(List.of(topicName), 1).get(topicName);
        utils.apply(client, new KafkaTopicBuilder()
                .withNewMetadata()
                    .withNamespace("default")
                    .withName(topicName)
                    .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName1)
                .endMetadata()
                .withNewSpec()
                    .withTopicName(topicName)
                    .withPartitions(1)
                .endSpec()
                .withNewStatus()
                    .withTopicId(topicId)
                    .withTopicName(topicName)
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus("True")
                    .endCondition()
                .endStatus()
                .build());

        // Wait for the managed topic list to include the topic
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.ofNullable(managedTopics.get("default"))
                    .map(clustersInNamespace -> clustersInNamespace.get(clusterName1))
                    .map(topicsInCluster -> topicsInCluster.get(topicName))
                    .isPresent());

        whenRequesting(req -> req.delete("{topicId}", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.NO_CONTENT.getStatusCode()));

        assertNull(client.resources(KafkaTopic.class).inNamespace("default").withName(topicName).get());
    }

    @Test
    void testPatchTopicWithAllOptions() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 3);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", topicIds.get(topicName))
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("numPartitions", 4) // adding partition
                                        .add("replicasAssignments", Json.createObjectBuilder()
                                                // provide reassignments for existing partitions + assignment for new
                                                .add("0", Json.createArrayBuilder().add(0))
                                                .add("1", Json.createArrayBuilder().add(0))
                                                .add("2", Json.createArrayBuilder().add(0))
                                                .add("3", Json.createArrayBuilder().add(0)))
                                        .add("configs", Json.createObjectBuilder()
                                                .add("retention.ms", Json.createObjectBuilder()
                                                        .add("value", "300000")))))
                        .build()
                        .toString())
                .patch("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.NO_CONTENT.getStatusCode()));

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions,configs")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.partitions.size()", is(4))
            .body("data.attributes.configs.'retention.ms'.value", is("300000"));
    }

    @Test
    void testPatchTopicWithConfigOverrideDelete() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 1, Map.of("retention.ms", "300000"));

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", topicIds.get(topicName))
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("numPartitions", 2) // adding partition
                                        .add("configs", Json.createObjectBuilder()
                                                .add("retention.ms", JsonValue.NULL))))
                        .build()
                        .toString())
                .patch("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.NO_CONTENT.getStatusCode()));

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,configs")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.configs.'retention.ms'.value", is(String.valueOf(Duration.ofDays(7).toMillis())));
    }

    @Test
    void testPatchTopicWithValidateOnly() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 1, Map.of("retention.ms", "300000"));

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("meta", Json.createObjectBuilder()
                                .add("validateOnly", true))
                        .add("data", Json.createObjectBuilder()
                                .add("id", topicIds.get(topicName))
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("numPartitions", 2) // adding partition
                                        .add("configs", Json.createObjectBuilder()
                                                .add("retention.ms", JsonValue.NULL))))
                        .build()
                        .toString())
                .patch("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.NO_CONTENT.getStatusCode()));

        // Confirm nothing changed
        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions,configs")
                .get("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.partitions.size()", is(1))
            .body("data.attributes.configs.'retention.ms'.value", is("300000"));
    }

    @ParameterizedTest
    @CsvFileSource(
        delimiter = '|',
        lineSeparator = "@\n",
        resources = { "/patchTopic-invalid-requests.txt" })
    void testPatchTopicWithInvalidRequest(String label, String requestBody, Status responseStatus, String expectedResponse)
            throws JSONException {

        String topicName = UUID.randomUUID().toString();
        String topicId = topicUtils.createTopics(List.of(topicName), 2).get(topicName);
        String preparedRequest = requestBody.contains("%s") ? requestBody.formatted(topicId) : requestBody;

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(preparedRequest)
                .patch("{topicId}", clusterId1, topicId))
            .assertThat()
            .statusCode(is(responseStatus.getStatusCode()))
            .body(new TypeSafeMatcher<String>() {
                @Override
                public boolean matchesSafely(String response) {
                    try {
                        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.LENIENT);
                    } catch (JSONException e) {
                        return false;
                    }
                    return true;
                }

                @Override
                public void describeTo(Description description) {
                    description.appendValue(expectedResponse);
                }
            });
    }

    @Test
    void testPatchTopicWithStrimziManagement() {
        String topicName = "t-" + UUID.randomUUID().toString();
        String topicId = topicUtils.createTopics(List.of(topicName), 1).get(topicName);
        utils.apply(client, new KafkaTopicBuilder()
                .withNewMetadata()
                    .withNamespace("default")
                    .withName(topicName)
                    .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName1)
                .endMetadata()
                .withNewSpec()
                    .withTopicName(topicName)
                    .withPartitions(1)
                    .addToConfig(TopicConfig.SEGMENT_BYTES_CONFIG, String.valueOf(16 * Math.pow(1024, 2)))
                    .addToConfig(TopicConfig.COMPRESSION_TYPE_CONFIG, "gzip")
                    .addToConfig(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .endSpec()
                .withNewStatus()
                    .withTopicId(topicId)
                    .withTopicName(topicName)
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus("True")
                    .endCondition()
                .endStatus()
                .build());

        // Wait for the managed topic list to include the topic
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Optional.ofNullable(managedTopics.get("default"))
                    .map(clustersInNamespace -> clustersInNamespace.get(clusterName1))
                    .map(topicsInCluster -> topicsInCluster.get(topicName))
                    .isPresent());

        var response = CompletableFuture.supplyAsync(() ->
            whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("meta", Json.createObjectBuilder()
                                .add("validateOnly", false))
                        .add("data", Json.createObjectBuilder()
                                .add("id", topicId)
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("numPartitions", 2) // adding partition
                                        .add("configs", Json.createObjectBuilder()
                                                // remove
                                                .add(TopicConfig.SEGMENT_BYTES_CONFIG, JsonValue.NULL)
                                                // change
                                                .add(TopicConfig.COMPRESSION_TYPE_CONFIG, Json.createObjectBuilder()
                                                        .add("value", "lz4"))
                                                // add
                                                .add(TopicConfig.RETENTION_MS_CONFIG, Json.createObjectBuilder()
                                                        .add("value", "86400000"))
                                        )
                                )
                        )
                        .build()
                        .toString())
                .patch("{topicId}", clusterId1, topicId)));

        // check that the API has changed the KafkaTopic CR
        var topicClient = client.resources(KafkaTopic.class).inNamespace("default").withName(topicName);
        await().atMost(TopicService.TOPIC_OPERATION_LIMIT + 5, TimeUnit.SECONDS).untilAsserted(() -> {
            var topic = topicClient.get();
            assertNotNull(topic);
            assertEquals(2, topic.getSpec().getPartitions());
            var expectedConfigs = Map.ofEntries(
                Map.entry(TopicConfig.CLEANUP_POLICY_CONFIG, "delete"),
                Map.entry(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4"),
                Map.entry(TopicConfig.RETENTION_MS_CONFIG, "86400000")
            );
            var actualConfigs = topic.getSpec().getConfig();
            assertEquals(expectedConfigs, actualConfigs);
        });

        // Acting as Strimzi Topic Operator
        topicUtils.createPartitions(topicName, 2);
        topicUtils.alterTopicConfigs(topicName, List.of(
            new AlterConfigOp(new ConfigEntry(TopicConfig.SEGMENT_BYTES_CONFIG, ""), AlterConfigOp.OpType.DELETE),
            new AlterConfigOp(new ConfigEntry(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4"), AlterConfigOp.OpType.SET),
            new AlterConfigOp(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, "86400000"), AlterConfigOp.OpType.SET)
        ));

        // update the generation to allow the response to complete
        topicClient.editStatus(t -> {
            return new KafkaTopicBuilder(t)
                    .editStatus()
                        .withObservedGeneration(t.getMetadata().getGeneration())
                    .endStatus()
                    .build();
        });

        response.join().assertThat().statusCode(is(Status.NO_CONTENT.getStatusCode()));

        // Confirm values are expected
        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions,configs")
                .get("{topicId}", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.partitions.size()", is(2))
            .body("data.attributes.configs.'segment.bytes'.value", is("1073741824"))
            .body("data.attributes.configs.'segment.bytes'.source", is(ConfigEntry.ConfigSource.DEFAULT_CONFIG.name()))
            .body("data.attributes.configs.'compression.type'.value", is("lz4"))
            .body("data.attributes.configs.'compression.type'.source", is(ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG.name()))
            .body("data.attributes.configs.'retention.ms'.value", is("86400000"))
            .body("data.attributes.configs.'retention.ms'.source", is(ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG.name()));
    }

    @Test
    void testListTopicConsumerGroupsMatchesRelatedConsumerGroups() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic1Id = topicUtils.createTopics(List.of(topic1), 2).get(topic1);

        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String group2 = "g2-" + UUID.randomUUID().toString();
        String group2Id = Identifiers.encode(group2);

        String client1 = "c1-" + UUID.randomUUID().toString();
        String client2 = "c2-" + UUID.randomUUID().toString();

        try (var consumer1 = groupUtils.request().groupId(group1).topic(topic1).createTopic(false).clientId(client1).messagesPerTopic(1).autoClose(false).consume();
             var consumer2 = groupUtils.request().groupId(group2).topic(topic1).createTopic(false).clientId(client2).messagesPerTopic(1).autoClose(false).consume()) {
            whenRequesting(req -> req
                    .queryParam("fields[topics]", "name,groups")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(1))
                .body("data[0].relationships.groups.data.size()", is(2))
                .body("data[0].relationships.groups.data.type", contains("groups", "groups"))
                .body("data[0].relationships.groups.data.id", containsInAnyOrder(group1Id, group2Id));

            whenRequesting(req -> req.get("{topicId}/groups", clusterId1, topic1Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(2))
                .body("data.id", containsInAnyOrder(group1Id, group2Id))
                .body("data.attributes.groupId", containsInAnyOrder(group1, group2));
        }
    }

    @Test
    void testListTopicConsumerGroupsWithEmptyList() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();

        String topic2 = "t2-" + UUID.randomUUID().toString();
        String topic2Id = topicUtils.createTopics(List.of(topic2), 2).get(topic2);

        try (var consumer1 = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req
                    .queryParam("fields[topics]", "name,groups")
                    .get("{topicId}", clusterId1, topic2Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.relationships.groups.data.size()", is(0));

            whenRequesting(req -> req.get("{topicId}/groups", clusterId1, topic2Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(0));
        }
    }

    @Test
    void testListTopicConsumerGroupsWithNoSuchTopic() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer1 = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req.get("{topicId}/groups", clusterId1, Uuid.randomUuid().toString()))
                .assertThat()
                .statusCode(is(Status.NOT_FOUND.getStatusCode()))
                .body("errors.size()", is(1))
                .body("errors.status", contains("404"))
                .body("errors.code", contains("4041"));
        }
    }

    @Test
    void testGetTopicMetricsSuccess() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 1);

        String topicId = whenRequesting(req -> req.get("", clusterId1))
                .statusCode(is(Status.OK.getStatusCode()))
                .extract()
                .path("data.find { it.attributes.name == '%s' }.id", topicName);

        whenRequesting(req -> req.pathParam("topicId", topicId)
                .queryParam("duration[metrics]", 10)
                .get("/{topicId}/metrics", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is(topicId))
            .body("data.type", is("topicMetrics"))
            .body("data.attributes.metrics", notNullValue());
    }

    @Test
    void testGetTopicMetrics_24HoursDuration() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 1);

        String topicId = whenRequesting(req -> req.get("", clusterId1))
            .extract()
            .path("data.find { it.attributes.name == '%s' }.id", topicName);

        whenRequesting(req ->
            req.pathParam("topicId", topicId)
                .queryParam("duration[metrics]", "1440")
                .get("/{topicId}/metrics", clusterId1)
        )
        .assertThat()
        .statusCode(Status.OK.getStatusCode())
            .body("data.id", equalTo(topicId));
    }

    @Test
    void testGetTopicMetrics_7DaysDuration() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 1);

        String topicId = whenRequesting(req -> req.get("", clusterId1))
            .extract()
            .path("data.find { it.attributes.name == '%s' }.id", topicName);

        whenRequesting(req ->
            req.pathParam("topicId", topicId)
                .queryParam("duration[metrics]", "10080")
                .get("/{topicId}/metrics", clusterId1)
        )
        .assertThat()
            .statusCode(Status.OK.getStatusCode());
    }

    // Utilities

    JsonObject linkExtract(String response) {
        try (JsonReader reader = Json.createReader(new StringReader(response))) {
            return reader.readObject().getJsonObject("links");
        }
    }
}
