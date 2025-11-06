package com.github.streamshub.console.api;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.Uuid;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.TimeoutEvent;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.serdes.RecordData;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.RecordHelper;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;

import io.apicurio.registry.types.ArtifactType;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.strimzi.api.kafka.model.kafka.Kafka;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(RecordsResource.class)
@TestProfile(TestPlainProfile.class)
class RecordsResourceIT {

    @Inject
    Config config;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> kafkaContexts;

    @Inject
    KubernetesClient client;

    TestHelper utils;
    TopicHelper topicUtils;
    RecordHelper recordUtils;
    String clusterId1;
    String clusterId2;

    @BeforeEach
    void setup() {
        URI bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        URI randomBootstrapServers = URI.create(consoleConfig.getKafka()
                .getCluster("default/test-kafka2")
                .map(k -> k.getProperties().get("bootstrap.servers"))
                .orElseThrow());

        topicUtils = new TopicHelper(bootstrapServers, config);
        topicUtils.deleteAllTopics();

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        recordUtils = new RecordHelper(bootstrapServers, config);

        client.resources(Kafka.class).inAnyNamespace().delete();

        utils.apply(client, utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers));
        // Second cluster is offline/non-existent
        utils.apply(client, utils.buildKafkaResource("test-kafka2", UUID.randomUUID().toString(), randomBootstrapServers));

        // Wait for the context map to be populated with all Kafka configurations
        await().atMost(10, TimeUnit.SECONDS).until(() -> kafkaContexts.values()
                .stream()
                .map(KafkaContext::clusterConfig)
                .map(KafkaClusterConfig::getName)
                .toList()
                .containsAll(List.of("test-kafka1", "test-kafka2")));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
    }

    @Test
    void testConsumeRecordFromInvalidTopic() {
        final String topicId = Uuid.randomUuid().toString();

        whenRequesting(req -> req.get("", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

    @Test
    void testConsumeRecordFromInvalidPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req
                .queryParam("filter[partition]", -1)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("filter[partition]"));
    }

    @Test
    void testConsumeRecordFromNonexistentPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req
                .queryParam("filter[partition]", 2)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(0));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 1001 })
    void testConsumeRecordWithInvalidPageSize(int pageSize) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req
                .queryParam("page[size]", pageSize)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("page[size]"));
    }

    @ParameterizedTest
    @CsvSource({
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-01T00:00:00.000Z, 2",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 1",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-03T00:00:00.000Z, 0"
    })
    void testConsumeRecordsAsOfTimestamp(Instant ts1, Instant ts2, Instant tsSearch, int expectedResults) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2, Map.of("retention.ms", "-1"));
        recordUtils.produceRecord(topicName, ts1, null, "the-key1", "the-value1");
        recordUtils.produceRecord(topicName, ts2, null, "the-key2", "the-value2");

        await().atMost(10, TimeUnit.SECONDS)
            .conditionEvaluationListener(new ConditionEvaluationListener<Boolean>() {
                @Override
                public void conditionEvaluated(EvaluatedCondition<Boolean> condition) {
                    // No-op
                }
                @Override
                public void onTimeout(TimeoutEvent timeoutEvent) {
                    Logger.getLogger(getClass()).warnf("Timed out waiting for number of records to be 2: actual %d", topicUtils.getTopicSize(topicName));
                }
            })
            .until(() -> topicUtils.getTopicSize(topicName) >= 2);

        whenRequesting(req -> req
                .queryParam("filter[timestamp]", "gte," + tsSearch.toString())
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(expectedResults));
    }

    @ParameterizedTest
    @CsvSource({
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-01T00:00:00.000Z",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, eq,2000-01-02T00:00:00.000Z",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, gte,1969-12-31T23:59:59.999Z"
    })
    void testConsumeRecordsAsOfInvalidTimestamp(Instant ts1, Instant ts2, String tsSearch) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2, Map.of("retention.ms", "-1"));
        recordUtils.produceRecord(topicName, ts1, null, "the-key1", "the-value1");
        recordUtils.produceRecord(topicName, ts2, null, "the-key2", "the-value2");

        await().atMost(10, TimeUnit.SECONDS)
            .conditionEvaluationListener(new ConditionEvaluationListener<Boolean>() {
                @Override
                public void conditionEvaluated(EvaluatedCondition<Boolean> condition) {
                    // No-op
                }
                @Override
                public void onTimeout(TimeoutEvent timeoutEvent) {
                    Logger.getLogger(getClass()).warnf("Timed out waiting for number of records to be 2: actual %d", topicUtils.getTopicSize(topicName));
                }
            })
            .until(() -> topicUtils.getTopicSize(topicName) >= 2);

        whenRequesting(req -> req
                .queryParam("filter[timestamp]", tsSearch)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("filter[timestamp]"));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 3",
        "1, 2",
        "2, 1",
        "3, 0",
        "4, 0"
    })
    void testConsumeRecordsByStartingOffset(int startingOffset, int expectedResults) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1); // single partition
        for (int i = 0; i < 3; i++) {
            recordUtils.produceRecord(topicName, null, null, "the-key-" + i, "the-value-" + i);
        }

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 3);

        whenRequesting(req -> req
                .queryParam("filter[partition]", 0)
                .queryParam("filter[offset]", "gte," + startingOffset)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(expectedResults));
    }

    @ParameterizedTest
    @CsvSource({
        "-1",
        "gte,-1",
        "eq,2"
    })
    void testConsumeRecordsByInvalidStartingOffset(String startingOffset) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1); // single partition
        for (int i = 0; i < 3; i++) {
            recordUtils.produceRecord(topicName, null, null, "the-key-" + i, "the-value-" + i);
        }

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 3);

        whenRequesting(req -> req
                .queryParam("filter[partition]", 0)
                .queryParam("filter[offset]", startingOffset)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("filter[offset]"));
    }

    @ParameterizedTest
    @CsvSource({
        "10",
        "20",
        "30",
        "40",
        "200"
    })
    void testConsumeLatestRecords(int limit) {
        final String topicName = UUID.randomUUID().toString();
        final int totalRecords = 100;
        var topicIds = topicUtils.createTopics(List.of(topicName), 10);
        List<String> messageValues = new ArrayList<>();

        for (int i = 0; i < totalRecords; i++) {
            String value = "the-value-" + i;
            messageValues.add(value);
            recordUtils.produceRecord(topicName, null, null, "the-key-" + i, value);
        }

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= totalRecords);

        Collections.reverse(messageValues);

        int resultCount = Math.min(limit, totalRecords);

        whenRequesting(req -> req
                .queryParam("page[size]", limit)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(resultCount))
            .body("data.findAll { it }.attributes.value", contains(messageValues.subList(0, resultCount).toArray(String[]::new)));
    }

    @Test
    void testConsumeRecordsIncludeOnlyHeaders() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);
        for (int i = 0; i < 3; i++) {
            recordUtils.produceRecord(topicName, null, Map.of("h1", "h1-value-" + i), "the-key-" + i, "the-value-" + i);
        }

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 3);

        whenRequesting(req -> req
                .queryParam("fields[records]", "headers")
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(3))
            .body("data", everyItem(allOf(
                    is(aMapWithSize(3)),
                    hasEntry("type", "records"),
                    hasKey("attributes"),
                    hasKey("relationships"))))
            .body("data.attributes.headers", everyItem(hasKey("h1")));
    }

    @Test
    void testConsumeRecordWithEmptyValue() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        recordUtils.produceRecord(topicName, null, null, null, "");

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        whenRequesting(req -> req.get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(1))
            .body("data[0].attributes.value", is(equalTo("")));
    }

    @Test
    void testConsumeRecordWithBinaryValue() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        final byte[] data = new byte[512];
        SecureRandom.getInstanceStrong().nextBytes(data);
        data[511] = -1; // ensure at least one byte invalid

        recordUtils.produceRecord(topicName, null, null, null, data);

        whenRequesting(req -> req.get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(1))
            .body("data[0].attributes.value", is(equalTo(RecordData.BINARY_DATA_MESSAGE)));
    }

    @ParameterizedTest
    @CsvSource({
        "  1,   1",
        "  5,   5",
        " 99,  99",
        "100, 100",
        "101, 100",
        "200, 100",
        "   , 100",
    })
    void testConsumeRecordWithValueLengthLimit(Integer maxValueLength, int responseValueLength) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        String h1Value = "h".repeat(100);
        String key = "k".repeat(100);
        String value = "v".repeat(100);
        recordUtils.produceRecord(topicName, null, Map.of("h1", h1Value), key, value);
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);
        Map<String, Object> queryParams = new HashMap<>(1);
        if (maxValueLength != null) {
            queryParams.put("maxValueLength", maxValueLength);
        }

        whenRequesting(req -> req
                .queryParams(queryParams)
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(1))
            .body("data[0].attributes.headers", hasEntry(equalTo("h1"), equalTo(h1Value.subSequence(0, responseValueLength))))
            .body("data[0].attributes.key", equalTo(key.subSequence(0, responseValueLength)))
            .body("data[0].attributes.value", equalTo(value.subSequence(0, responseValueLength)));
    }

    @Test
    void testConsumeRecordWithOffsetBeforeBeginning() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        Stream.of("first", "second", "third", "fourth")
            .forEach(msg -> recordUtils.produceRecord(topicName, null, null, null, msg));

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 4);

        topicUtils.deleteRecords(topicName, 0, 3);

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 1);

        whenRequesting(req -> req
                .param("filter[offset]", "gte,0")
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(1))
            .body("data[0].attributes.offset", is(equalTo(3)))
            .body("data[0].attributes.value", is(equalTo("fourth")));
    }

    @Test
    void testConsumeRecordFromTimestampAcrossPartitions() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 3);

        Instant ts1 = Instant.now();
        recordUtils.produceRecord(topicName, 0, ts1, null, null, "message A");
        recordUtils.produceRecord(topicName, 1, ts1, null, null, "message A");
        recordUtils.produceRecord(topicName, 2, ts1, null, null, "message A");

        Instant ts2 = ts1.plusSeconds(1);
        recordUtils.produceRecord(topicName, 0, ts2, null, null, "message B");
        recordUtils.produceRecord(topicName, 1, ts2, null, null, "message B");
        recordUtils.produceRecord(topicName, 2, ts2, null, null, "message B");

        Instant ts3 = ts2.plusSeconds(1);
        recordUtils.produceRecord(topicName, 0, ts3, null, null, "message C");
        recordUtils.produceRecord(topicName, 1, ts3, null, null, "message C");
        recordUtils.produceRecord(topicName, 2, ts3, null, null, "message C");

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 9);

        AtomicInteger offset = new AtomicInteger(0);

        Stream.of(ts1, ts2, ts3).forEach(ts -> {
            int expectedOffset = offset.getAndIncrement();

            whenRequesting(req -> req
                    .param("filter[timestamp]", "gte," + ts.toString())
                    .param("page[size]", 3)
                    .get("", clusterId1, topicIds.get(topicName)))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data", hasSize(3))
                .body("data[0].attributes", allOf(hasEntry("partition", 0), hasEntry("offset", expectedOffset)))
                .body("data[1].attributes", allOf(hasEntry("partition", 1), hasEntry("offset", expectedOffset)))
                .body("data[2].attributes", allOf(hasEntry("partition", 2), hasEntry("offset", expectedOffset)));
        });
    }

    @Test
    void testProduceRecordSimple() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("value", "TEST")))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.attributes.value", is("TEST"));
    }

    @Test
    void testProduceRecordSimpleWithPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("partition", 1)
                                        .add("value", "TEST")))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(1))
            .body("data.attributes.offset", is(0))
            .body("data.attributes.value", is("TEST"));
    }

    @ParameterizedTest
    @CsvSource({
        "'hdr1', ", // null value
        "'hdr2', 'hdr2Value'",
        "'hdr3', ''",
    })
    void testProduceRecordWithHeaders(String headerName, String headerValue) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("value", "TEST")
                                        .add("headers", Json.createObjectBuilder()
                                                .add(headerName,
                                                     headerValue == null
                                                         ? JsonValue.NULL
                                                         : Json.createValue(headerValue)))))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.attributes.value", is("TEST"))
            .body("data.attributes.headers." + headerName, is(headerValue));
    }

    @Test
    void testProduceRecordWithInvalidPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("partition", 1) // only partition 0 is valid
                                        .add("value", "TEST")))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"))
            .body("errors[0].source.pointer", is("/data/attributes/partition"));
    }

    @Test
    void testProduceRecordWithAvroFormat() {
        var registryClient = kafkaContexts.get(clusterId1).schemaRegistryContext().registryClient();

        final String keyArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String keySchema = """
              {
                "namespace": "console.avro",
                "type": "record",
                "name": "name_%s",
                "fields": [
                  {
                    "name": "key1",
                    "type": "string"
                  }
                ]
              }
              """
              .formatted(keyArtifactId);

        final String valueArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String valueSchema = """
              {
                "namespace": "console.avro",
                "type": "record",
                "name": "name_%s",
                "fields": [
                  {
                    "name": "value1",
                    "type": "string"
                  }
                ]
              }
              """
              .formatted(valueArtifactId);

        registryClient.createArtifact("default", keyArtifactId, ArtifactType.AVRO, new ByteArrayInputStream(keySchema
                .getBytes()));

        registryClient.createArtifact("default", valueArtifactId, ArtifactType.AVRO, new ByteArrayInputStream(valueSchema
                .getBytes()));


        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        JsonObject requestBody = Json.createObjectBuilder()
                .add("data", Json.createObjectBuilder()
                        .add("type", "records")
                        .add("relationships", Json.createObjectBuilder()
                                .add("keySchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                .add("coordinates", "default:" + keyArtifactId)))
                                .add("valueSchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                .add("coordinates", "default:" + valueArtifactId))))
                        .add("attributes", Json.createObjectBuilder()
                                .add("key", """
                                        {
                                          "key1": "value-of-key1",
                                          "field2": "field-not-in-the-schema"
                                        }""")
                                .add("value", """
                                        {
                                          "value1": "value-of-value1",
                                          "field2": "field-not-in-the-schema"
                                        }""")))
                .build();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(requestBody.toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.relationships.keySchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data.relationships.keySchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + keyArtifactId))
            .body("data.relationships.valueSchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data.relationships.valueSchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + valueArtifactId));

        var recordsResponse = whenRequesting(req -> req.get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(1))
            .body("data[0].attributes.partition", is(0))
            .body("data[0].attributes.offset", is(0))
            .body("data[0].relationships.keySchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data[0].relationships.keySchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + keyArtifactId))
            .body("data[0].relationships.valueSchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data[0].relationships.valueSchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + valueArtifactId))
            .body("data[0].attributes.key", is("""
                    {"key1":"value-of-key1"}"""))
            .body("data[0].attributes.value", is("""
                    {"value1":"value-of-value1"}"""))
            .extract();

        assertSchemaContent(keySchema, valueSchema, recordsResponse, (exp, act) -> {
            JSONAssert.assertEquals(exp, act, true);
        });
    }

    @Test
    void testProduceRecordWithProtobufFormat() {
        var registryClient = kafkaContexts.get(clusterId1).schemaRegistryContext().registryClient();

        final String keyArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String keySchema = """
                message name_%s {
                optional string key1 = 1;
              }
              """
              .formatted(keyArtifactId);

        final String valueArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String valueSchema = """
                message some_other_name_%s {
                optional string field1 = 1;
              }
              message name_%s {
                optional string value1 = 1;
              }
              """
              .formatted(valueArtifactId, valueArtifactId);

        registryClient.createArtifact("default", keyArtifactId, ArtifactType.PROTOBUF, new ByteArrayInputStream(keySchema
                .getBytes()));

        registryClient.createArtifact("default", valueArtifactId, ArtifactType.PROTOBUF, new ByteArrayInputStream(valueSchema
                .getBytes()));


        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        JsonObject requestBody = Json.createObjectBuilder()
                .add("data", Json.createObjectBuilder()
                        .add("type", "records")
                        .add("relationships", Json.createObjectBuilder()
                                .add("keySchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                // messageType omitted since there is only 1 in the key schema
                                                .add("coordinates", "default:" + keyArtifactId)))
                                .add("valueSchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                .add("coordinates", "default:" + valueArtifactId)
                                                .add("messageType", "name_" + valueArtifactId))))
                        .add("attributes", Json.createObjectBuilder()
                                .add("key", """
                                        {
                                          "key1": "value-of-key1",
                                          "field2": "field-not-in-the-schema"
                                        }""")
                                .add("value", """
                                        {
                                          "value1": "value-of-value1",
                                          "field2": "field-not-in-the-schema"
                                        }""")))
                .build();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(requestBody.toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.relationships.keySchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data.relationships.keySchema.meta.name", is("name_" + keyArtifactId))
            .body("data.relationships.valueSchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data.relationships.valueSchema.meta.name", is("name_" + valueArtifactId));

        var recordsResponse = whenRequesting(req -> req.get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(1))
            .body("data[0].attributes.partition", is(0))
            .body("data[0].attributes.offset", is(0))
            .body("data[0].relationships.keySchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data[0].relationships.keySchema.meta.name", is("name_" + keyArtifactId))
            .body("data[0].attributes.key", is("""
                    {"key1":"value-of-key1"}"""))
            .body("data[0].relationships.valueSchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data[0].relationships.valueSchema.meta.name", is("name_" + valueArtifactId))
            .body("data[0].attributes.value", is("""
                    {"value1":"value-of-value1"}"""))
            .extract();

        assertSchemaContent(keySchema, valueSchema, recordsResponse, Assertions::assertEquals);
    }

    private void assertSchemaContent(
            String expectedKeySchema,
            String expectedValueSchema,
            ExtractableResponse<?> response,
            BiConsumer<String, String> assertion) {
        String keySchemaLink = response.jsonPath().getString("data[0].relationships.keySchema.links.content");

        String actualKeySchema = RestAssured.given()
            .basePath(keySchemaLink)
            .when().get()
            .then()
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .extract()
            .asString();

        assertion.accept(expectedKeySchema, actualKeySchema);

        String valueSchemaLink = response.jsonPath().getString("data[0].relationships.valueSchema.links.content");

        String actualValueSchema = RestAssured.given()
            .basePath(valueSchemaLink)
            .when().get()
            .then()
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .extract()
            .asString();

        assertion.accept(expectedValueSchema, actualValueSchema);
    }
}
