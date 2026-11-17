package com.github.streamshub.console.api.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.console.api.support.KafkaConnectAPI;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.TestHelper;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.connector.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UI tests for the Connectors page of "test-kafka1" (id "k1-id").
 *
 * Only the connectors of the Connect cluster attached to this Kafka cluster
 * (test-connect1) are listed; the connect2 connectors in the shared fixtures
 * belong to a different Kafka cluster and do not appear here.
 */
@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaConnectorsIT implements ClientRequestFilter {

    static final String CONNECTOR_1 = "connect1-connector1";
    static final String CONNECTOR_2 = "connect1-connector2";

    static final Pattern RUNNING = ci("Running");

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL home;

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    KafkaConnectAPI.Client connectClient;

    TestHelper utils;
    URI bootstrapServers;
    String clusterId;

    Consumer<ClientRequestContext> filterRequest;

    static ObjectNode fixtures;

    @BeforeAll
    static void initialize() throws IOException {
        try (InputStream in = KafkaConnectorsIT.class.getResourceAsStream(
                "/com/github/streamshub/console/api/KafkaConnect-fixtures.yaml")) {
            fixtures = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(in);
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        filterRequest.accept(requestContext);
    }

    @BeforeEach
    void setup() {
        PlaywrightTimeouts.configure(context);

        filterRequest = ctx -> {
            var uri = ctx.getUri();
            String host = uri.getHost();
            String path = uri.getPath();

            if (path == null || path.equals("/")) {
                path = "";
            }

            var hostFixtures = fixtures.get(host);
            var fixture = hostFixtures == null ? null : hostFixtures.get(path);

            Objects.requireNonNull(
                    fixture,
                    "Kafka Connect fixture not found for host " + host + "; path " + path);

            ctx.abortWith(Response.ok(fixture)
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        };

        connectClient.setAdditionalFilter(Optional.of(this));

        bootstrapServers = URI.create(
                config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);

        clusterId = consoleConfig.getKafka()
                .getCluster("default/test-kafka1")
                .orElseThrow()
                .getId();

        resetStrimziResources();
        applyKafkaAndConnectResources();
        awaitKafkaReady();
    }

    @AfterEach
    void cleanup() {
        connectClient.setAdditionalFilter(Optional.empty());
        resetStrimziResources();
    }

    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private void resetStrimziResources() {
        client.resources(io.strimzi.api.kafka.model.connector.KafkaConnector.class)
                .inAnyNamespace().delete();

        client.resources(io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2.class)
                .inAnyNamespace().delete();

        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    private void applyKafkaAndConnectResources() {
        Kafka kafka = utils.apply(client,
                new KafkaBuilder(
                        utils.buildKafkaResource("test-kafka1", clusterId, bootstrapServers))
                        .editOrNewStatus()
                            .addNewCondition()
                                .withType("Ready")
                                .withStatus("True")
                            .endCondition()
                            .addNewKafkaNodePool()
                                .withName("my-node-pool")
                            .endKafkaNodePool()
                        .endStatus()
                        .build());

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

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> configuredContexts.values()
                        .stream()
                        .map(KafkaContext::clusterConfig)
                        .map(KafkaClusterConfig::clusterKey)
                        .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));
    }

    private void awaitKafkaReady() {
        var request = context.request();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = request.get(home + "api/kafkas/" + clusterId);
            assertEquals(200, response.status(), response.text());
        });
    }

    private String connectorsUrl() {
        return home + "kafka/" + clusterId + "/connect/connectors";
    }

    private static Locator row(Page page, String connectorName) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(connectorName));
    }

    private static Locator connectorLink(Page page, String connectorName) {
        return page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName(connectorName));
    }

    private static Locator columnHeader(Page page, String name) {
        return page.getByRole(AriaRole.COLUMNHEADER,
                new Page.GetByRoleOptions().setName(ci(name)));
    }

    @Test
    void connectorsPageShowsTableAndExpectedColumns() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            assertThat(page.getByRole(AriaRole.TABLE)
                    .or(page.getByRole(AriaRole.GRID)).first()).isVisible();

            for (String header : new String[] {
                "Name", "Connect cluster", "Type", "State", "Tasks" }) {
                assertThat(columnHeader(page, header).first()).isVisible();
            }
        }
    }

    @Test
    void connectorsPageShowsAllConfiguredConnectors() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            for (String connector : List.of(CONNECTOR_1, CONNECTOR_2)) {
                assertThat(connectorLink(page, connector)).isVisible();
            }
        }
    }

    @Test
    void connectorsShowExpectedTypesAndStates() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            for (String connector : List.of(CONNECTOR_1, CONNECTOR_2)) {
                assertThat(row(page, connector)).containsText("Source");
                assertThat(row(page, connector)).containsText(RUNNING);
            }
        }
    }

    @Test
    void connectorsShowTheirConnectClusterNames() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            assertThat(row(page, CONNECTOR_1)).containsText("test-connect1");
            assertThat(row(page, CONNECTOR_2)).containsText("test-connect1");
        }
    }

    @Test
    void connectorNameLinksOpenConnectorDetails() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            Locator link = connectorLink(page, CONNECTOR_1);
            assertThat(link).isVisible();
            assertThat(link).hasAttribute(
                    "href", Pattern.compile(".*/connect/connectors/.+"));
        }
    }

    @Test
    void connectClusterLinksOpenClusterDetails() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            // Both connectors belong to test-connect1, so two links match; use the first
            Locator clusterLink = page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("test-connect1").setExact(true)).first();

            assertThat(clusterLink).isVisible();
            assertThat(clusterLink).hasAttribute(
                    "href", Pattern.compile(".*/connect/clusters/.+"));
        }
    }

    @Test
    void connectorsShowManagedLabel() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            assertThat(row(page, CONNECTOR_1)).containsText(ci("Managed"));
            assertThat(row(page, CONNECTOR_2)).containsText(ci("Managed"));
        }
    }

    @Test
    void nameFilterShowsOnlyMatchingConnectors() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl());

            Locator filter = page.getByPlaceholder(ci("Filter by name"));
            assertThat(filter).isVisible();

            filter.fill(CONNECTOR_1);
            filter.press("Enter");

            assertThat(connectorLink(page, CONNECTOR_1)).isVisible();
            assertThat(connectorLink(page, CONNECTOR_2)).hasCount(0);
        }
    }
}
