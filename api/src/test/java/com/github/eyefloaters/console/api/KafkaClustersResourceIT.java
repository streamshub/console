package com.github.eyefloaters.console.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.UUID;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationCustomBuilder;
import io.strimzi.test.container.StrimziKafkaContainer;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
@TestHTTPEndpoint(KafkaClustersResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaClustersResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;

    StrimziKafkaContainer kafkaContainer;
    String clusterId1;
    String clusterId2;
    URI bootstrapServers;
    ServerSocket randomSocket;
    URI randomBootstrapServers;

    @BeforeEach
    void setup() throws IOException {
        kafkaContainer = deployments.getKafkaContainer();
        bootstrapServers = URI.create(kafkaContainer.getBootstrapServers());
        randomSocket = new ServerSocket(0);
        randomBootstrapServers = URI.create("dummy://localhost:" + randomSocket.getLocalPort());

        utils = new TestHelper(bootstrapServers, config, null);

        clusterId1 = utils.getClusterId();
        clusterId2 = UUID.randomUUID().toString();

        client.resources(Kafka.class).delete();
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka1", clusterId1, bootstrapServers,
                new KafkaListenerAuthenticationCustomBuilder()
                    .withSasl()
                .build()))
            .create();
        // Second cluster is offline/non-existent
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka2", clusterId2, randomBootstrapServers))
            .create();
    }

    @AfterEach
    void teardown() throws IOException {
        if (randomSocket != null) {
            randomSocket.close();
        }
    }

    @Test
    void testListClusters() {
        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(2))
            .body("data.id", containsInAnyOrder(clusterId1, clusterId2))
            .body("data.attributes.name", containsInAnyOrder("test-kafka1", "test-kafka2"))
            .body("data.attributes.bootstrapServers", containsInAnyOrder(
                    bootstrapServers.getHost() + ":" + bootstrapServers.getPort(),
                    randomBootstrapServers.getHost() + ":" + randomBootstrapServers.getPort()))
            .body("data.attributes.authType", containsInAnyOrder(equalTo("custom"), nullValue()));
    }

    @Test
    void testListClustersWithInformerError() {
        SharedIndexInformer<Kafka> informer = Mockito.mock();

        var informerType = new TypeLiteral<SharedIndexInformer<Kafka>>() {
            private static final long serialVersionUID = 1L;
        };

        @SuppressWarnings("all")
        class NamedLiteral extends AnnotationLiteral<jakarta.inject.Named> implements jakarta.inject.Named {
            private static final long serialVersionUID = 1L;

            @Override
            public String value() {
                return "KafkaInformer";
            }
        }

        // Force an unhandled exception
        Mockito.when(informer.getStore()).thenThrow(new RuntimeException("EXPECTED TEST EXCEPTION") {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        });

        QuarkusMock.installMockForType(informer, informerType, new NamedLiteral());

        whenRequesting(req -> req.get("{clusterId}", UUID.randomUUID().toString()))
            .assertThat()
            .statusCode(is(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("500"))
            .body("errors.code", contains("5001"));
    }

    @ParameterizedTest
    @CsvSource({
        "'name' , 'test-kafka1,test-kafka2'",
        "'-name', 'test-kafka2,test-kafka1'"
    })
    void testListClustersSortedByName(String sortParam, String expectedNameList) {
        whenRequesting(req -> req.queryParam("sort", sortParam).get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(2))
            .body("data.attributes.name", contains(expectedNameList.split(",")));
    }

    @Test
    void testDescribeCluster() {
        whenRequesting(req -> req.get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", equalTo(clusterId1))
            .body("data.attributes.name", equalTo("test-kafka1"))
            .body("data.attributes.bootstrapServers", equalTo(bootstrapServers.getHost() + ":" + bootstrapServers.getPort()))
            .body("data.attributes.authType", equalTo("custom"));
    }

    @Test
    void testDescribeClusterWithKafkaUnavailable() {
        whenRequesting(req -> req.get("{clusterId}", clusterId2))
            .assertThat()
            .statusCode(is(Status.GATEWAY_TIMEOUT.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("504"))
            .body("errors.code", contains("5041"));
    }

    @Test
    void testDescribeClusterWithNoSuchCluster() {
        whenRequesting(req -> req.get("{clusterId}", UUID.randomUUID().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }
}
