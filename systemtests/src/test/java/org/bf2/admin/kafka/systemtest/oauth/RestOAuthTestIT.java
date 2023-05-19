package org.bf2.admin.kafka.systemtest.oauth;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.systemtest.TestOAuthProfile;
import org.bf2.admin.kafka.systemtest.deployment.DeploymentManager.UserType;
import org.bf2.admin.kafka.systemtest.utils.MetricsUtils;
import org.bf2.admin.kafka.systemtest.utils.TokenUtils;
import org.bf2.admin.kafka.systemtest.utils.TopicUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.bf2.admin.kafka.systemtest.utils.ErrorTypeMatcher.matchesError;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(TestOAuthProfile.class)
class RestOAuthTestIT {

    static TokenUtils tokenUtils;
    static String initialToken;
    static long initialTokenExpiresAt;

    @TestHTTPResource("/metrics")
    URL metricsUrl;

    @Inject
    Config config;

    TopicUtils topicUtils;
    MetricsUtils metricsUtils;

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
        this.metricsUtils = new MetricsUtils(metricsUrl);
    }

    @Test
    void testListWithValidToken() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items", hasSize(topicNames.size()))
            .body("items.name", containsInAnyOrder(topicNames.toArray(String[]::new)));
    }

    @Test
    void testListUnauthorizedUser() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items", hasSize(0))
            .body("total", equalTo(0));
    }

    @Test
    void testTopicsListWithInvalidToken() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.invalidAuthorizationHeader())
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
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
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void testDescribeSingleTopicAuthorized() {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;
        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .get(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("name", equalTo(topicName))
            .body("partitions.size()", equalTo(numPartitions));
    }

    @Test
    void testDescribeSingleTopicUnauthorized() {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;
        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);
        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
        .when()
            .get(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));
    }

    @Test
    void testCreateTopicAuthorized() {
        String topicName = UUID.randomUUID().toString();
        int numPartitions = 3;
        String minInSyncReplicas = "1";

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .body(TopicUtils.buildNewTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.CREATED.getStatusCode())
        .assertThat()
            .header("Location", equalTo("/api/v1/topics/" + topicName))
            .body("name", equalTo(topicName))
            .body("isInternal", equalTo(false))
            .body("partitions.size()", equalTo(numPartitions))
            .body("config.find { it.key == 'min.insync.replicas' }.value", equalTo(minInSyncReplicas));
    }

    @Test
    void testCreateTopicUnauthorized() {
        String topicName = UUID.randomUUID().toString();
        int numPartitions = 3;
        String minInSyncReplicas = "1";
        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
            .body(TopicUtils.buildNewTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));
    }

    @Test
    void testCreateTopicUnauthorizedIncrementsMetric() {
        List<String> preMetrics = metricsUtils.getMetrics();
        String topicName = UUID.randomUUID().toString();
        int numPartitions = 3;
        String minInSyncReplicas = "1";

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
            .body(TopicUtils.buildNewTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            // User not authorized to view the group, therefore the list is empty
            .statusCode(Status.FORBIDDEN.getStatusCode());

        List<String> postMetrics = metricsUtils.getMetrics();
        assertEquals(1, metricsUtils.getMetricDiff(preMetrics, postMetrics, "^failed_requests_total\\{.*status_code=\"403\"").intValueExact());
    }

    @Test
    @Disabled("Requires fix for https://github.com/quarkusio/quarkus/issues/22971")
    void testCreateTopicInvalidTokenIncrementsMetric() {
        List<String> preMetrics = metricsUtils.getMetrics();
        String topicName = UUID.randomUUID().toString();
        int numPartitions = 3;
        String minInSyncReplicas = "1";

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.invalidAuthorizationHeader())
            .body(TopicUtils.buildNewTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            // User not authorized to view the group, therefore the list is empty
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        List<String> postMetrics = metricsUtils.getMetrics();
        assertEquals(1, metricsUtils.getMetricDiff(preMetrics, postMetrics, "^failed_requests_total\\{.*status_code=\"401\"").intValueExact());
    }

    @Test
    void testTopicDeleteSingleAuthorized() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .delete(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode());

        topicUtils.assertNoTopicsExist();
    }

    @Test
    void testTopicDeleteSingleUnauthorized() {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;
        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);
        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
        .when()
            .delete(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));

        // Confirm it still exists
        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .get(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("name", equalTo(topicName))
            .body("partitions.size()", equalTo(numPartitions));
    }

    @Test
    void testUpdateTopicAuthorized() {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;

        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);

        // Original value is "1" by `createTopics`
        String minInSyncReplicas = "2";

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
            .body(TopicUtils.buildUpdateTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
        .when()
            .patch(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("name", equalTo(topicName))
            .body("isInternal", equalTo(false))
            .body("partitions.size()", equalTo(numPartitions))
            .body("config.find { it.key == 'min.insync.replicas' }.value", equalTo(minInSyncReplicas));
    }

    @Test
    void testUpdateTopicUnauthorized() {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;
        String minInSyncReplicas = "1";
        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);

        given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
            // Try changing `min.insync.replicas` to 2
            .body(TopicUtils.buildUpdateTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", "2")).toString())
        .when()
            .patch(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));

        // Confirm unchanged
        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OWNER.getUsername()))
        .when()
            .get(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("name", equalTo(topicName))
            .body("isInternal", equalTo(false))
            .body("partitions.size()", equalTo(numPartitions))
            .body("config.find { it.key == 'min.insync.replicas' }.value", equalTo(minInSyncReplicas));
    }

}
