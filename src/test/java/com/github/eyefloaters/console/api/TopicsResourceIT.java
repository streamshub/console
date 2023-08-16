package com.github.eyefloaters.console.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.common.Uuid;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.test.TestHelper;
import com.github.eyefloaters.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
@TestHTTPEndpoint(TopicsResource.class)
@TestProfile(TestPlainProfile.class)
class TopicsResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;
    TopicHelper topicUtils;
    String clusterId1;
    String clusterId2;
    ServerSocket randomSocket;

    @BeforeEach
    void setup() throws IOException {
        URI bootstrapServers = URI.create(deployments.getExternalBootstrapServers());
        randomSocket = new ServerSocket(0);
        URI randomBootstrapServers = URI.create("dummy://localhost:" + randomSocket.getLocalPort());

        topicUtils = new TopicHelper(bootstrapServers, config, null);
        topicUtils.deleteAllTopics();

        utils = new TestHelper(bootstrapServers, config, null);

        clusterId1 = utils.getClusterId();
        clusterId2 = UUID.randomUUID().toString();

        client.resources(Kafka.class).delete();
        client.resources(Kafka.class).resource(new KafkaBuilder()
                .withNewMetadata()
                    .withName("test-kafka1")
                .endMetadata()
                .withNewSpec()
                    .withNewKafka()
                        .addNewListener()
                            .withName("listener0")
                            .withType(KafkaListenerType.NODEPORT)
                        .endListener()
                    .endKafka()
                .endSpec()
                .withNewStatus()
                    .withClusterId(clusterId1)
                    .addNewListener()
                        .withName("listener0")
                        .addNewAddress()
                            .withHost(bootstrapServers.getHost())
                            .withPort(bootstrapServers.getPort())
                        .endAddress()
                    .endListener()
                .endStatus()
                .build())
            .create();

        // Second cluster is offline/non-existent
        client.resources(Kafka.class).resource(new KafkaBuilder()
                .withNewMetadata()
                    .withName("test-kafka2")
                .endMetadata()
                .withNewSpec()
                    .withNewKafka()
                        .addNewListener()
                            .withName("listener0")
                            .withType(KafkaListenerType.NODEPORT)
                        .endListener()
                    .endKafka()
                .endSpec()
                .withNewStatus()
                    .withClusterId(clusterId2)
                    .addNewListener()
                        .withName("listener0")
                        .addNewAddress()
                            .withHost(randomBootstrapServers.getHost())
                            .withPort(randomBootstrapServers.getPort())
                        .endAddress()
                    .endListener()
                .endStatus()
                .build())
            .create();
    }

    @AfterEach
    void teardown() throws IOException {
        if (randomSocket != null) {
            randomSocket.close();
        }
    }

    @Test
    void testListTopicsAfterCreation() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(clusterId1, topicNames, 1);

        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(topicNames.size()))
            .body("data.attributes.name", containsInAnyOrder(topicNames.toArray(String[]::new)));
    }

    @Test
    void testListTopicsWithKafkaUnavailable() {
        whenRequesting(req -> req.get("", clusterId2))
            .assertThat()
            .statusCode(is(Status.GATEWAY_TIMEOUT.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("504"))
            .body("errors.code", contains("5041"));
    }

    @Test
    void testListTopicsWithNameAndConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "configs,name")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.configs[0]", not(anEmptyMap()))
            .body("data.attributes.configs[0].findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testListTopicsWithNameAndAuthorizedOperationsIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "authorizedOperations,name")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.authorizedOperations", contains(iterableWithSize(greaterThan(0))));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndLatestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0][0].offset.offset", is(2))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .queryParam("offsetSpec", "earliest")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0][0].offset.offset", is(0))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testListTopicsWithPartitionsIncludedAndOffsetWithTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .queryParam("offsetSpec", first.toString())
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0][0].offset.offset", is(0))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(first.toString()));
    }

    @Test
    void testListTopicsWithNameAndPartitionsIncludedAndOffsetWithMaxTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,partitions")
                .queryParam("offsetSpec", "maxTimestamp")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes", contains(aMapWithSize(2)))
            .body("data.attributes.name", contains(topicName))
            .body("data.attributes.partitions[0]", hasSize(2))
            .body("data.attributes.partitions[0][0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0][0].offset.offset", is(1))
            .body("data.attributes.partitions[0][0].offset.timestamp", is(second.toString()));
    }


    @Test
    void testDescribeTopicWithNameAndConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("fields[topics]", "name,configs")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes", is(aMapWithSize(2)))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes.configs", not(anEmptyMap()))
            .body("data.attributes.configs.findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testDescribeTopicWithAuthorizedOperations() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req.get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.authorizedOperations", is(notNullValue()))
            .body("data.attributes.authorizedOperations", hasSize(greaterThan(0)));
    }

    @Test
    void testDescribeTopicWithLatestOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req.get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0].offset.offset", is(2))
            .body("data.attributes.partitions[0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testDescribeTopicWithEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "earliest")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0].offset.offset", is(0))
            .body("data.attributes.partitions[0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testDescribeTopicWithTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", first.toString())
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0].offset.offset", is(0))
            .body("data.attributes.partitions[0].offset.timestamp", is(first.toString()));
    }

    @Test
    void testDescribeTopicWithMaxTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "maxTimestamp")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(topicName))
            .body("data.attributes", not(hasKey("configs")))
            .body("data.attributes.partitions", hasSize(2))
            .body("data.attributes.partitions[0].offset.type", is(not("error")))
            .body("data.attributes.partitions[0].offset.offset", is(1))
            .body("data.attributes.partitions[0].offset.timestamp", is(second.toString()));
    }

    @Test
    void testDescribeTopicWithBadOffsetTimestamp() {
        String topicName = UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        whenRequesting(req -> req
                .queryParam("offsetSpec", "Invalid Timestamp")
                .get("{topicName}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.title", contains("Invalid query parameter"))
            .body("errors.source.parameter", contains("offsetSpec"));
    }

    @Test
    void testDescribeTopicWithNoSuchTopic() {
        whenRequesting(req -> req.get("{topicName}", clusterId1, Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

}
