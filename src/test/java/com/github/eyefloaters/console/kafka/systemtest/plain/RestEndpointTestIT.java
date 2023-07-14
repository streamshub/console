package com.github.eyefloaters.console.kafka.systemtest.plain;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static com.github.eyefloaters.console.kafka.systemtest.utils.ErrorTypeMatcher.matchesError;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.KafkaUnsecuredResourceManager;
import com.github.eyefloaters.console.kafka.systemtest.utils.TopicUtils;
import com.github.eyefloaters.console.legacy.model.ErrorType;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(TestPlainProfile.class)
class RestEndpointTestIT {

    @Inject
    Config config;

    TopicUtils topicUtils;

    @BeforeEach
    void setup() {
        topicUtils = new TopicUtils(config, null);
        topicUtils.deleteAllTopics();
    }

    @Test
    void testTopicListAfterCreation() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items.size()", equalTo(topicNames.size()))
            .body("items.name", containsInAnyOrder(topicNames.toArray(String[]::new)));
    }

//    @Test
//    void testTopicListWithKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws InterruptedException {
//        HttpClient client = createHttpClient(vertx);
//        deployments.stopKafkaContainer();
//        client.request(HttpMethod.GET, publishedAdminPort, "localhost", "/rest/topics")
//                .compose(req -> req.send().onComplete(l -> testContext.verify(() -> {
//                    assertThat(testContext.failed()).isFalse();
//                    if (l.succeeded()) {
//                        assertThat(l.result().statusCode()).isEqualTo(ReturnCodes.KAFKA_DOWN.code);
//                    }
//                    assertStrictTransportSecurityDisabled(l.result(), testContext);
//                    testContext.completeNow();
//                })).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }

    @Test
    void testTopicListWithFilter() {
        String batchId = UUID.randomUUID().toString();

        List<String> topicNames = IntStream.range(0, 10)
                .mapToObj(i -> (i % 2 == 0 ? "-even-" : "-odd-") + i + "-")
                .map(name -> name + batchId)
                .collect(Collectors.toList());

        String[] exepectedNames = topicNames.stream()
                .filter(name -> name.contains("-even-"))
                .toArray(String[]::new);

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .queryParam("filter", "even")
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items.size()", equalTo(topicNames.size() / 2))
            .body("items.name", containsInAnyOrder(exepectedNames));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 3", // three items on first page
        "2, 2", // two items on second page
    })
    void testTopicListWithValidPagination(int page, int itemsOnPage) throws Exception {
        int totalTopics = 5;
        int pageSize = 3;
        List<String> topicNames = IntStream.range(0, totalTopics)
                .mapToObj(i -> UUID.randomUUID().toString())
                .sorted() // API sorts by name by default
                .collect(Collectors.toList());

        int expectedStart = (page - 1) * pageSize;
        int expectedEnd = expectedStart + pageSize;
        List<String> expectedNames = topicNames.subList(expectedStart, Math.min(expectedEnd, topicNames.size()));

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .queryParam("size", pageSize)
            .queryParam("page", page)
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("page", equalTo(page))
            .body("size", equalTo(pageSize))
            .body("total", equalTo(totalTopics))
            .body("items.size()", equalTo(itemsOnPage))
            .body("items.name", contains(expectedNames.toArray(String[]::new)));
    }

    @ParameterizedTest
    @CsvSource({
        " 3, Requested pagination incorrect. Beginning of list greater than full list size (5)",
        "-1, page must be greater than 0"
    })
    void testTopicListWithInvalidPagination(int page, String detail) throws Exception {
        int pageSize = 3;
        List<String> topicNames = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString())
                .sorted() // API sorts by name by default
                .collect(Collectors.toList());
        final ErrorType expectedError = ErrorType.INVALID_REQUEST;

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .queryParam("size", pageSize)
            .queryParam("page", page)
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError, detail));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 5 })
    void testTopicListWithLimit(int limit) {
        int totalTopics = 5;
        List<String> topicNames = IntStream.range(0, totalTopics)
                .mapToObj(i -> UUID.randomUUID().toString())
                .sorted() // API sorts by name by default
                .collect(Collectors.toList());

        List<String> expectedNames = topicNames.subList(0, limit);

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .queryParam("limit", limit)
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("offset", equalTo(0)) // default
            .body("limit", equalTo(limit))
            .body("count", equalTo(limit))
            .body("items.size()", equalTo(limit))
            .body("items.name", contains(expectedNames.toArray(String[]::new)));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 3, 4 })
    void testTopicListWithOffset(int offset) {
        int totalTopics = 3;
        List<String> topicNames = IntStream.range(0, totalTopics)
                .mapToObj(i -> UUID.randomUUID().toString())
                .sorted() // API sorts by name by default
                .collect(Collectors.toList());

        String[] expectedNames = offset < totalTopics ?
            topicNames.subList(offset, totalTopics).toArray(String[]::new) :
            null;

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        var response =
            given()
                .queryParam("offset", offset)
                .log().ifValidationFails()
            .when()
                .get(TopicUtils.TOPIC_COLLECTION_PATH)
            .then()
                .log().ifValidationFails();

        if (offset > totalTopics) {
            response.statusCode(Status.BAD_REQUEST.getStatusCode());
        } else {
            response.assertThat()
                .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("offset", equalTo(offset))
                .body("limit", equalTo(10)) // default
                .body("count", equalTo(totalTopics - offset))
                .body("items.size()", equalTo(totalTopics - offset))
                .body("items.name", expectedNames == null ? empty() : contains(expectedNames));
        }
    }

    @Test
    void testTopicListWithFilterNone() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .queryParam("filter", "zcfsada.*")
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("page", equalTo(1))
            .body("size", equalTo(10))
            .body("total", equalTo(0))
            .body("items.size()", equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 5 })
    void testTopicListWithSize(int size) {
        int totalTopics = 3;
        List<String> topicNames = IntStream.range(0, totalTopics)
                .mapToObj(i -> UUID.randomUUID().toString())
                .sorted() // API sorts by name by default
                .collect(Collectors.toList());

        List<String> expectedNames = topicNames.subList(0, Math.min(topicNames.size(), size));

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .queryParam("size", size)
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("page", equalTo(1)) // default
            .body("size", equalTo(size))
            .body("total", equalTo(totalTopics))
            .body("items.size()", equalTo(expectedNames.size()))
            .body("items.name", contains(expectedNames.toArray(String[]::new)));
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 'page must be greater than 0'",
        " 0, 'page must be greater than 0'",
        " 1, ",
        " 3, 'Requested pagination incorrect. Beginning of list greater than full list size (3)'",
        "50, 'Requested pagination incorrect. Beginning of list greater than full list size (3)'"
    })

    void testTopicListWithPage(int page, String errorDetail) throws Exception {
        int totalTopics = 3;
        List<String> topicNames = IntStream.range(0, totalTopics)
                .mapToObj(i -> UUID.randomUUID().toString())
                .sorted() // API sorts by name by default
                .collect(Collectors.toList());

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        var response =
            given()
                .queryParam("page", page)
                .log().ifValidationFails()
            .when()
                .get(TopicUtils.TOPIC_COLLECTION_PATH)
            .then()
                .log().ifValidationFails();

        if (errorDetail != null) {
            ErrorType expectedError = ErrorType.INVALID_REQUEST;
            response.assertThat()
                .statusCode(expectedError.getHttpStatus().getStatusCode())
                .body("", matchesError(expectedError, errorDetail));
        } else {
            response.assertThat()
                .statusCode(Status.OK.getStatusCode())
                .body("page", equalTo(page))
                .body("size", equalTo(10)) // default
                .body("total", equalTo(totalTopics))
                .body("items.size()", equalTo(totalTopics))
                .body("items.name", contains(topicNames.toArray(String[]::new)));
        }
    }

    @Test
    void testDescribeSingleTopic() throws Exception {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;
        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);

        given()
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("name", equalTo(topicName))
            .body("partitions.size()", equalTo(numPartitions));
    }

//    @Test
//    void testDescribeSingleTopicWithKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws Exception {
//        final String topicName = UUID.randomUUID().toString();
//        String queryReq = "/rest/topics/" + topicName;
//        deployments.stopKafkaContainer();
//
//        createHttpClient(vertx).request(HttpMethod.GET, publishedAdminPort, "localhost", queryReq)
//                .compose(req -> req.send().onComplete(l -> testContext.verify(() -> {
//                    if (l.succeeded()) {
//                        assertThat(l.result().statusCode()).isEqualTo(ReturnCodes.KAFKA_DOWN.code);
//                        assertStrictTransportSecurityDisabled(l.result(), testContext);
//                    }
//                    testContext.completeNow();
//                })).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }

    @Test
    void testDescribeNonExistingTopic() throws Exception {
        given()
            .log().ifValidationFails()
        .when()
            .get(TopicUtils.TOPIC_PATH, "nosuchtopic-" + UUID.randomUUID().toString())
        .then()
            .log().ifValidationFails()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void testCreateTopic() {
        String topicName = UUID.randomUUID().toString();
        int numPartitions = 3;
        String minInSyncReplicas = "1";

        given()
            .body(TopicUtils.buildNewTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
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

//    @Test
//    void testCreateTopicWithKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws InterruptedException {
//        Types.NewTopic topic = RequestUtils.getTopicObject(3);
//        deployments.stopKafkaContainer();
//
//        createHttpClient(vertx).request(HttpMethod.POST, publishedAdminPort, "localhost", "/rest/topics")
//                .compose(req -> req.putHeader("content-type", "application/json")
//                        .send(MODEL_DESERIALIZER.serializeBody(topic)).onComplete(l -> testContext.verify(() -> {
//                            if (l.succeeded()) {
//                                assertThat(l.result().statusCode()).isEqualTo(ReturnCodes.KAFKA_DOWN.code);
//                                assertStrictTransportSecurityDisabled(l.result(), testContext);
//                            }
//                            testContext.completeNow();
//                        })).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{./as}",
        "{ \"name\": \"name_ok\", \"settings\": { \"numPartitions\": \"not_a_number\" } }",
        "{{ \"name\": \"name_ok\", \"numPartitions\": 1 }", // extra opening brace
    })
    void testCreateTopicWithInvalidJson(String invalidJson) throws InterruptedException {
        ErrorType expectedError = ErrorType.INVALID_REQUEST_FORMAT;

        given()
            .body(invalidJson)
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));

        topicUtils.assertNoTopicsExist();
    }

    @Test
    void testCreateTopicWithInvalidName() {
        ErrorType expectedError = ErrorType.TOPIC_PARTITION_INVALID;

        given()
            .body(TopicUtils.buildNewTopicRequest("testTopic3_9-=", 3, Collections.emptyMap()).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));

        topicUtils.assertNoTopicsExist();
    }

    @Test
    void testCreateFaultTopic() {
        ErrorType expectedError = ErrorType.INVALID_CONFIGURATION;

        given()
            // Invalid value for configuration cleanup.policy: String must be one of: compact, delete
            .body(TopicUtils.buildNewTopicRequest(UUID.randomUUID().toString(), 3, Map.of("cleanup.policy", "true")).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError, "Invalid value true for configuration cleanup.policy: String must be one of: compact, delete"));

        topicUtils.assertNoTopicsExist();
    }

    @Test
    void testCreateDuplicatedTopic() {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;
        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);
        final ErrorType expectedError = ErrorType.TOPIC_DUPLICATED;

        given()
            .body(TopicUtils.buildNewTopicRequest(topicName, 1, Collections.emptyMap()).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 'numPartitions must be greater than 0'",
        " 0, 'numPartitions must be greater than 0'",
        KafkaUnsecuredResourceManager.EXCESSIVE_PARTITIONS + ", 'numPartitions must be between 1 and 100 (inclusive)'"
    })
    void testCreateTopicWithInvalidNumPartitions(int numPartitions, String errorDetail) throws InterruptedException {
        final String topicName = UUID.randomUUID().toString();
        final ErrorType expectedError = ErrorType.INVALID_REQUEST;

        given()
            .body(TopicUtils.buildNewTopicRequest(topicName, numPartitions, Collections.emptyMap()).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
        .when()
            .post(TopicUtils.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError, errorDetail));

        topicUtils.assertNoTopicsExist();
    }

    @Test
    void testTopicDeleteSingle() {
        final String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 2, Status.CREATED);

        given()
            .log().ifValidationFails()
        .when()
            .delete(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode());

        topicUtils.assertNoTopicsExist();
    }

//    @Test
//    void testTopicDeleteWithKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws Exception {
//        final String topicName = UUID.randomUUID().toString();
//        String query = "/rest/topics/" + topicName;
//        deployments.stopKafkaContainer();
//
//        createHttpClient(vertx).request(HttpMethod.DELETE, publishedAdminPort, "localhost", query)
//                .compose(req -> req.putHeader("content-type", "application/json")
//                        .send().onComplete(l -> testContext.verify(() -> {
//                            if (l.succeeded()) {
//                                assertThat(l.result().statusCode()).isEqualTo(ReturnCodes.KAFKA_DOWN.code);
//                            }
//                            assertStrictTransportSecurityDisabled(l.result(), testContext);
//                            testContext.completeNow();
//                        })).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }

    @Test
    void testTopicDeleteNotExisting() {
        final ErrorType expectedError = ErrorType.TOPIC_NOT_FOUND;

        given()
            .log().ifValidationFails()
        .when()
            .delete(TopicUtils.TOPIC_PATH, "nosuchtopic-" + UUID.randomUUID().toString())
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError));

        topicUtils.assertNoTopicsExist();
    }

    @Test
    void testUpdateTopic() {
        final String topicName = UUID.randomUUID().toString();
        final int numPartitions = 2;

        topicUtils.createTopics(List.of(topicName), numPartitions, Status.CREATED);

        // Original value is "1" by `createTopics`
        String minInSyncReplicas = "2";

        given()
            .body(TopicUtils.buildUpdateTopicRequest(topicName, numPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
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

//    @Test
//    void testUpdateTopicWithKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws InterruptedException {
//        final String topicName = UUID.randomUUID().toString();
//        final String configKey = "min.insync.replicas";
//        Types.Topic topic1 = new Types.Topic();
//        topic1.setName(topicName);
//        Types.ConfigEntry conf = new Types.ConfigEntry();
//        conf.setKey(configKey);
//        conf.setValue("2");
//        topic1.setConfig(Collections.singletonList(conf));
//        deployments.stopKafkaContainer();
//
//        createHttpClient(vertx).request(HttpMethod.PATCH, publishedAdminPort, "localhost", "/rest/topics/" + topicName)
//                .compose(req -> req.putHeader("content-type", "application/json")
//                        .send(MODEL_DESERIALIZER.serializeBody(topic1)).onSuccess(response -> {
//                            if (response.statusCode() !=  ReturnCodes.KAFKA_DOWN.code) {
//                                testContext.failNow("Status code " + response.statusCode() + " is not correct");
//                            }
//                            assertStrictTransportSecurityDisabled(response, testContext);
//                            testContext.completeNow();
//                        }).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }

    @Test
    void testIncreaseTopicPartitions() {
        final String topicName = UUID.randomUUID().toString();
        final int originalNumPartitions = 1;
        final int updatedNumPartitions = 3;

        topicUtils.createTopics(List.of(topicName), originalNumPartitions, Status.CREATED);

        // Original value is "1" by `createTopics`
        String minInSyncReplicas = "2";

        given()
            .body(TopicUtils.buildUpdateTopicRequest(topicName, updatedNumPartitions, Map.of("min.insync.replicas", minInSyncReplicas)).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
        .when()
            .patch(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("name", equalTo(topicName))
            .body("isInternal", equalTo(false))
            .body("partitions.size()", equalTo(updatedNumPartitions))
            .body("config.find { it.key == 'min.insync.replicas' }.value", equalTo(minInSyncReplicas));
    }

    @Test
    void testDecreaseTopicPartitions() throws Exception {
        final String topicName = UUID.randomUUID().toString();
        final int originalNumPartitions = 3;
        final int updatedNumPartitions = 2;
        final ErrorType expectedError = ErrorType.TOPIC_PARTITION_INVALID;

        topicUtils.createTopics(List.of(topicName), originalNumPartitions, Status.CREATED);

        given()
            .body(TopicUtils.buildUpdateTopicRequest(topicName, updatedNumPartitions, Collections.emptyMap()).toString())
            .contentType(ContentType.JSON)
            .log().ifValidationFails()
        .when()
            .patch(TopicUtils.TOPIC_PATH, topicName)
        .then()
            .log().ifValidationFails()
        .assertThat()
            .statusCode(expectedError.getHttpStatus().getStatusCode())
            .body("", matchesError(expectedError, "Topic currently has 3 partitions, which is higher than the requested 2."));
    }

    @Test
    void testOptionsRequestIncludesTwoHourMaxAge() {
        given()
            .header("Origin", "http://localhost")
            .header("Access-Control-Request-Method", "POST")
            .log().ifValidationFails()
            .when()
                .options(TopicUtils.TOPIC_COLLECTION_PATH)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .header("Access-Control-Max-Age",
                        equalTo(String.valueOf(Duration.ofHours(2).toSeconds())));
    }

    @Test
    void testUnknownEndpointReturnsNotFound() {
        final ErrorType expectedError = ErrorType.RESOURCE_NOT_FOUND;
        given()
            .log().ifValidationFails()
            .when()
                .get("/foo/bar")
            .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(Status.NOT_FOUND.getStatusCode())
                .body("", matchesError(expectedError));
    }
}
