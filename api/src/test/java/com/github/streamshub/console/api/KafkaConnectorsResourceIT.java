package com.github.streamshub.console.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.Config;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.console.api.support.KafkaConnectAPI;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.test.container.StrimziKafkaContainer;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(KafkaConnectorsResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaConnectorsResourceIT implements ClientRequestFilter {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    KafkaConnectAPI.Client connectClient;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;

    StrimziKafkaContainer kafkaContainer;
    URI bootstrapServers;

    Consumer<ClientRequestContext> filterRequest;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        filterRequest.accept(requestContext);
    }

    static ObjectNode fixtures;

    @BeforeAll
    static void initialize() throws IOException {
        try (InputStream in = KafkaConnectorsResourceIT.class.getResourceAsStream("KafkaConnect-fixtures.yaml")) {
            fixtures = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(in);
        }
    }

    @BeforeEach
    void setup() {
        filterRequest = ctx -> {
            var uri = ctx.getUri();
            String host = uri.getHost();
            String path = uri.getPath();
            var fixture = fixtures.get(host).get(path);
            Objects.requireNonNull(fixture, "Kafka Connect fixture not found for host " + host + "; path " + path);

            ctx.abortWith(Response.ok(fixture)
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        };
        connectClient.setAdditionalFilter(Optional.of(this));
        kafkaContainer = deployments.getKafkaContainer();
        bootstrapServers = URI.create(kafkaContainer.getBootstrapServers());

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);

        client.resources(Kafka.class).inAnyNamespace().delete();

        Kafka kafka1 = new KafkaBuilder(utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers))
            .editOrNewStatus()
                .addNewCondition()
                    .withType("Ready")
                    .withStatus("True")
                .endCondition()
                .addNewKafkaNodePool()
                    .withName("my-node-pool")
                .endKafkaNodePool()
            .endStatus()
            .build();

        utils.apply(client, kafka1);

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka1)::equals));
    }

    @AfterEach
    void teardown() {
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    @Test
    void testListConnectorsSortedByNameDesc() {
        whenRequesting(req -> req.param("sort", "-name").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(4))
            .body("data.attributes.name", Matchers.contains(
                "connect2-connector2",
                "connect2-connector1",
                "connect1-connector2",
                "connect1-connector1"
            ));
    }

    @Test
    void testListConnectorsWithAllIncluded() {
        whenRequesting(req -> req
                .param("fields[connectors]", "name,namespace,config,offsets,topics,connectCluster,tasks")
                .param("fields[connects]", "name,namespace,connectors")
                .param("fields[connectorTasks]", "taskId,config,state,connector")
                .param("include", "connectCluster,tasks")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(4))
            .body("included.size()", is(8)) // 2 clusters + 3 tasks each
            .body("included.findAll { it.type == 'connects' }.size()", is(2))
            .body("included.findAll { it.type == 'connects' }.relationships.connectors.data", hasSize(2))
            .body("included.findAll { it.type == 'connects' }.relationships.connectors.data.type.flatten()", everyItem(is("connects")))
            .body("included.findAll { it.type == 'connectorTasks' }.size()", is(6))
            .body("included.findAll { it.type == 'connectorTasks' }.relationships.connector.data.type.flatten()", everyItem(is("connectors")));
    }

    @ParameterizedTest
    @CsvSource({
        "'like,connect1-*', 'connect1-connector1,connect1-connector2'",
        "'like,connect2-*', 'connect2-connector1,connect2-connector2'",
        "'like,*-connector1', 'connect1-connector1,connect2-connector1'",
        "'like,*-connector2', 'connect1-connector2,connect2-connector2'",
        "'in,connect1-connector1,connect2-connector2', 'connect1-connector1,connect2-connector2'"
    })
    void testListConnectorsFilteredByName(String filter, String expected) {
        String[] expectedNames = expected.split(",");

        whenRequesting(req -> req
                .param("filter[name]", filter)
                .param("sort", "name")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", Matchers.contains(expectedNames));
    }

}
