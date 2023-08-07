package com.github.eyefloaters.console.test;

import io.restassured.http.ContentType;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicListing;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.kafka.systemtest.utils.ClientsConfig;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

public class TopicHelper {

    public static final String TOPIC_COLLECTION_PATH = "/api/clusters/{clusterId}/topics";
    public static final String TOPIC_PATH = "/api/clusters/{clusterId}/topics/{topicName}";

    static final Logger log = Logger.getLogger(TopicHelper.class);
    final Config config;
    final String token;
    final Properties adminConfig;

    public TopicHelper(URI bootstrapServers, Config config, String token) {
        this.config = config;
        this.token = token;

        adminConfig = token != null ?
            ClientsConfig.getAdminConfigOauth(config, token) :
            ClientsConfig.getAdminConfig(config);

        adminConfig.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
    }

    public void deleteAllTopics() {
        // Tests assume a clean slate - remove any existing topics
        try (Admin admin = Admin.create(adminConfig)) {
            admin.listTopics()
                .listings()
                .toCompletionStage()
                .thenApply(topics -> topics.stream().map(TopicListing::name).collect(Collectors.toList()))
                .thenComposeAsync(topicNames -> {
                    log.infof("Deleting topics: %s", topicNames);
                    return admin.deleteTopics(topicNames).all().toCompletionStage();
                })
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Process interruptted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }
    }

    private Map<String, ?> getHeaders() {
        return token != null ?
            Map.of(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token) :
            Collections.emptyMap();
    }

    public void createTopics(String clusterId, List<String> names, int numPartitions, Status expectedStatus) {
        names.forEach(name ->
            given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .headers(getHeaders())
                .body(buildNewTopicRequest(name, numPartitions, Map.of("min.insync.replicas", "1")).toString())
            .when()
                .post(TOPIC_COLLECTION_PATH, clusterId)
            .then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.getStatusCode()));
    }

    public void assertNoTopicsExist(String clusterId) {
        given()
            .log().ifValidationFails()
            .headers(getHeaders())
        .when()
            .get(TOPIC_COLLECTION_PATH, clusterId)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("page", equalTo(1))
            .body("size", equalTo(10))
            .body("total", equalTo(0))
            .body("items.size()", equalTo(0));
    }

    public static JsonObject buildNewTopicRequest(String name, int numPartitions, Map<String, String> config) {
        JsonArrayBuilder configBuilder = Json.createArrayBuilder();
        config.forEach((key, value) -> configBuilder.add(Json.createObjectBuilder().add(key, value)));

        return Json.createObjectBuilder()
            .add("name", name)
            .add("settings", Json.createObjectBuilder()
                 .add("numPartitions", numPartitions)
                 .add("configs", configBuilder))
            .build();
    }

    public static JsonObject buildUpdateTopicRequest(String name, int numPartitions, Map<String, String> config) {
        JsonArrayBuilder configBuilder = Json.createArrayBuilder();

        config.forEach((key, value) ->
            configBuilder.add(Json.createObjectBuilder()
                              .add("key", key)
                              .add("value", value)));

        return Json.createObjectBuilder()
            .add("name", name)
            .add("numPartitions", numPartitions)
            .add("config", configBuilder)
            .build();
    }
}
