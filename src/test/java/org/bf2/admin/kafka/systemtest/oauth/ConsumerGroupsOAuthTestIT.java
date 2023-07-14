package org.bf2.admin.kafka.systemtest.oauth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.systemtest.TestOAuthProfile;
import org.bf2.admin.kafka.systemtest.deployment.DeploymentManager.UserType;
import org.bf2.admin.kafka.systemtest.utils.ConsumerUtils;
import org.bf2.admin.kafka.systemtest.utils.TokenUtils;
import org.bf2.admin.kafka.systemtest.utils.TopicUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.bf2.admin.kafka.systemtest.utils.ErrorTypeMatcher.matchesError;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@TestProfile(TestOAuthProfile.class)
class ConsumerGroupsOAuthTestIT {

    static final String CONSUMER_GROUP_COLLECTION_PATH = "/rest/consumer-groups";
    static final String CONSUMER_GROUP_PATH = "/rest/consumer-groups/{groupId}";

    @Inject
    Config config;

    static TokenUtils tokenUtils;
    static String initialToken;
    static long initialTokenExpiresAt;

    TopicUtils topicUtils;
    ConsumerUtils groupUtils;
    String batchId;

    @BeforeAll
    static void initialize() {
        tokenUtils = new TokenUtils(ConfigProvider.getConfig());
        JsonObject token = tokenUtils.getTokenObject(UserType.OWNER.getUsername());

        // Fetch one token before the tests begin to minimize wait time in expired token test
        initialToken = token.getString("access_token");
        initialTokenExpiresAt = System.currentTimeMillis() + (token.getInt("expires_in") * 1000);
    }

    @BeforeEach
    void setup() {
        String token = tokenUtils.getToken(UserType.OWNER.getUsername());
        topicUtils = new TopicUtils(config, token);
        topicUtils.deleteAllTopics();
        groupUtils = new ConsumerUtils(config, token);
        batchId = UUID.randomUUID().toString();
    }

    @Test
    void testConsumerGroupsListAuthorized() throws Exception {
        String consumerPath = "items.find { it.groupId == '%1$s' }.consumers.find { it.topic == '%2$s' && it.partition == %3$d }.memberId";
        String topicName1 = "t1" + batchId;
        String groupId1 = "g1 + batchId";
        String clientId1 = "c1" + batchId;
        String topicName2 = "t2" + batchId;
        String groupId2 = "g2" + batchId;
        String clientId2 = "c2" + batchId;

        groupUtils.consume(groupId1, topicName1, clientId1, 2, true);

        try (var openConsumer = groupUtils.consume(groupId2, topicName2, clientId2, 2, false)) {
            given()
                .log().ifValidationFails()
                .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .when()
                .get(CONSUMER_GROUP_COLLECTION_PATH)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("items", hasSize(2))
                .body("items.state", contains("EMPTY", "STABLE"))
                .body("items.groupId", contains(groupId1, groupId2))
                .body("items[0].consumers", hasSize(2))
                .body("items[1].consumers", hasSize(2))
                .body(String.format(consumerPath, groupId1, topicName1, 0), Matchers.nullValue()) // group 1 closed
                .body(String.format(consumerPath, groupId1, topicName1, 1), Matchers.nullValue()) // group 1 closed
                .body(String.format(consumerPath, groupId2, topicName2, 0), startsWith(clientId2)) // group 2 open
                .body(String.format(consumerPath, groupId2, topicName2, 1), startsWith(clientId2)); // group 2 open
        }
    }

    @Test
    void testConsumerGroupsListUnauthorized() throws Exception {
        String topicName1 = "t1" + batchId;
        String groupId1 = "g1" + batchId;
        String clientId1 = "c1" + batchId;
        groupUtils.consume(groupId1, topicName1, clientId1, 2, true);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            // User not authorized to view the group, therefore the list is empty
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items", hasSize(0))
            .body("page", equalTo(1))
            .body("size", equalTo(10))
            .body("total", equalTo(0));
    }

    @Test
    void testConsumerGroupsListWithInvalidToken() throws Exception {
        String topicName1 = "t1" + batchId;
        String groupId1 = "g1" + batchId;
        String clientId1 = "c1" + batchId;
        groupUtils.consume(groupId1, topicName1, clientId1, 2, true);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.invalidAuthorizationHeader())
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void testConsumerGroupsListWithMissingToken() throws Exception {
        String topicName1 = "t1" + batchId;
        String groupId1 = "g1" + batchId;
        String clientId1 = "c1" + batchId;
        groupUtils.consume(groupId1, topicName1, clientId1, 2, true);

        given()
            .log().ifValidationFails()
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void testConsumerGroupsDescribeAuthorized() throws Exception {
        String topicName = "t1" + batchId;
        String groupId = "g1" + batchId;
        String clientId = "c1" + batchId;
        int numPartitions = 2;

        try (var consumer = groupUtils.request()
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(2)
                .consume()) {

            given()
                .log().ifValidationFails()
                .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .when()
                .get(CONSUMER_GROUP_PATH, groupId)
            .then()
                .log().ifValidationFails()
                // User not authorized to view the group, therefore the list is empty
                .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("groupId", equalTo(groupId))
                .body("state", equalToIgnoringCase("stable")) // Consumer is still open
                .body("consumers", hasSize(numPartitions))
                .body("consumers.findAll { it }.groupId", contains(groupId, groupId))
                .body("consumers.findAll { it }.topic", contains(topicName, topicName))
                .body("consumers.findAll { it }.partition", contains(0, 1))
                .body("consumers.findAll { it }.memberId", contains(startsWith(clientId), startsWith(clientId)));
        }
    }

    @Test
    void testConsumerGroupsDescribeUnauthorized() {
        String topicName = "t1" + batchId;
        String groupId = "g1" + batchId;
        String clientId = "c1" + batchId;
        int numPartitions = 2;
        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        try (var consumer = groupUtils.request()
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(2)
                .consume()) {

            given()
                .log().ifValidationFails()
                .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
            .when()
                .get(CONSUMER_GROUP_PATH, groupId)
            .then()
                .log().ifValidationFails()
            .assertThat()
                .statusCode(expectedError.getHttpStatus().getStatusCode())
                .body("", matchesError(expectedError));
        }
    }

    @Test
    void testDescribeWithInvalidToken() {
        String topicName = "t1" + batchId;
        String groupId = "g1" + batchId;
        String clientId = "c1" + batchId;
        int numPartitions = 2;

        try (var consumer = groupUtils.request()
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(2)
                .consume()) {

            given()
                .log().ifValidationFails()
                .header(tokenUtils.invalidAuthorizationHeader())
            .when()
                .get(CONSUMER_GROUP_PATH, groupId)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());
        }
    }

    @Test
    void testDescribeWithMissingToken() {
        String topicName = "t1" + batchId;
        String groupId = "g1" + batchId;
        String clientId = "c1" + batchId;
        int numPartitions = 2;

        try (var consumer = groupUtils.request()
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(2)
                .consume()) {

            given()
                .log().ifValidationFails()
            .when()
                .get(CONSUMER_GROUP_PATH, groupId)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());
        }
    }

    @Test
    void testConsumerGroupsDeleteAuthorized() throws Exception {
        int groupCount = 5;
        String prefix = UUID.randomUUID().toString();

        List<String> groupIds = IntStream.range(0, groupCount)
            .mapToObj(i -> String.format("-%03d-%s", i, UUID.randomUUID().toString()))
            .map(suffix -> {
                String groupId = prefix + "-g-" + suffix;
                String topicName = "t-" + suffix;
                String clientId = "c-" + suffix;
                groupUtils.consume(groupId, topicName, clientId, 2, true);
                return groupId;
            })
            .collect(Collectors.toList());

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .delete(CONSUMER_GROUP_PATH, groupIds.get(0))
        .then()
            .log().ifValidationFails()
            .statusCode(Status.NO_CONTENT.getStatusCode());

        given()
            .queryParam("group-id-filter", prefix + "-g-")
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("items.size()", equalTo(groupCount - 1))
                .body("items.findAll { it }.groupId", contains(groupIds.subList(1, groupCount).toArray(String[]::new)));
    }

    @Test
    void testConsumerGroupsDeleteUnauthorized() throws Exception {
        int groupCount = 5;
        String prefix = UUID.randomUUID().toString();

        List<String> groupIds = IntStream.range(0, groupCount)
            .mapToObj(i -> String.format("-%03d-%s", i, UUID.randomUUID().toString()))
            .map(suffix -> {
                String groupId = prefix + "-g-" + suffix;
                String topicName = "t-" + suffix;
                String clientId = "c-" + suffix;
                groupUtils.consume(groupId, topicName, clientId, 2, true);
                return groupId;
            })
            .collect(Collectors.toList());

        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
        .when()
            .delete(CONSUMER_GROUP_PATH, groupIds.get(0))
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));

        given()
            .queryParam("group-id-filter", prefix + "-g-")
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("items.size()", equalTo(groupCount))
                .body("items.findAll { it }.groupId", contains(groupIds.toArray(String[]::new)));
    }

    @Test
    void testDeleteWithInvalidToken() throws Exception {
        int groupCount = 5;
        String prefix = UUID.randomUUID().toString();

        List<String> groupIds = IntStream.range(0, groupCount)
            .mapToObj(i -> String.format("-%03d-%s", i, UUID.randomUUID().toString()))
            .map(suffix -> {
                String groupId = prefix + "-g-" + suffix;
                String topicName = "t-" + suffix;
                String clientId = "c-" + suffix;
                groupUtils.consume(groupId, topicName, clientId, 2, true);
                return groupId;
            })
            .collect(Collectors.toList());

        given()
            .log().ifValidationFails()
            .header(tokenUtils.invalidAuthorizationHeader())
        .when()
            .delete(CONSUMER_GROUP_PATH, groupIds.get(0))
        .then()
            .log().ifValidationFails()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .queryParam("group-id-filter", prefix + "-g-")
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("items.size()", equalTo(groupCount))
                .body("items.findAll { it }.groupId", contains(groupIds.toArray(String[]::new)));
    }

    @Test
    void testListWithExpiredToken() throws InterruptedException {
        // Wait for token to expire
        long sleepDuration = initialTokenExpiresAt - System.currentTimeMillis();

        if (sleepDuration > 0) {
            Thread.sleep(sleepDuration);
        }

        given()
            .log().ifValidationFails()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + initialToken)
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }
}
