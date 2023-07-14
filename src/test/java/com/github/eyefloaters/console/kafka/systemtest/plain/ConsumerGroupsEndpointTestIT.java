package com.github.eyefloaters.console.kafka.systemtest.plain;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.utils.ConsumerUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(TestPlainProfile.class)
class ConsumerGroupsEndpointTestIT {

    static final String CONSUMER_GROUP_COLLECTION_PATH = "/rest/consumer-groups";
    static final String CONSUMER_GROUP_PATH = "/rest/consumer-groups/{groupId}";

    @Inject
    Config config;

    ConsumerUtils groupUtils;

    @BeforeEach
    void setup() {
        groupUtils = new ConsumerUtils(config, null);
    }

    @Test
    void testListConsumerGroups() throws Exception {
        String topicName = "t-" + UUID.randomUUID().toString();
        String groupId = "g-" + UUID.randomUUID().toString();
        String clientId = "c-" + UUID.randomUUID().toString();

        groupUtils.consume(groupId, topicName, clientId, 2, true);
        String groupPath = String.format("items.find { it.groupId == '%s' }", groupId);

        given()
            .log().ifValidationFails()
            .queryParam("group-id-filter", groupId)
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("items.size()", equalTo(1))
            .body(groupPath + ".state", equalToIgnoringCase("empty")) // Consumer is closed
            .body(groupPath + ".consumers", hasSize(2))
            .body(groupPath + ".consumers.findAll { it }.groupId", contains(groupId, groupId))
            .body(groupPath + ".consumers.findAll { it }.topic", contains(topicName, topicName))
            .body(groupPath + ".consumers.findAll { it }.partition", contains(0, 1))
            .body(groupPath + ".consumers.findAll { it }.memberId", contains(null, null)); // Consumer is closed
    }

//    @Test
//    void testEmptyTopicsOnList(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws Exception {
//        SyncMessaging.createConsumerGroups(kafkaClient, 4, deployments.getExternalBootstrapServers(), testContext);
//        String topic = UUID.randomUUID().toString();
//        kafkaClient.createTopics(Collections.singletonList(new NewTopic(topic, 3, (short) 1)));
//        DynamicWait.waitForTopicsExists(Collections.singletonList(topic), kafkaClient);
//
//        Properties props = ClientsConfig.getConsumerConfig(deployments.getExternalBootstrapServers(), "test-group");
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, props);
//
//        AsyncMessaging.produceMessages(vertx, deployments.getExternalBootstrapServers(), topic, 30, null);
//        consumer.subscribe(topic);
//        AtomicReference<KafkaConsumerRecords<String, String>> records = new AtomicReference<>();
//        CountDownLatch cd = new CountDownLatch(1);
//        consumer.poll(Duration.ofSeconds(60), result -> {
//            if (!result.result().isEmpty()) {
//                cd.countDown();
//                records.set(result.result());
//            }
//        });
//        assertThat(cd.await(80, TimeUnit.SECONDS)).isTrue();
//        consumer.close();
//
//        HttpClient client = createHttpClient(vertx);
//        CircuitBreaker breaker = CircuitBreaker.create("rebalance-waiter", vertx, new CircuitBreakerOptions()
//                .setTimeout(2000).setResetTimeout(3000).setMaxRetries(60)).retryPolicy(retryCount -> retryCount * 1000L);
//        AtomicReference<Types.ConsumerGroupList> lastResp = new AtomicReference<>();
//        breaker.executeWithFallback(promise -> {
//            client.request(HttpMethod.GET, publishedAdminPort, "localhost", "/rest/consumer-groups")
//                    .compose(req -> req.send().compose(HttpClientResponse::body))
//                    .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
//                        Types.ConsumerGroupList response = MODEL_DESERIALIZER.deserializeResponse(buffer, Types.ConsumerGroupList.class);
//                        Types.ConsumerGroupDescription g = response.getItems().stream().filter(i -> i.getGroupId().equals("test-group")).collect(Collectors.toList()).stream().findFirst().get();
//                        if (g.getConsumers().size() != 0) {
//                            lastResp.set(response);
//                            promise.complete();
//                        }
//                    })));
//        }, t -> null);
//
//        try {
//            await().atMost(1, TimeUnit.MINUTES).untilAtomic(lastResp, is(notNullValue()));
//        } catch (Exception e) {
//            testContext.failNow("Test wait for rebalance");
//        }
//
//        AtomicInteger parts = new AtomicInteger(0);
//
//        testContext.verify(() -> {
//            lastResp.get().getItems().forEach(item -> {
//                if (item.getGroupId().equals("test-group")) {
//                    for (Types.Consumer c : item.getConsumers()) {
//                        parts.getAndIncrement();
//                        assertThat(c.getMemberId()).isNull();
//                        int actOffset = records.get().records().records(new TopicPartition(topic, c.getPartition())).size();
//                        assertThat(c.getOffset()).isEqualTo(actOffset);
//                        assertThat(c.getLag()).isNotNegative();
//                        assertThat(c.getLag()).isEqualTo(c.getLogEndOffset() - c.getOffset());
//                    }
//                } else {
//                    item.getConsumers().forEach(c -> assertThat(c.getMemberId()).isNull());
//                }
//            });
//            assertThat(parts.get()).isEqualTo(3);
//            List<String> responseGroupIDs = lastResp.get().getItems().stream().map(Types.ConsumerGroup::getGroupId).collect(Collectors.toList());
//            List<String> consumerGroups = kafkaClient.listConsumerGroups().all().get().stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());
//            assertThat(consumerGroups).hasSameElementsAs(responseGroupIDs);
//        });
//
//        testContext.completeNow();
//
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }
//
    @ParameterizedTest
    @ValueSource(strings = { "asc", "desc" })
    void testListConsumerGroupsWithSort(String order) throws Exception {
        int groupCount = 5;
        String prefix = UUID.randomUUID().toString();

        Comparator<String> comparator = Comparator.naturalOrder();

        if ("desc".equals(order)) {
            comparator = comparator.reversed();
        }

        List<String> groupIds = IntStream.range(0, groupCount)
            .mapToObj(i -> String.format("-%03d-%s", i, UUID.randomUUID().toString()))
            .map(suffix -> {
                String groupId = prefix + "-g-" + suffix;
                String topicName = "t-" + suffix;
                String clientId = "c-" + suffix;
                groupUtils.consume(groupId, topicName, clientId, 2, true);
                return groupId;
            })
            .sorted(comparator)
            .collect(Collectors.toList());

        given()
            .queryParam("group-id-filter", prefix + "-g-")
            .queryParam("orderKey", "name")
            .queryParam("order", order)
        .when()
            .get(CONSUMER_GROUP_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
            .assertThat()
            .body("items.size()", equalTo(groupCount))
            .body("items.findAll { it }.groupId", contains(groupIds.toArray(String[]::new)));
    }

//    @Test
//    void testListConsumerGroupKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws Exception {
//        HttpClient client = createHttpClient(vertx);
//        deployments.stopKafkaContainer();
//        client.request(HttpMethod.GET, publishedAdminPort, "localhost", "/rest/consumer-groups")
//                .compose(req -> req.send().onComplete(l -> testContext.verify(() -> {
//                    assertThat(testContext.failed()).isFalse();
//                    if (l.succeeded()) {
//                        assertThat(l.result().statusCode()).isEqualTo(ReturnCodes.KAFKA_DOWN.code);
//                    }
//                    testContext.completeNow();
//                })).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }
//
    @Test
    void testDeleteConsumerGroup() throws Exception {
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

        when()
            .delete(CONSUMER_GROUP_PATH, groupIds.get(0))
        .then()
            .log().ifValidationFails()
            .statusCode(Status.NO_CONTENT.getStatusCode());

        given()
            .queryParam("group-id-filter", prefix + "-g-")
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
    void testDeleteActiveConsumerGroup() {
        String topicName = "t-" + UUID.randomUUID().toString();
        String groupId = "g-" + UUID.randomUUID().toString();
        String clientId = "c-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(groupId, topicName, clientId, 2, false)) {
            when()
                .delete(CONSUMER_GROUP_PATH, groupId)
            .then()
                .log().ifValidationFails()
                .statusCode(423);

            when()
                .get(CONSUMER_GROUP_PATH, groupId)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("groupId", equalTo(groupId));
        }
    }
//
//    @Test
//    void testDeleteConsumerGroupKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws Exception {
//        List<String> groupdIds = SyncMessaging.createConsumerGroups(kafkaClient, 2, deployments.getExternalBootstrapServers(), testContext);
//
//        HttpClient client = createHttpClient(vertx);
//        deployments.stopKafkaContainer();
//        client.request(HttpMethod.DELETE, publishedAdminPort, "localhost", "/rest/consumer-groups/" + groupdIds.get(0))
//                .compose(req -> req.send().onComplete(l -> testContext.verify(() -> {
//                    assertThat(testContext.failed()).isFalse();
//                    if (l.succeeded()) {
//                        assertThat(l.result().statusCode()).isEqualTo(ReturnCodes.KAFKA_DOWN.code);
//                    }
//                    testContext.completeNow();
//                })).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }
//
    @Test
    void testDescribeConsumerGroup() throws Exception {
        String topicName = "t-" + UUID.randomUUID().toString();
        String groupId = "g-" + UUID.randomUUID().toString();
        String clientId = "c-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(groupId, topicName, clientId, 2, false)) {
            when()
                .get(CONSUMER_GROUP_PATH, groupId)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
            .assertThat()
                .body("groupId", equalTo(groupId))
                .body("state", equalToIgnoringCase("stable")) // Consumer is still open
                .body("consumers", hasSize(2))
                .body("metrics.laggingPartitions", equalTo(0))
                .body("metrics.activeConsumers", equalTo(1))
                .body("metrics.unassignedPartitions", equalTo(0))
                .body("consumers.findAll { it }.groupId", contains(groupId, groupId))
                .body("consumers.findAll { it }.topic", contains(topicName, topicName))
                .body("consumers.findAll { it }.partition", contains(0, 1))
                .body("consumers.findAll { it }.memberId", contains(startsWith(clientId), startsWith(clientId)));
        }
    }
//
//    @Test
//    void testDescribeConsumerGroupKafkaDown(Vertx vertx, VertxTestContext testContext, ExtensionContext extensionContext) throws Exception {
//        List<String> groupdIds = SyncMessaging.createConsumerGroups(kafkaClient, 2, deployments.getExternalBootstrapServers(), testContext);
//
//        HttpClient client = createHttpClient(vertx);
//        deployments.stopKafkaContainer();
//        client.request(HttpMethod.GET, publishedAdminPort, "localhost", "/rest/consumer-groups/" + groupdIds.get(0))
//                .compose(req -> req.send().onComplete(l -> testContext.verify(() -> {
//                    assertThat(testContext.failed()).isFalse();
//                    if (l.succeeded()) {
//                        assertThat(l.result().statusCode()).isEqualTo(ReturnCodes.KAFKA_DOWN.code);
//                    }
//                    testContext.completeNow();
//                })).onFailure(testContext::failNow));
//        assertThat(testContext.awaitCompletion(1, TimeUnit.MINUTES)).isTrue();
//    }
//
    @Test
    void testDescribeNonExistingConsumerGroup() throws Exception {
        when()
            .get(CONSUMER_GROUP_PATH, UUID.randomUUID().toString())
        .then()
            .log().ifValidationFails()
            .statusCode(Status.NOT_FOUND.getStatusCode())
        .assertThat()
            .body("code", equalTo(Status.NOT_FOUND.getStatusCode()));
    }

    @ParameterizedTest
    @CsvSource({
        "-1,      p1-w,   1",
        "topic,   GrOuP,  4",
        "opic,    RoUp,   4",
        "opic,    '',     4",
        "opic,    ,       4",
        "topic-1, z,      0",
        ",        grouP2, 2",
        "'',      grouP2, 2"
    })
    void testConsumerGroupsListFilteredWithoutAuthentication(String topicFilter, String groupFilter, int expectedCount) throws Exception {
        String batchId = UUID.randomUUID().toString();
        groupUtils.consume("group1-W-bat" + batchId, "topic-1-A-bat" + batchId, UUID.randomUUID().toString(), 1, false);
        groupUtils.consume("group1-X-bat" + batchId, "topic-1-B-bat" + batchId, UUID.randomUUID().toString(), 1, false);
        groupUtils.consume("group2-Y-bat" + batchId, "topic-2-C-bat" + batchId, UUID.randomUUID().toString(), 1, false);
        groupUtils.consume("group2-Z-bat" + batchId, "topic-2-D-bat" + batchId, UUID.randomUUID().toString(), 1, false);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");
        queryParams.put("size", "100");

        if (topicFilter != null) {
            queryParams.put("topic",  topicFilter);
        }

        if (groupFilter != null) {
            queryParams.put("group-id-filter", groupFilter);
        }

        var response =
            given()
                .queryParams(queryParams)
            .when()
                .get(CONSUMER_GROUP_COLLECTION_PATH)
            .then()
                .log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
                .body("items.size()", greaterThanOrEqualTo(expectedCount));

        List<JsonObject> currentConsumers;

        try (var stream = response.extract().body().asInputStream(); var reader = Json.createReader(stream)) {
            currentConsumers = reader.readObject()
                .getJsonArray("items")
                .stream()
                .filter(value -> value.getValueType() == ValueType.OBJECT)
                .map(JsonValue::asJsonObject)
                // Filter out any consumer groups from other tests
                .filter(object -> object.getString("groupId", "").contains("bat" + batchId))
                .map(object -> object.getJsonArray("consumers"))
                .flatMap(List::stream)
                .filter(value -> value.getValueType() == ValueType.OBJECT)
                .map(JsonValue::asJsonObject)
                .collect(Collectors.toList());
        }

        assertEquals(expectedCount, currentConsumers.size());

        currentConsumers.forEach(consumer -> {
            if (notBlank(topicFilter)) {
                assertTrue(consumer.getString("topic").toLowerCase(Locale.ROOT).contains(topicFilter.toLowerCase(Locale.ROOT)));
            }
            if (notBlank(groupFilter)) {
                assertTrue(consumer.getString("groupId").toLowerCase(Locale.ROOT).contains(groupFilter.toLowerCase(Locale.ROOT)));
            }
        });
    }

    boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

}
