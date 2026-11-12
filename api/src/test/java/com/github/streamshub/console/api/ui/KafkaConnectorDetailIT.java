package com.github.streamshub.console.api.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
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
 * UI tests for the connector detail page. Each test opens the page by clicking the
 * connector's link on the connectors list, so the test does not need to compute the
 * (encoded) connector id.
 *
 * Expected values come from KafkaConnect-fixtures.yaml, host test-connect1.example.com.
 */
@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaConnectorDetailIT implements ClientRequestFilter {

    static final String CONNECTOR_1 = "connect1-connector1";
    static final String CONNECTOR_2 = "connect1-connector2";

    static final Pattern RUNNING = Pattern.compile("Running", Pattern.CASE_INSENSITIVE);

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
        try (InputStream in = KafkaConnectorDetailIT.class.getResourceAsStream(
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
        context.setDefaultTimeout(15_000);

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

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private String connectorsUrl() {
        return home + "kafka/" + clusterId + "/connect/connectors";
    }

    /** Opens the detail page by following the connector's link on the list page. */
    private void openConnector(Page page, String connectorName) {
        page.navigate(connectorsUrl());

        page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName(connectorName).setExact(true)).click();

        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setLevel(1).setName(connectorName))).isVisible();
    }

    private static Locator tasksTable(Page page) {
        return page.locator("table[aria-label='Connector tasks']");
    }

    private static Locator configTable(Page page) {
        return page.locator("table[aria-label='Connector configuration']");
    }

    @Test
    void headerShowsConnectorNameAndManagedLabel() {
        try (Page page = context.newPage()) {
            openConnector(page, CONNECTOR_1);

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1))).containsText("Managed");
        }
    }

    @Test
    void tasksTabIsSelectedByDefaultAndListsTasks() {
        try (Page page = context.newPage()) {
            openConnector(page, CONNECTOR_1);

            assertThat(page.getByRole(AriaRole.TAB,
                    new Page.GetByRoleOptions().setName("Tasks"))).hasAttribute("aria-selected", "true");

            Locator rows = tasksTable(page).locator("tbody tr");
            assertThat(rows).hasCount(2);

            // task 0 and task 1, both running on node1
            assertThat(rows.nth(0)).containsText("0");
            assertThat(rows.nth(1)).containsText("1");
            for (int i = 0; i < 2; i++) {
                assertThat(rows.nth(i)).containsText(RUNNING);
                assertThat(rows.nth(i)).containsText("node1.test-cluster.example.com");
            }
        }
    }

    @Test
    void tasksTableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            openConnector(page, CONNECTOR_1);

            for (String header : new String[] {"Task ID", "State", "Worker ID" }) {
                assertThat(tasksTable(page).getByRole(AriaRole.COLUMNHEADER,
                        new Locator.GetByRoleOptions().setName(header))).isVisible();
            }
        }
    }

    @Test
    void connectorWithOneTaskListsOneRow() {
        try (Page page = context.newPage()) {
            openConnector(page, CONNECTOR_2);

            assertThat(tasksTable(page).locator("tbody tr")).hasCount(1);
        }
    }

    @Test
    void configurationTabShowsConnectorConfig() {
        try (Page page = context.newPage()) {
            openConnector(page, CONNECTOR_1);

            page.getByRole(AriaRole.TAB,
                    new Page.GetByRoleOptions().setName("Configuration")).click();

            assertThat(configTable(page)).isVisible();

            Locator classRow = configTable(page).locator("tbody tr")
                    .filter(new Locator.FilterOptions().setHasText("connector.class"));
            assertThat(classRow).containsText("my.custom.Connector1");

            // Switching tabs hides the tasks table
            assertThat(tasksTable(page)).not().isVisible();
        }
    }

    // ---------------------------------------------------------------------
    // Errors
    // ---------------------------------------------------------------------

    @Test
    void unknownConnectorShowsError() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl() + "/does-not-exist");

            assertThat(page.getByText("Error loading connector")).isVisible();
        }
    }
}
