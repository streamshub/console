package com.github.eyefloaters.console.api;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.ServerSocket;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.kafka.systemtest.utils.ConsumerUtils;
import com.github.eyefloaters.console.test.AdminClientSpy;
import com.github.eyefloaters.console.test.TestHelper;
import com.github.eyefloaters.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.Kafka;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;
    TopicHelper topicUtils;
    ConsumerUtils groupUtils;
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

        groupUtils = new ConsumerUtils(config, null);

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

        // Wait for the informer cache to be populated with all Kafka CRs
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Objects.equals(kafkaInformer.getStore().list().size(), 2));
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
                .toList();

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
            .body("data.attributes.partitions[0][0].offsets.latest.offset", is(2))
            .body("data.attributes.partitions[0][0].offsets.latest", not(hasKey("timestamp")));
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
            .body("data.attributes.partitions[0][0].offsets.earliest.offset", is(0))
            .body("data.attributes.partitions[0][0].offsets.earliest", not(hasKey("timestamp")));
    }

    @Test
    void testListTopicsWithRecordCountIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,recordCount")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.recordCount", contains(2));
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
            .body("data.attributes.partitions[0][0].offsets.timestamp.offset", is(0))
            .body("data.attributes.partitions[0][0].offsets.timestamp.timestamp", is(first.toString()));
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
            topicIds.putAll(topicUtils.createTopics(clusterId1, names, 1, configs));
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
    void testListTopicsPaginationLinksWithDefaultPageSize() throws MalformedURLException {
        List<String> topicNames = IntStream.range(0, 102)
                .mapToObj("%03d-"::formatted)
                .map(prefix -> prefix + UUID.randomUUID().toString())
                .toList();

        topicUtils.createTopics(clusterId1, topicNames, 1);

        Function<String, JsonObject> linkExtract = response -> {
            try (JsonReader reader = Json.createReader(new StringReader(response))) {
                return reader.readObject().getJsonObject("links");
            }
        };

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

        JsonObject links1 = linkExtract.apply(response1);
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
        JsonObject links2 = linkExtract.apply(response2);
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

        // Return to page 1 using the `first` link provided by the last page, 11
        JsonObject links3 = linkExtract.apply(response3);
        URI request4 = URI.create(links3.getString("first"));
        String response4 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request4))
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

        assertEquals(response1, response4);
    }

    @Test
    void testListTopicsPaginationLinksNullWithSinglePage() throws MalformedURLException {
        List<String> topicNames = IntStream.range(0, 102)
                .mapToObj("%03d-"::formatted)
                .map(prefix -> prefix + UUID.randomUUID().toString())
                .toList();

        topicUtils.createTopics(clusterId1, topicNames, 1);

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
    void testListTopicsWithConsumerGroupsLinkage() throws Exception {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic2 = "t2-" + UUID.randomUUID().toString();

        String group1 = "g1-" + UUID.randomUUID().toString();
        String group2 = "g2-" + UUID.randomUUID().toString();

        String client1 = "c1-" + UUID.randomUUID().toString();
        String client2 = "c2-" + UUID.randomUUID().toString();

        try (var consumer1 = groupUtils.consume(group1, topic1, client1, 2, false);
             var consumer2 = groupUtils.consume(group2, topic2, client2, 2, false)) {
            whenRequesting(req -> req
                    .queryParam("fields[topics]", "name,consumerGroups")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(2))
                .body("data.find { it.attributes.name == '%s' }.relationships.consumerGroups.data[0]".formatted(topic1),
                    allOf(
                        hasEntry(equalTo("type"), equalTo("consumerGroups")),
                        hasEntry(equalTo("id"), equalTo(group1))))
                .body("data.find { it.attributes.name == '%s' }.relationships.consumerGroups.data[0]".formatted(topic2),
                    allOf(
                        hasEntry(equalTo("type"), equalTo("consumerGroups")),
                        hasEntry(equalTo("id"), equalTo(group2))));
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

        topicUtils.createTopics(clusterId1, topicNames, 1);

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

        var topicIds = topicUtils.createTopics(clusterId1, topicNames, 1).values().stream().sorted().toList();
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
    void testDescribeTopicWithNameAndConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);
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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);
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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        whenRequesting(req -> req.get("{topicId}", clusterId1, Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
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

    @Test
    void testDeleteTopicSucceeds() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
    void testPatchTopicWithAllOptions() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 3);

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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1, Map.of("retention.ms", "300000"));

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
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1, Map.of("retention.ms", "300000"));

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
        String topicId = topicUtils.createTopics(clusterId1, List.of(topicName), 2).get(topicName);
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
    void testListTopicConsumerGroupsMatchesRelatedConsumerGroups() throws Exception {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic1Id = topicUtils.createTopics(clusterId1, List.of(topic1), 2).get(topic1);

        String group1 = "g1-" + UUID.randomUUID().toString();
        String group2 = "g2-" + UUID.randomUUID().toString();

        String client1 = "c1-" + UUID.randomUUID().toString();
        String client2 = "c2-" + UUID.randomUUID().toString();

        try (var consumer1 = groupUtils.request().groupId(group1).topic(topic1).createTopic(false).clientId(client1).messagesPerTopic(1).autoClose(false).consume();
             var consumer2 = groupUtils.request().groupId(group2).topic(topic1).createTopic(false).clientId(client2).messagesPerTopic(1).autoClose(false).consume()) {
            whenRequesting(req -> req
                    .queryParam("fields[topics]", "name,consumerGroups")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(1))
                .body("data[0].relationships.consumerGroups.data.size()", is(2))
                .body("data[0].relationships.consumerGroups.data.type", contains("consumerGroups", "consumerGroups"))
                .body("data[0].relationships.consumerGroups.data.id", containsInAnyOrder(group1, group2));

            whenRequesting(req -> req.get("{topicId}/consumerGroups", clusterId1, topic1Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(2))
                .body("data.id", containsInAnyOrder(group1, group2));
        }
    }

    @Test
    void testListTopicConsumerGroupsWithEmptyList() throws Exception {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();

        String topic2 = "t2-" + UUID.randomUUID().toString();
        String topic2Id = topicUtils.createTopics(clusterId1, List.of(topic2), 2).get(topic2);

        try (var consumer1 = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req
                    .queryParam("fields[topics]", "name,consumerGroups")
                    .get("{topicId}", clusterId1, topic2Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.relationships.consumerGroups.data.size()", is(0));

            whenRequesting(req -> req.get("{topicId}/consumerGroups", clusterId1, topic2Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(0));
        }
    }

    @Test
    void testListTopicConsumerGroupsWithNoSuchTopic() throws Exception {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer1 = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req.get("{topicId}/consumerGroups", clusterId1, Uuid.randomUuid().toString()))
                .assertThat()
                .statusCode(is(Status.NOT_FOUND.getStatusCode()))
                .body("errors.size()", is(1))
                .body("errors.status", contains("404"))
                .body("errors.code", contains("4041"));
        }
    }
}
