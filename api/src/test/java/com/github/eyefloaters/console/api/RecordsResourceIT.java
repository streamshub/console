package com.github.eyefloaters.console.api;

import java.io.IOException;
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
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.common.Uuid;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.TimeoutEvent;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.eyefloaters.console.api.service.RecordService;
import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.test.RecordHelper;
import com.github.eyefloaters.console.test.TestHelper;
import com.github.eyefloaters.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.kafka.Kafka;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
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
@QuarkusTestResource(KubernetesServerTestResource.class)
@TestHTTPEndpoint(RecordsResource.class)
@TestProfile(TestPlainProfile.class)
class RecordsResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;
    TopicHelper topicUtils;
    RecordHelper recordUtils;
    String clusterId1;
    String clusterId2;

    @BeforeEach
    void setup() throws IOException {
        URI bootstrapServers = URI.create(deployments.getExternalBootstrapServers());
        URI randomBootstrapServers = URI.create(config.getValue("console.kafka.testk2.bootstrap.servers", String.class));

        topicUtils = new TopicHelper(bootstrapServers, config, null);
        topicUtils.deleteAllTopics();

        utils = new TestHelper(bootstrapServers, config, null);
        recordUtils = new RecordHelper(bootstrapServers, config, null);

        clusterId1 = utils.getClusterId();
        clusterId2 = UUID.randomUUID().toString();

        client.resources(Kafka.class).inAnyNamespace().delete();
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka1", clusterId1, bootstrapServers))
            .create();
        // Second cluster is offline/non-existent
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka2", clusterId2, randomBootstrapServers))
            .create();
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2, Map.of("retention.ms", "-1"));
        recordUtils.produceRecord(topicName, ts1, null, "the-key1", "the-value1");
        recordUtils.produceRecord(topicName, ts2, null, "the-key2", "the-value2");

        await().atMost(10, TimeUnit.SECONDS)
            .conditionEvaluationListener(new ConditionEvaluationListener<Boolean>() {
                @Override
                public void conditionEvaluated(EvaluatedCondition<Boolean> condition) {
                    // No-op
                }
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2, Map.of("retention.ms", "-1"));
        recordUtils.produceRecord(topicName, ts1, null, "the-key1", "the-value1");
        recordUtils.produceRecord(topicName, ts2, null, "the-key2", "the-value2");

        await().atMost(10, TimeUnit.SECONDS)
            .conditionEvaluationListener(new ConditionEvaluationListener<Boolean>() {
                @Override
                public void conditionEvaluated(EvaluatedCondition<Boolean> condition) {
                    // No-op
                }
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1); // single partition
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1); // single partition
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 10);
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);
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
            .body("data", everyItem(allOf(is(aMapWithSize(2)), hasEntry("type", "records"))))
            .body("data.attributes.headers", everyItem(hasKey("h1")));
    }

    @Test
    void testConsumeRecordWithEmptyValue() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        final byte[] data = new byte[512];
        SecureRandom.getInstanceStrong().nextBytes(data);
        data[511] = -1; // ensure at least one byte invalid

        recordUtils.produceRecord(topicName, null, null, null, data);

        whenRequesting(req -> req.get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", hasSize(1))
            .body("data[0].attributes.value", is(equalTo(RecordService.BINARY_DATA_MESSAGE)));
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);
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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 3);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

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
        var topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

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
}
