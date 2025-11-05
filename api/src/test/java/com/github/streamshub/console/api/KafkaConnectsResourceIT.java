package com.github.streamshub.console.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
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
import io.strimzi.api.kafka.model.connect.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2Builder;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestHTTPEndpoint(KafkaConnectsResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaConnectsResourceIT implements ClientRequestFilter {

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
        try (InputStream in = KafkaConnectsResourceIT.class.getResourceAsStream("KafkaConnect-fixtures.yaml")) {
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

        List<Kafka> kafkaClusters = Stream.of(1, 2).map(kid -> {
            Kafka k = new KafkaBuilder(utils.buildKafkaResource("test-kafka" + kid, utils.getClusterId(), bootstrapServers))
                    .editOrNewStatus()
                        .addNewCondition()
                            .withType("Ready")
                            .withStatus("True")
                        .endCondition()
                        .addNewKafkaNodePool()
                            .withName("my-node-pool" + kid)
                        .endKafkaNodePool()
                    .endStatus()
                    .build();

            return utils.apply(client, k);
        }).toList();

        utils.apply(client, new KafkaConnectBuilder()
                .withNewMetadata()
                    .withNamespace("default")
                    .withName("test-connect1")
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .withNewStatus()
                    .withReplicas(2)
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
                .endStatus()
                .build());

        // Wait for the added clusters to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> kafkaClusters.stream()
                    .allMatch(k -> configuredContexts.values()
                        .stream()
                        .map(KafkaContext::clusterConfig)
                        .map(KafkaClusterConfig::clusterKey)
                        .anyMatch(Cache.metaNamespaceKeyFunc(k)::equals)));
    }

    @AfterEach
    void teardown() {
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    String kafkaClusterId(String clusterKey) {
        return configuredContexts.values()
                .stream()
                .filter(ctx -> ctx.clusterConfig().clusterKey().equals(clusterKey))
                .map(ctx -> ctx.clusterId())
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(clusterKey));
    }

    @Test
    void testListConnectClustersSortedByVersion() {
        var enc = Base64.getUrlEncoder().withoutPadding();
        whenRequesting(req -> req.param("sort", "version").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(3))
            .body("data.meta.managed", contains(true, true, false))
            .body("data[0].id", is(enc.encodeToString("default/test-connect1".getBytes())))
            .body("data[0].attributes.commit", is("abc123d"))
            .body("data[0].attributes.kafkaClusterId", is(consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId()))
            .body("data[0].attributes.version", is("4.0.0"))
            .body("data[0].attributes.replicas", is(2))
            .body("data[1].id", is(enc.encodeToString("default/test-connect2".getBytes())))
            .body("data[1].attributes.commit", is("zyx987w"))
            .body("data[1].attributes.kafkaClusterId", is("k2-id"))
            .body("data[1].attributes.version", is("4.0.1"))
            .body("data[1].attributes.replicas", is(4))
            .body("data[2].id", is(enc.encodeToString("test-connect3".getBytes())))
            .body("data[2].attributes.commit", is("yyy777x"))
            .body("data[2].attributes.kafkaClusterId", is("test-kafkaY"))
            .body("data[2].attributes.version", is("4.0.2"))
            .body("data[2].attributes.replicas", is(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-kafka1, test-connect1, abc123d, k1-id, 4.0.0",
        "default/test-kafka2, test-connect2, zyx987w, k2-id, 4.0.1",
    })
    void testListConnectClustersFilteredByKafkaCluster(String kafkaCluster,
            String expectedName, String expectedCommit, String expectedKafkaId, String expectedVersion) {

        whenRequesting(req -> req.param("filter[kafkaClusters]", "contains," + kafkaClusterId(kafkaCluster)).get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data[0].id", is(Base64.getUrlEncoder().withoutPadding().encodeToString(("default/" + expectedName).getBytes())))
            .body("data[0].attributes.namespace", is("default"))
            .body("data[0].attributes.name", is(expectedName))
            .body("data[0].attributes.commit", is(expectedCommit))
            .body("data[0].attributes.kafkaClusterId", is(expectedKafkaId))
            .body("data[0].attributes.version", is(expectedVersion));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-kafka1, test-connect1, abc123d, k1-id, 4.0.0",
        "default/test-kafka2, test-connect2, zyx987w, k2-id, 4.0.1",
    })
    void testListConnectClustersWithPlugins(String kafkaCluster,
            String expectedName, String expectedCommit, String expectedKafkaId, String expectedVersion) {

        whenRequesting(req -> req
                .param("filter[kafkaClusters]", "in," + kafkaClusterId(kafkaCluster))
                .param("fields[connects]", "commit,kafkaClusterId,version,plugins,namespace")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data[0].id", is(Base64.getUrlEncoder().withoutPadding().encodeToString(("default/" + expectedName).getBytes())))
            .body("data[0].attributes.namespace", is("default"))
            .body("data[0].attributes.commit", is(expectedCommit))
            .body("data[0].attributes.kafkaClusterId", is(expectedKafkaId))
            .body("data[0].attributes.version", is(expectedVersion))
            .body("data[0].attributes.plugins.version", everyItem(is(expectedVersion)));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-kafka1, test-connect1",
        "default/test-kafka2, test-connect2",
    })
    void testListConnectClustersWithConnectors(String kafkaCluster, String expectedName) {
        whenRequesting(req -> req
                .param("filter[kafkaClusters]", "contains," + kafkaClusterId(kafkaCluster))
                .param("fields[connects]", "connectors")
                .param("include", "connectors")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data[0].id", is(Base64.getUrlEncoder().withoutPadding().encodeToString(("default/" + expectedName).getBytes())))
            .body("data[0].relationships.connectors.data.size()", is(2))
            .body("included.size()", is(2));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-kafka1, test-connect1",
        "default/test-kafka2, test-connect2",
    })
    void testListConnectClustersWithConnectorsNotIncluded(String kafkaCluster, String expectedName) {
        whenRequesting(req -> req
                .param("filter[kafkaClusters]", "in," + kafkaClusterId(kafkaCluster))
                .param("fields[connects]", "connectors")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data[0].id", is(Base64.getUrlEncoder().withoutPadding().encodeToString(("default/" + expectedName).getBytes())))
            .body("data[0].relationships.connectors.data.size()", is(2))
            .body("included", anyOf(nullValue(), hasSize(0))); // included is either missing or empty
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-kafka1, test-connect1",
        "default/test-kafka2, test-connect2",
    })
    void testListConnectClustersWithConnectorTasks(String kafkaCluster, String expectedName) {
        whenRequesting(req -> req
                .param("filter[kafkaClusters]", "contains," + kafkaClusterId(kafkaCluster))
                .param("fields[connects]", "connectors")
                .param("fields[connectors]", "tasks")
                .param("include", "connectors,tasks")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data[0].id", is(Base64.getUrlEncoder().withoutPadding().encodeToString(("default/" + expectedName).getBytes())))
            .body("included.size()", is(5)) // 2 connectors + 3 tasks
            .body("included.findAll { it.type == 'connectors' }.size()", is(2))
            .body("included.findAll { it.type == 'connectors' }.relationships.tasks.data", containsInAnyOrder(hasSize(1), hasSize(2)))
            .body("included.findAll { it.type == 'connectors' }.relationships.tasks.data.type.flatten()", everyItem(is("connectorTasks")))
            .body("included.findAll { it.type == 'connectorTasks' }.size()", is(3))
            .body("included.findAll { it.type == 'connectorTasks' }.relationships.connector.data.type.flatten()", everyItem(is("connectors")));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-kafka1, test-connect1",
        "default/test-kafka2, test-connect2",
    })
    void testListConnectClustersWithConnectorTasksNotIncluded(String kafkaCluster, String expectedName) {
        // Tests that the connectorTasks relationships are returned, but the resources themselves are not included
        whenRequesting(req -> req
                .param("filter[kafkaClusters]", "in," + kafkaClusterId(kafkaCluster))
                .param("fields[connects]", "connectors")
                .param("fields[connectors]", "tasks")
                .param("include", "connectors")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data[0].id", is(Base64.getUrlEncoder().withoutPadding().encodeToString(("default/" + expectedName).getBytes())))
            .body("included.size()", is(2)) // 2 connectors + no tasks
            .body("included.type", everyItem(is("connectors")))
            .body("included.relationships.tasks.data", containsInAnyOrder(hasSize(1), hasSize(2)))
            .body("included.relationships.tasks.data.type.flatten()", everyItem(is("connectorTasks")));
    }

    @ParameterizedTest
    @CsvSource({
        "default/test-connect1, default/test-kafka1",
        "default/test-connect2, default/test-kafka2",
    })
    void testDescribeConnectCluster(String connectClusterKey, String kafkaClusterKey) {
        String connectClusterID = Base64.getUrlEncoder().withoutPadding().encodeToString(connectClusterKey.getBytes());
        // Tests that the connectorTasks relationships are returned, but the resources themselves are not included
        whenRequesting(req -> req
                .param("fields[connects]", "name,kafkaClusters")
                .get(connectClusterID))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is(connectClusterID))
            .body("data.attributes.name", is(connectClusterKey.substring(connectClusterKey.indexOf('/') + 1)))
            .body("data.relationships.kafkaClusters.data.id", contains(kafkaClusterId(kafkaClusterKey)));
    }
}
