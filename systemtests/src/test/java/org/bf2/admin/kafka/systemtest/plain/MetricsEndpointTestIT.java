package org.bf2.admin.kafka.systemtest.plain;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.bf2.admin.kafka.systemtest.TestPlainProfile;
import org.bf2.admin.kafka.systemtest.utils.MetricsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(TestPlainProfile.class)
class MetricsEndpointTestIT {

    @TestHTTPResource("/metrics")
    URL metricsUrl;

    MetricsUtils metricsUtils;

    @BeforeEach
    void setup() {
        this.metricsUtils = new MetricsUtils(metricsUrl);
    }

    @Test
    void testListTopicMetrics() {
        final int number = 3;
        List<String> preMetrics = metricsUtils.getMetrics();

        listTopics(number, Status.OK);

        List<String> postMetrics = metricsUtils.getMetrics();
        assertMetricDiff(number, preMetrics, postMetrics, "^list_topics_requests_total[ {]");
    }

    @Test
    void testCreateTopicMetrics() {
        final int number = 4;
        List<String> preMetrics = metricsUtils.getMetrics();

        List<String> topicNames = IntStream.range(0, number)
                .mapToObj(Integer::toString)
                .map("create_topic_metrics_"::concat)
                .collect(Collectors.toList());

        createTopics(topicNames, 1, Status.CREATED);

        List<String> postMetrics = metricsUtils.getMetrics();
        assertMetricDiff(number, preMetrics, postMetrics, "^create_topic_requests_total[ {]");
    }

    @Test
    void testDeleteTopicMetrics() {
        final int number = 2;
        List<String> preMetrics = metricsUtils.getMetrics();

        List<String> topicNames = IntStream.range(0, number)
                .mapToObj(Integer::toString)
                .map("delete_topic_metrics_"::concat)
                .collect(Collectors.toList());

        createTopics(topicNames, 1, Status.CREATED);
        deleteTopics(topicNames, Status.OK);

        List<String> postMetrics = metricsUtils.getMetrics();
        assertMetricDiff(number, preMetrics, postMetrics, "^delete_topic_requests_total[ {]");
    }

    @Test
    void testDescribeTopicMetrics() throws Exception {
        final int number = 3;
        List<String> preMetrics = metricsUtils.getMetrics();

        List<String> topicNames = IntStream.range(0, number)
                .mapToObj(Integer::toString)
                .map("describe_topic_metrics_"::concat)
                .collect(Collectors.toList());

        createTopics(topicNames, 1, Status.CREATED);
        describeTopics(topicNames, Status.OK);

        List<String> postMetrics = metricsUtils.getMetrics();
        assertMetricDiff(number, preMetrics, postMetrics, "^describe_topic_requests_total[ {]");
    }

    @Test
    void testUpdateTopicMetrics() {
        final int number = 2;
        List<String> preMetrics = metricsUtils.getMetrics();

        List<String> topicNames = IntStream.range(0, number)
                .mapToObj(Integer::toString)
                .map("update_topic_metrics_"::concat)
                .collect(Collectors.toList());

        createTopics(topicNames, 1, Status.CREATED);
        updateTopics(topicNames);

        List<String> postMetrics = metricsUtils.getMetrics();
        assertMetricDiff(number, preMetrics, postMetrics, "^update_topic_requests_total[ {]");
    }

    @Test
    void testAdminSucceededAndFailedMetrics() {
        List<String> preMetrics = metricsUtils.getMetrics();

        Map<String, Integer> failedRequests = new HashMap<>();
        int successfulRequests = 0;

        deleteTopics(List.of("no_such_topic_0", "no_such_topic_1"), Status.NOT_FOUND);
        failedRequests.merge("404", 2, Integer::sum);

        createTopics(List.of("good_topic_0", "good_topic_1"), 1, Status.CREATED);
        successfulRequests += 2;

        deleteTopics(List.of("good_topic_0", "good_topic_1"), Status.OK);
        successfulRequests += 2;

        createTopics(List.of("bad_topic_0", "bad_topic_1"), -1, Status.BAD_REQUEST);
        failedRequests.merge("400", 2, Integer::sum);

        listTopics(1, Status.OK);
        successfulRequests++;

        int totalFailedRequests = failedRequests.values().stream().mapToInt(Integer::intValue).sum();
        List<String> postMetrics = metricsUtils.getMetrics();

        assertMetricDiff(successfulRequests, preMetrics, postMetrics, "^succeeded_requests_total[ {]");
        assertMetricDiff(successfulRequests + totalFailedRequests, preMetrics, postMetrics, "^requests_total[ {]");
        assertMetricDiff(failedRequests.get("400"), preMetrics, postMetrics, "^failed_requests_total\\{.*status_code=\"400\"");
        assertMetricDiff(failedRequests.get("404"), preMetrics, postMetrics, "^failed_requests_total\\{.*status_code=\"404\"");
    }

    void assertMetricDiff(int expectedDiff, List<String> preMetrics, List<String> postMetrics, String nameRegex) {
        int actualDiff = metricsUtils.getMetricDiff(preMetrics, postMetrics, nameRegex).intValueExact();
        assertEquals(expectedDiff, actualDiff,
            () -> {
                String preMsg = String.join("\n\t", preMetrics);
                String postMsg = String.join("\n\t", postMetrics);
                return String.format("Unexpected metrics diff for /%s/.\nPre-metrics: %s\nPost-metrics: %s", nameRegex, preMsg, postMsg);
            });
    }

    void listTopics(int times, Status expectedStatus) {
        IntStream.range(0, times).forEach(i ->
            when()
                .get("/rest/topics")
            .then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.getStatusCode()));
    }

    void createTopics(List<String> names, int numPartitions, Status expectedStatus) {
        names.forEach(name -> {
            given()
                .body(buildTopicRequest(name, numPartitions, Map.of("min.insync.replicas", "1")).toString())
                .contentType(ContentType.JSON)
                .log().ifValidationFails()
                .post("/rest/topics")
            .then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.getStatusCode());
        });
    }

    void describeTopics(List<String> names, Status expectedStatus) {
        names.forEach(name -> {
            given()
                .log().ifValidationFails()
                .get("/rest/topics/" + name)
            .then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.getStatusCode());
        });
    }

    void updateTopics(List<String> names) {
        names.forEach(name -> {
            given()
                .body(buildTopicRequest(name, 6, Map.of("min.insync.replicas", "2")).toString())
                .contentType(ContentType.JSON)
                .log().ifValidationFails()
                .patch("/rest/topics/" + name)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode());
        });
    }

    void deleteTopics(List<String> names, Status expectedStatus) {
        names.forEach(name -> {
            given()
                .log().ifValidationFails()
                .delete("/rest/topics/" + name)
            .then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.getStatusCode());
        });
    }

    JsonObject buildTopicRequest(String name, int numPartitions, Map<String, String> config) {
        JsonArrayBuilder configBuilder = Json.createArrayBuilder();

        config.forEach((key, value) ->
            configBuilder.add(Json.createObjectBuilder()
                              .add("key", key)
                              .add("value", value)));

        return Json.createObjectBuilder()
            .add("name", name)
            .add("settings", Json.createObjectBuilder()
                 .add("numPartitions", numPartitions)
                 .add("config", configBuilder))
            .build();
    }


}
