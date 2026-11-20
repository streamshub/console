package io.streamshub.console.api.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
 * UI tests for the Connectors page of "test-kafka1" (id "k1-id").
 *
 * Only the connectors of the Connect cluster attached to this Kafka cluster
 * (test-connect1) are listed; the connect2 connectors in the shared fixtures
 * belong to a different Kafka cluster and do not appear here.
 */
@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaConnectorsIT extends BaseKafkaIT implements ClientRequestFilter {

    static final String CONNECTOR_1 = "connect1-connector1";
    static final String CONNECTOR_2 = "connect1-connector2";

    static final Pattern RUNNING = ci("Running");

    @Inject
    KafkaConnectAPI.Client connectClient;

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

        // Playwright objects are not thread-safe: poll on the test thread
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
                new Page.GetByRoleOptions().setName(
                        Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE)));
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

            assertThat(connectorLink(page, CONNECTOR_1)).isVisible();
            assertThat(connectorLink(page, CONNECTOR_2)).isVisible();

            Locator filter = page.getByPlaceholder(ci("Filter by name"));
            assertThat(filter).isVisible();

            filter.fill(CONNECTOR_1);
            filter.press("Enter");

            assertThat(connectorLink(page, CONNECTOR_2)).hasCount(0);
            assertThat(connectorLink(page, CONNECTOR_1)).isVisible();
        }
    }
}
