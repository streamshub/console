package com.github.eyefloaters.console.api;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.test.container.StrimziKafkaContainer;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
@TestHTTPEndpoint(ClustersResource.class)
@TestProfile(TestPlainProfile.class)
class ClustersResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

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
        clusterId = utils.getClusterId();

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
                            .withNewKafkaListenerAuthenticationCustomAuth()
                                .withSasl()
                            .endKafkaListenerAuthenticationCustomAuth()
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
    void testListClusters() {
        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("size()", equalTo(1))
            .body("name", contains("test-kafka1"))
            .body("clusterId", contains(clusterId))
            .body("bootstrapServers", contains(bootstrapServers.getHost() + ":" + bootstrapServers.getPort()))
            .body("authType", contains("custom"));
    }

    @Test
    void testDescribeCluster() {
        whenRequesting(req -> req.get("{clusterId}", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("name", equalTo("test-kafka1"))
            .body("clusterId", equalTo(clusterId))
            .body("bootstrapServers", equalTo(bootstrapServers.getHost() + ":" + bootstrapServers.getPort()))
            .body("authType", equalTo("custom"));
    }
}
