package com.github.eyefloaters.console.api;

import java.net.URI;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
    String clusterId;

    @BeforeEach
    void setup() {
        URI bootstrapServers = URI.create(deployments.getExternalBootstrapServers());

        topicUtils = new TopicHelper(bootstrapServers, config, null);
        topicUtils.deleteAllTopics();

        clusterId = UUID.randomUUID().toString();

        client.resources(Kafka.class).delete();
        client.resources(Kafka.class).resource(new KafkaBuilder()
                .withNewMetadata()
                    .withName("test-kafka")
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
                    .withClusterId(clusterId)
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
    }

    @Test
    void testTopicListAfterCreation() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(clusterId, topicNames, 1, Status.CREATED);

        whenRequesting(req -> req.get(TopicHelper.TOPIC_COLLECTION_PATH, clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", equalTo(topicNames.size()))
            .body("name", containsInAnyOrder(topicNames.toArray(String[]::new)));
    }
}
