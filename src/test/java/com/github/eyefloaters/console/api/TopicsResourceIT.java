package com.github.eyefloaters.console.api;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
@TestProfile(TestPlainProfile.class)
class TopicsResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TopicHelper topicUtils;
    String clusterId1;
    String clusterId2;

    @BeforeEach
    void setup() {
        URI bootstrapServers = URI.create(deployments.getExternalBootstrapServers());

        topicUtils = new TopicHelper(bootstrapServers, config, null);
        topicUtils.deleteAllTopics();

        clusterId1 = UUID.randomUUID().toString();
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
                            .withHost(bootstrapServers.getHost())
                            .withPort(bootstrapServers.getPort() + 1)
                        .endAddress()
                    .endListener()
                .endStatus()
                .build())
            .create();
    }

    @Test
    void testListTopicsAfterCreation() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(clusterId1, topicNames, 1);

        whenRequesting(req -> req.get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", is(topicNames.size()))
            .body("name", containsInAnyOrder(topicNames.toArray(String[]::new)));
    }

    @Test
    void testListTopicsWithKafkaUnavailable() {
        whenRequesting(req -> req.get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId2))
            .assertThat()
            .statusCode(is(Status.GATEWAY_TIMEOUT.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("504"))
            .body("errors.code", contains("5041"));
    }

    @Test
    void testListTopicsWithConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("include", "configs")
                .get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", is(1))
            .body("name", contains(topicName))
            .body("configs[0]", not(anEmptyMap()))
            .body("configs[0].findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testListTopicsWithAuthorizedOperationsIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("include", "authorizedOperations")
                .get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", is(1))
            .body("name", contains(topicName))
            .body("authorizedOperations", contains(nullValue()));
    }

    @Test
    void testListTopicsWithPartitionsIncludedAndLatestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("include", "partitions")
                .get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", is(1))
            .body("name", contains(topicName))
            .body("partitions[0]", hasSize(2))
            .body("partitions[0][0].offset.kind", is("Offset"))
            .body("partitions[0][0].offset.offset", is(2))
            .body("partitions[0][0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testListTopicsWithPartitionsIncludedAndEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("include", "partitions")
                .queryParam("offsetSpec", "earliest")
                .get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", is(1))
            .body("name", contains(topicName))
            .body("partitions[0]", hasSize(2))
            .body("partitions[0][0].offset.kind", is("Offset"))
            .body("partitions[0][0].offset.offset", is(0))
            .body("partitions[0][0].offset.timestamp", is(nullValue()));
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
                .queryParam("include", "partitions")
                .queryParam("offsetSpec", first.toString())
                .get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", is(1))
            .body("name", contains(topicName))
            .body("partitions[0]", hasSize(2))
            .body("partitions[0][0].offset.kind", is("Offset"))
            .body("partitions[0][0].offset.offset", is(0))
            .body("partitions[0][0].offset.timestamp", is(first.toString()));
    }

    @Test
    void testListTopicsWithPartitionsIncludedAndOffsetWithMaxTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("include", "partitions")
                .queryParam("offsetSpec", "maxTimestamp")
                .get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", is(1))
            .body("name", contains(topicName))
            .body("partitions[0]", hasSize(2))
            .body("partitions[0][0].offset.kind", is("Offset"))
            .body("partitions[0][0].offset.offset", is(1))
            .body("partitions[0][0].offset.timestamp", is(second.toString()));
    }


    @Test
    void testDescribeTopicWithConfigsIncluded() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req
                .queryParam("include", "configs")
                .get(TopicHelper.TOPIC_PATH, clusterId1, topicName))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("name", is(topicName))
            .body("configs", not(anEmptyMap()))
            .body("configs.findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testDescribeTopicWithAuthorizedOperationsNull() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 1);

        whenRequesting(req -> req.get(TopicHelper.TOPIC_PATH, clusterId1, topicName))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("name", is(topicName))
            .body("$", not(hasKey("configs")))
            .body("$", hasKey("authorizedOperations"))
            .body("authorizedOperations", is(nullValue()));
    }

    @Test
    void testDescribeTopicWithLatestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req.get(TopicHelper.TOPIC_PATH, clusterId1, topicName))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("name", is(topicName))
            .body("$", not(hasKey("configs")))
            .body("partitions", hasSize(2))
            .body("partitions[0].offset.kind", is("Offset"))
            .body("partitions[0].offset.offset", is(2))
            .body("partitions[0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testDescribeTopicWithEarliestOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k1", "v1");
        topicUtils.produceRecord(topicName, 0, null, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "earliest")
                .get(TopicHelper.TOPIC_PATH, clusterId1, topicName))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("name", is(topicName))
            .body("$", not(hasKey("configs")))
            .body("partitions", hasSize(2))
            .body("partitions[0].offset.kind", is("Offset"))
            .body("partitions[0].offset.offset", is(0))
            .body("partitions[0].offset.timestamp", is(nullValue()));
    }

    @Test
    void testDescribeTopicWithTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", first.toString())
                .get(TopicHelper.TOPIC_PATH, clusterId1, topicName))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("name", is(topicName))
            .body("$", not(hasKey("configs")))
            .body("partitions", hasSize(2))
            .body("partitions[0].offset.kind", is("Offset"))
            .body("partitions[0].offset.offset", is(0))
            .body("partitions[0].offset.timestamp", is(first.toString()));
    }

    @Test
    void testDescribeTopicWithMaxTimestampOffset() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, first, Collections.emptyMap(), "k1", "v1");
        Instant second = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        topicUtils.produceRecord(topicName, 0, second, Collections.emptyMap(), "k2", "v2");

        whenRequesting(req -> req
                .queryParam("offsetSpec", "maxTimestamp")
                .get(TopicHelper.TOPIC_PATH, clusterId1, topicName))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("name", is(topicName))
            .body("$", not(hasKey("configs")))
            .body("partitions", hasSize(2))
            .body("partitions[0].offset.kind", is("Offset"))
            .body("partitions[0].offset.offset", is(1))
            .body("partitions[0].offset.timestamp", is(second.toString()));
    }

    @Test
    void testDescribeTopicWithBadOffsetTimestamp() {
        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(clusterId1, List.of(topicName), 2);

        whenRequesting(req -> req
                .queryParam("offsetSpec", "Invalid Timestamp")
                .get(TopicHelper.TOPIC_PATH, clusterId1, topicName))
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
        whenRequesting(req -> req.get(TopicHelper.TOPIC_PATH, clusterId1, UUID.randomUUID().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

    @Test
    void testDescribeTopicConfigsWithNoSuchTopic() {
        whenRequesting(req -> req.get(TopicHelper.TOPIC_PATH + "/configs", clusterId1, UUID.randomUUID().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

}
