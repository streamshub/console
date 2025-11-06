package com.github.streamshub.console.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
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

import org.apache.kafka.clients.CommonClientConfigs;
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
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.connector.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2Builder;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

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

    TestHelper utils;

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
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));

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

        utils.apply(client, new KafkaConnectorBuilder()
                .withNewMetadata()
                    .withNamespace("default")
                    .withName("test-connect1")
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .withNewStatus()
                .endStatus()
                .build());

        utils.apply(client, new KafkaMirrorMaker2Builder()
                .withNewMetadata()
                    .withNamespace("default")
                    .withName("test-connect2")
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .withNewStatus()
                    .withReplicas(4)
                    // connector1 will not be managed
                    .withConnectors(List.of(Map.of("name", "connect2-connector2")))
                .endStatus()
                .build());

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
            .body("data.meta.managed", contains(true, false, true, true))
            .body("data.attributes.name", contains(
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
            .body("included.size()", is(8)) // 2 clusters + 6 tasks
            .body("included.findAll { it.type == 'connects' }.size()", is(2))
            .body("included.findAll { it.type == 'connects' }.relationships.connectors.data", hasSize(2))
            .body("included.findAll { it.type == 'connects' }.relationships.connectors.data.type.flatten()", everyItem(is("connectors")))
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

    @ParameterizedTest
    @CsvSource({
        "'eq,source',       connect1-",
        "'in,source:mm,source:mm-checkpoint,source:mm-heartbeat', connect2-"
    })
    void testListConnectorsFilteredByMirrorMakerTypes(String filter, String connectorPrefix) {
        whenRequesting(req -> req
                .param("filter[type]", String.valueOf(filter))
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(2))
            .body("data.attributes.name", everyItem(startsWith(connectorPrefix)));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-connect1, connect1-connector1",
        "default/test-connect1, connect1-connector2",
        "default/test-connect2, connect2-connector1",
        "default/test-connect2, connect2-connector2"
    })
    void testDescribeConnector(String connectCluster, String connectorName) {
        var enc = Base64.getUrlEncoder().withoutPadding();
        String connectorID = enc.encodeToString(connectCluster.getBytes()) +
                ',' +
                enc.encodeToString(connectorName.getBytes());

        whenRequesting(req -> req.get(connectorID))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(connectorName));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-connect1, connect1-connector1, 2",
        "default/test-connect1, connect1-connector2, 1",
        "default/test-connect2, connect2-connector1, 1",
        "default/test-connect2, connect2-connector2, 2"
    })
    void testDescribeConnectorWithAllIncluded(String connectCluster, String connectorName, int taskCount) {
        var enc = Base64.getUrlEncoder().withoutPadding();
        String connectorID = enc.encodeToString(connectCluster.getBytes()) +
                ',' +
                enc.encodeToString(connectorName.getBytes());

        whenRequesting(req -> req
                .param("fields[connectors]", "name,namespace,config,offsets,topics,connectCluster,tasks")
                .param("fields[connects]", "name,namespace,connectors")
                .param("fields[connectorTasks]", "taskId,config,state,connector")
                .param("include", "connectCluster,tasks")
                .get(connectorID))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.relationships.connectCluster.data.type", is("connects"))
            .body("data.relationships.tasks.data.type.flatten()", everyItem(is("connectorTasks")))
            .body("included.size()", is(1 + taskCount))
            .body("included.findAll { it.type == 'connects' }.size()", is(1))
            .body("included.findAll { it.type == 'connectorTasks' }.size()", is(taskCount))
            .body("included.findAll { it.type == 'connectorTasks' }.relationships.connector.data.id.flatten()", everyItem(is(connectorID)));
    }
}
