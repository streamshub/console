package com.github.streamshub.console.api;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.test.container.StrimziKafkaContainer;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestHTTPEndpoint(NodesResource.class)
@TestProfile(TestPlainProfile.class)
class NodesResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;

    StrimziKafkaContainer kafkaContainer;
    String clusterId;
    URI bootstrapServers;

    @BeforeEach
    void setup() {
        kafkaContainer = deployments.getKafkaContainer();
        bootstrapServers = URI.create(kafkaContainer.getBootstrapServers());
        utils = new TestHelper(bootstrapServers, config, null);

        client.resources(Kafka.class).inAnyNamespace().delete();
        consoleConfig.clearSecurity();

        utils.apply(client, new KafkaBuilder()
                .withNewMetadata()
                    .withName("test-kafka1")
                    .withNamespace("default")
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
                    .withClusterId(utils.getClusterId())
                    .addNewListener()
                        .withName("listener0")
                        .addNewAddress()
                            .withHost(bootstrapServers.getHost())
                            .withPort(bootstrapServers.getPort())
                        .endAddress()
                    .endListener()
                .endStatus()
                .build());

        clusterId = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
    }

    @Test
    void testDescribeConfigs() {
        whenRequesting(req -> req.get("{nodeId}/configs", clusterId, "0"))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", not(anEmptyMap()))
            .body("data.attributes.findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testDescribeConfigsNodeNotFound() {
        whenRequesting(req -> req.get("{nodeId}/configs", clusterId, "99"))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", equalTo(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"))
            .body("errors.detail", contains("No such node: 99"));
    }
}
