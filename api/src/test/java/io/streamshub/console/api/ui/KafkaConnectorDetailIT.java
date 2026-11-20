package io.streamshub.console.api.ui;

import java.io.IOException;
import java.io.InputStream;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.streamshub.console.api.support.KafkaConnectAPI;
import io.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.connector.KafkaConnector;
import io.strimzi.api.kafka.model.connector.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2;

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
class KafkaConnectorDetailIT extends BaseKafkaIT implements ClientRequestFilter {

    static final String CONNECTOR_1 = "connect1-connector1";
    static final String CONNECTOR_2 = "connect1-connector2";

    static final Pattern RUNNING = ci("Running");

    @Inject
    KafkaConnectAPI.Client connectClient;

    Consumer<ClientRequestContext> filterRequest;

    static ObjectNode fixtures;

    @BeforeAll
    static void initialize() throws IOException {
        try (InputStream in = KafkaConnectorDetailIT.class.getResourceAsStream(
                "/io/streamshub/console/api/KafkaConnect-fixtures.yaml")) {
            fixtures = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(in);
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        filterRequest.accept(requestContext);
    }

    @BeforeEach
    void setup() {
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

        applyKafka(builder -> builder
                .editOrNewStatus()
                    .addNewKafkaNodePool()
                        .withName("my-node-pool")
                    .endKafkaNodePool()
                .endStatus());

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

        awaitKafkaReady();
    }

    @AfterEach
    void cleanup() {
        connectClient.setAdditionalFilter(Optional.empty());
    }

    @Override
    protected void deleteStrimziResources() {
        client.resources(KafkaConnector.class).inAnyNamespace().delete();
        client.resources(KafkaMirrorMaker2.class).inAnyNamespace().delete();
        super.deleteStrimziResources();
    }

    private void awaitKafkaReady() {
        var request = context.request();

        await().atMost(30, TimeUnit.SECONDS)
                .pollInSameThread()
                .untilAsserted(() -> {
                    var response = request.get(home + "api/kafkas/" + clusterId);
                    assertEquals(200, response.status(), response.text());
                });
    }

    private String connectorsUrl() {
        return home + "kafka/" + clusterId + "/connect/connectors";
    }

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
            assertThat(tasksTable(page)).not().isVisible();
        }
    }

    @Test
    void unknownConnectorShowsError() {
        try (Page page = context.newPage()) {
            page.navigate(connectorsUrl() + "/does-not-exist");

            assertThat(page.getByText("Error loading connector")).isVisible();
        }
    }
}

