package org.bf2.admin.kafka.systemtest.plain;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.bf2.admin.kafka.admin.RecordOperations;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.systemtest.TestPlainProfile;
import org.bf2.admin.kafka.systemtest.utils.ConsumerUtils;
import org.bf2.admin.kafka.systemtest.utils.RecordUtils;
import org.bf2.admin.kafka.systemtest.utils.TopicUtils;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.bf2.admin.kafka.systemtest.utils.ErrorTypeMatcher.matchesError;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;

@QuarkusTest
@TestProfile(TestPlainProfile.class)
class RecordEndpointTestIT {

    @Inject
    Config config;

    TopicUtils topicUtils;
    ConsumerUtils groupUtils;
    RecordUtils recordUtils;

    @BeforeEach
    void setup() {
        topicUtils = new TopicUtils(config, null);
        topicUtils.deleteAllTopics();
        groupUtils = new ConsumerUtils(config, null);
        recordUtils = new RecordUtils(config, null);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void testProduceRecordWithPartitionAndTimestamp(int partition) {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10).withNano(0);

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .body(recordUtils.buildRecordRequest(partition, timestamp, null, null, "record value").toString())
        .when()
            .post(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.CREATED.getStatusCode())
            .header("Location", stringContainsInOrder("/api/v1/topics/" + topicName + "/records", "partition=" + partition))
            .body("partition", equalTo(partition))
            .body("timestamp", equalTo(timestamp.toString()))
            .body("value", equalTo("record value"))
            .body("offset", greaterThanOrEqualTo(0));
    }


    @Test
    void testProduceRecordToInvalidTopic() {
        final String topicName = UUID.randomUUID().toString();
        ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10).withNano(0);
        final ErrorType expected = ErrorType.TOPIC_NOT_FOUND;

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .body(recordUtils.buildRecordRequest(0, timestamp, null, null, "record value").toString())
        .when()
            .post(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(expected.getHttpStatus().getStatusCode())
            .body("", matchesError(expected));
    }

    @Test
    void testProduceRecordToInvalidPartition() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 1, Status.CREATED);
        ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10).withNano(0);
        final ErrorType expected = ErrorType.TOPIC_PARTITION_INVALID;

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .body(recordUtils.buildRecordRequest(1, timestamp, null, null, "record value").toString())
        .when()
            .post(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expected.getHttpStatus().getStatusCode())
            .body("", matchesError(expected, "No such partition for topic " + topicName + ": 1"));
    }

    @Test
    void testConsumeRecordFromInvalidTopic() {
        final String topicName = UUID.randomUUID().toString();
        final ErrorType expected = ErrorType.TOPIC_NOT_FOUND;

        given()
            .log().ifValidationFails()
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(expected.getHttpStatus().getStatusCode())
            .body("", matchesError(expected));
    }

    @Test
    void testConsumeRecordFromInvalidPartition() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        final ErrorType expected = ErrorType.TOPIC_PARTITION_INVALID;

        given()
            .log().ifValidationFails()
            .queryParam("partition", -1)
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(expected.getHttpStatus().getStatusCode())
            .body("", matchesError(expected, "No such partition for topic " + topicName + ": -1"));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void testConsumeRecordWithInvalidLimit(int limit) {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        final ErrorType expected = ErrorType.INVALID_REQUEST;

        given()
            .log().ifValidationFails()
            .queryParam("limit", limit)
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(expected.getHttpStatus().getStatusCode())
            .body("", matchesError(expected, "limit must be greater than 0"));
    }

    @ParameterizedTest
    @CsvSource({
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-01T00:00:00.000Z, 2",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 1",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-03T00:00:00.000Z, 0"
    })
    void testConsumeRecordsAsOfTimestamp(ZonedDateTime ts1, ZonedDateTime ts2, ZonedDateTime tsSearch, int expectedResults) {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        recordUtils.produceRecord(topicName, ts1, null, "the-key1", "the-value1");
        recordUtils.produceRecord(topicName, ts2, null, "the-key2", "the-value2");

        given()
            .log().ifValidationFails()
            .queryParam("timestamp", tsSearch.toString())
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(expectedResults))
            .body("items", hasSize(expectedResults));
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
        topicUtils.createTopics(List.of(topicName), 1, Status.CREATED); // single partition
        for (int i = 0; i < 3; i++) {
            recordUtils.produceRecord(topicName, null, null, "the-key-" + i, "the-value-" + i);
        }

        given()
            .log().ifValidationFails()
            .queryParam("partition", 0)
            .queryParam("offset", startingOffset)
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(expectedResults))
            .body("items", hasSize(expectedResults));
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
        topicUtils.createTopics(List.of(topicName), 10, Status.CREATED); // single partition
        List<String> messageValues = new ArrayList<>();

        for (int i = 0; i < totalRecords; i++) {
            String value = "the-value-" + i;
            messageValues.add(value);
            recordUtils.produceRecord(topicName, null, null, "the-key-" + i, value);
        }
        Collections.reverse(messageValues);

        int resultCount = Math.min(limit, totalRecords);

        given()
            .log().ifValidationFails()
            .queryParam("limit", limit)
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(resultCount))
            .body("items", hasSize(resultCount))
            .body("items.findAll { it }.value", contains(messageValues.subList(0, resultCount).toArray(String[]::new)));
    }

    @Test
    void testConsumeRecordsIncludeOnlyHeaders() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        for (int i = 0; i < 3; i++) {
            recordUtils.produceRecord(topicName, null, Map.of("h1", "h1-value-" + i), "the-key-" + i, "the-value-" + i);
        }

        given()
            .log().ifValidationFails()
            .param("include", "headers")
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(3))
            .body("items", hasSize(3))
            .body("items", everyItem(allOf(aMapWithSize(2), hasEntry("kind", "Record"))))
            .body("items.headers", everyItem(hasKey("h1")));
    }

    @Test
    void testProduceAndConsumeRecordWithNullHeaderValue() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        Map<String, Object> headers = new HashMap<>();
        headers.put("h1", null);
        recordUtils.produceRecord(topicName, null, headers, "the-key", "the-value");

        given()
            .log().ifValidationFails()
            .param("include", "headers")
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(1))
            .body("items", hasSize(1))
            .body("items", everyItem(allOf(aMapWithSize(2), hasEntry("kind", "Record"))))
            .body("items[0].headers", hasEntry(equalTo("h1"), nullValue()));
    }

    @Test
    void testProduceAndConsumeRecordWithEmptyHeaderValue() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        Map<String, Object> headers = new HashMap<>();
        headers.put("h1", "");
        recordUtils.produceRecord(topicName, null, headers, "the-key", "the-value");

        given()
            .log().ifValidationFails()
            .param("include", "headers")
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(1))
            .body("items", hasSize(1))
            .body("items", everyItem(allOf(aMapWithSize(2), hasEntry("kind", "Record"))))
            .body("items[0].headers", hasEntry(equalTo("h1"), equalTo("")));
    }

    @Test
    void testConsumeRecordWithBinaryValue() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        final byte[] data = new byte[512];
        SecureRandom.getInstanceStrong().nextBytes(data);
        data[511] = -1; // ensure at least one byte invalid

        groupUtils.request()
            .topic(topicName, 1)
            .createTopic(true)
            .messagesPerTopic(1)
            .messageSupplier(i -> data)
            .valueSerializer(ByteArraySerializer.class.getName())
            .clientId(UUID.randomUUID().toString())
            .autoClose(true)
            .consume();

        given()
            .log().ifValidationFails()
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(1))
            .body("items", hasSize(1))
            .body("items[0].value", equalTo(RecordOperations.BINARY_DATA_MESSAGE));
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
        topicUtils.createTopics(List.of(topicName), 1, Status.CREATED);
        String h1Value = "h".repeat(100);
        String key = "k".repeat(100);
        String value = "v".repeat(100);
        recordUtils.produceRecord(topicName, null, Map.of("h1", h1Value), key, value);
        Map<String, Object> queryParams = new HashMap<>(1);
        if (maxValueLength != null) {
            queryParams.put("maxValueLength", maxValueLength);
        }

        given()
            .log().ifValidationFails()
            .queryParams(queryParams)
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .body("total", equalTo(1))
            .body("items", hasSize(1))
            .body("items[0].headers", hasEntry(equalTo("h1"), equalTo(h1Value.subSequence(0, responseValueLength))))
            .body("items[0].key", equalTo(key.subSequence(0, responseValueLength)))
            .body("items[0].value", equalTo(value.subSequence(0, responseValueLength)));
    }
}
