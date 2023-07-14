package org.bf2.admin.kafka.systemtest.oauth;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.systemtest.TestOAuthProfile;
import org.bf2.admin.kafka.systemtest.deployment.DeploymentManager.UserType;
import org.bf2.admin.kafka.systemtest.utils.MetricsUtils;
import org.bf2.admin.kafka.systemtest.utils.RecordUtils;
import org.bf2.admin.kafka.systemtest.utils.TokenUtils;
import org.bf2.admin.kafka.systemtest.utils.TopicUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.bf2.admin.kafka.systemtest.utils.ErrorTypeMatcher.matchesError;

@QuarkusTest
@TestProfile(TestOAuthProfile.class)
class RecordEndpointOAuthTestIT {

    static TokenUtils tokenUtils;

    @TestHTTPResource("/metrics")
    URL metricsUrl;

    @Inject
    Config config;

    TopicUtils topicUtils;
    RecordUtils recordUtils;
    MetricsUtils metricsUtils;

    @BeforeAll
    static void initialize() {
        tokenUtils = new TokenUtils(ConfigProvider.getConfig());
    }

    @BeforeEach
    void setup() {
        String token = tokenUtils.getToken(UserType.OWNER.getUsername());
        topicUtils = new TopicUtils(config, token);
        topicUtils.deleteAllTopics();
        recordUtils = new RecordUtils(config, token);
        metricsUtils = new MetricsUtils(metricsUrl);
    }

    @Test
    void testProduceRecordAsUnauthenticatedUser() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.invalidAuthorizationHeader())
            .contentType(ContentType.JSON)
            .body(recordUtils.buildRecordRequest(0, null, null, null, "record value").toString())
        .when()
            .post(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void testProduceRecordAsUnauthorizedUser() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
            .contentType(ContentType.JSON)
            .body(recordUtils.buildRecordRequest(0, null, null, null, "record value").toString())
        .when()
            .post(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));
    }

    @Test
    void testConsumeRecordsAsUnauthenticatedUser() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);

        given()
            .log().ifValidationFails()
            .header(tokenUtils.invalidAuthorizationHeader())
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void testConsumeRecordsAsUnauthorizedUser() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);
        final ErrorType expectedError = ErrorType.NOT_AUTHORIZED;

        given()
            .log().ifValidationFails()
            .header(tokenUtils.authorizationHeader(UserType.OTHER.getUsername()))
        .when()
            .get(RecordUtils.RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));
    }
}
