package com.github.eyefloaters.console.kafka.systemtest.oauth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static com.github.eyefloaters.console.kafka.systemtest.utils.ErrorTypeMatcher.matchesError;

import org.eclipse.microprofile.config.Config;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.eyefloaters.console.kafka.systemtest.TestOAuthProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager.UserType;
import com.github.eyefloaters.console.kafka.systemtest.utils.ConsumerUtils;
import com.github.eyefloaters.console.kafka.systemtest.utils.TokenUtils;
import com.github.eyefloaters.console.legacy.model.ErrorType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(TestOAuthProfile.class)
class PartitionsOffsetOauthIT {

    static final String CONSUMER_GROUP_RESET_PATH = "/api/v1/consumer-groups/{groupId}/reset-offset";

    @Inject
    Config config;

    TokenUtils tokenUtils;
    ConsumerUtils consumerUtils;

    @BeforeEach
    void setup() {
        tokenUtils = new TokenUtils(config);
        consumerUtils = new ConsumerUtils(config, tokenUtils.getToken(UserType.OWNER.getUsername()));
    }

    @Test
    void testResetOffsetToStartWithOpenClient() {
        final String batchId = UUID.randomUUID().toString();
        final String topicName = "t-" + batchId;
        final String groupId = "g-" + batchId;
        final String clientId = "c-" + batchId;
        final ErrorType expected = ErrorType.GROUP_NOT_EMPTY;

        try (var consumer = consumerUtils.request().groupId(groupId).topic(topicName).clientId(clientId).messagesPerTopic(5).consume()) {
            given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
                .body(Json.createObjectBuilder()
                      .add("offset", "earliest")
                      .add("value", "")
                      .add("topics", Json.createArrayBuilder()
                           .add(Json.createObjectBuilder()
                                .add("topic", topicName)))
                      .build()
                      .toString())
            .when()
                .post(CONSUMER_GROUP_RESET_PATH, groupId)
            .then()
                .log().ifValidationFails()
            .assertThat()
                .statusCode(expected.getHttpStatus().getStatusCode())
                .body("", matchesError(expected, containsString("partition 0 has connected clients")));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "topic1 | topicBad | 1 | ",
        "topic1 | topic1   | 5 | 'Topic topic1%s, partition 5 is not valid'",
    })
    void testResetOffsetToStartWithInvalidTopicPartition(String topicPrefix, String topicResetPrefix, int resetPartition, String detail) {
        final String batchId = UUID.randomUUID().toString();
        final String topicName = topicPrefix + batchId;
        final String topicResetName = topicResetPrefix + batchId;
        final String groupId = "g-" + batchId;
        final String clientId = "c-" + batchId;
        final ErrorType expected = ErrorType.TOPIC_PARTITION_INVALID;
        final String formattedDetail = detail != null ? String.format(detail, batchId) : null;

        consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .clientId(clientId)
            .messagesPerTopic(1)
            .autoClose(true)
            .consume();

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .body(Json.createObjectBuilder()
                  .add("offset", "earliest")
                  .add("value", "")
                  .add("topics", Json.createArrayBuilder()
                       .add(Json.createObjectBuilder()
                            .add("topic", topicResetName)
                            .add("partitions", Json.createArrayBuilder().add(resetPartition))))
                  .build()
                  .toString())
        .when()
            .post(CONSUMER_GROUP_RESET_PATH, groupId)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expected.getHttpStatus().getStatusCode())
            .body("", matchesError(expected, formattedDetail));
    }

    @Test
    void testResetOffsetUnauthorized() throws InterruptedException {
        final String batchId = UUID.randomUUID().toString();
        final String topicName = "t-" + batchId;
        final String groupId = "g-" + batchId;
        final String clientId = "c-" + batchId;
        final ErrorType expected = ErrorType.NOT_AUTHORIZED;

        consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .clientId(clientId)
            .messagesPerTopic(10)
            .autoClose(true)
            .consume();

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername())) // Other user without access to reset offsets
            .body(Json.createObjectBuilder()
                  .add("offset", "latest")
                  .add("value", "")
                  .add("topics", Json.createArrayBuilder()
                       .add(Json.createObjectBuilder()
                            .add("topic", topicName)))
                  .build()
                  .toString())
        .when()
            .post(CONSUMER_GROUP_RESET_PATH, groupId)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expected.getHttpStatus().getStatusCode())
            .body("", matchesError(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "earliest, 10, 10, '',  0,  10", // Reset to earliest offset
        "latest,   10,  5, '', 10,   0", // Reset to latest offset
        "absolute, 10, 10,  5,  5,   5"  // Reset to absolute offset
    })
    void testResetOffsetAuthorized(String offset, int produceCount, int consumeCount, String value, int expectedOffset, int expectedMessageConsumeCount) throws InterruptedException {
        final String batchId = UUID.randomUUID().toString();
        final String topicName = "t-" + batchId;
        final String groupId = "g-" + batchId;
        final String clientId = "c-" + batchId;

        consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .clientId(clientId)
            .messagesPerTopic(produceCount)
            .consumeMessages(consumeCount)
            .autoClose(true)
            .consume();

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .body(Json.createObjectBuilder()
                  .add("offset", offset)
                  .add("value", value)
                  .add("topics", Json.createArrayBuilder()
                       .add(Json.createObjectBuilder()
                            .add("topic", topicName)))
                  .build()
                  .toString())
        .when()
            .post(CONSUMER_GROUP_RESET_PATH, groupId)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items", hasSize(1))
            .body("items.find { it }.topic", equalTo(topicName))
            .body("items.find { it }.partition", equalTo(0))
            .body("items.find { it }.offset", equalTo(expectedOffset));

        var consumer = consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .createTopic(false)
            .clientId(clientId)
            .autoClose(true)
            .consume();

        assertEquals(expectedMessageConsumeCount, consumer.records().size());
    }

    @Test
    void testResetOffsetToTimestampAuthorized() throws InterruptedException {
        int firstBatchSize = 6;
        int secondBatchSize = 7;
        final String batchId = UUID.randomUUID().toString();
        final String topicName = "t-" + batchId;
        final String groupId = "g-" + batchId;
        final String clientId = "c-" + batchId;

        consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .clientId(clientId)
            .messagesPerTopic(firstBatchSize)
            .autoClose(true)
            .consume();

        Thread.sleep(3000);

        String resetTimestamp = DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC).withNano(0));

        consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .createTopic(false)
            .clientId(clientId)
            .messagesPerTopic(secondBatchSize)
            .autoClose(true)
            .consume();

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .body(Json.createObjectBuilder()
                  .add("offset", "timestamp")
                  .add("value", resetTimestamp)
                  .add("topics", Json.createArrayBuilder()
                       .add(Json.createObjectBuilder()
                            .add("topic", topicName)))
                  .build()
                  .toString())
        .when()
            .post(CONSUMER_GROUP_RESET_PATH, groupId)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items", hasSize(1))
            .body("items.find { it }.topic", equalTo(topicName))
            .body("items.find { it }.partition", equalTo(0))
            .body("items.find { it }.offset", equalTo(firstBatchSize));

        var consumer = consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .createTopic(false)
            .clientId(clientId)
            .autoClose(true)
            .consume();

        assertEquals(secondBatchSize, consumer.records().size());
    }

    @Test
    void testResetOffsetOnMultiplePartitionsAuthorized() throws Exception {
        final String batchId = UUID.randomUUID().toString();
        final String topicName = "t-" + batchId;
        final String groupId = "g-" + batchId;
        final String clientId = "c-" + batchId;

        var consumer1 = consumerUtils.request()
            .groupId(groupId)
            .topic(topicName, 3)
            .clientId(clientId)
            .messagesPerTopic(20)
            .autoClose(true)
            .consume();

        assertEquals(20, consumer1.records().size());

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .body(Json.createObjectBuilder()
                  .add("offset", "absolute")
                  .add("value", "0")
                  .add("topics", Json.createArrayBuilder()
                       .add(Json.createObjectBuilder()
                            .add("topic", topicName)
                            .add("partitions", Json.createArrayBuilder()
                                 .add(0)
                                 .add(1)
                                 .add(2))))
                  .build()
                  .toString())
        .when()
            .post(CONSUMER_GROUP_RESET_PATH, groupId)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items", hasSize(3))
            .body("items.findAll { it }.topic", Matchers.contains(topicName, topicName, topicName))
            .body("items.findAll { it }.partition", Matchers.containsInAnyOrder(0, 1, 2))
            .body("items.findAll { it }.offset", Matchers.contains(0, 0, 0));

        var consumer2 = consumerUtils.request()
            .groupId(groupId)
            .topic(topicName)
            .createTopic(false)
            .clientId(clientId)
            .autoClose(true)
            .consume();

        assertEquals(20, consumer2.records().size());

        // ConsumerRecord does not implement equals - string representation (brittle...)
        assertEquals(consumer1.records().toString(), consumer2.records().toString());
    }

}
