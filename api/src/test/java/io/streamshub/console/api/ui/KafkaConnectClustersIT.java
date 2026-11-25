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

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaConnectClustersIT extends BaseKafkaIT implements ClientRequestFilter {

    static final String CONNECT_CLUSTER = "test-connect1";
    static final String CONNECT_VERSION = "4.0.0";

    @Inject
    KafkaConnectAPI.Client connectClient;

    Consumer<ClientRequestContext> filterRequest;

    static ObjectNode fixtures;

    @BeforeAll
    static void initialize() throws IOException {
        try (InputStream in = KafkaConnectClustersIT.class.getResourceAsStream(
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
                    .withName(CONNECT_CLUSTER)
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

    private String clustersUrl() {
        return home + "kafka/" + clusterId + "/connect/clusters";
    }

    private static Locator row(Page page, String clusterName) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(clusterName));
    }

    private static Locator clusterLink(Page page, String clusterName) {
        return page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName(clusterName).setExact(true));
    }

    private static Locator columnHeader(Page page, String name) {
        return page.getByRole(AriaRole.COLUMNHEADER,
                new Page.GetByRoleOptions().setName(ci(name)));
    }

    @Test
    void clustersPageShowsTableAndExpectedColumns() {
        try (Page page = context.newPage()) {
            page.navigate(clustersUrl());

            assertThat(page.getByRole(AriaRole.TABLE)
                    .or(page.getByRole(AriaRole.GRID)).first()).isVisible();

            for (String header : new String[] {"Name", "Version", "Workers" }) {
                assertThat(columnHeader(page, header).first()).isVisible();
            }
        }
    }

    @Test
    void clustersPageShowsConfiguredConnectCluster() {
        try (Page page = context.newPage()) {
            page.navigate(clustersUrl());

            assertThat(clusterLink(page, CONNECT_CLUSTER)).isVisible();
        }
    }

    @Test
    void clusterShowsVersionFromConnectApi() {
        try (Page page = context.newPage()) {
            page.navigate(clustersUrl());

            assertThat(row(page, CONNECT_CLUSTER)).containsText(CONNECT_VERSION);
        }
    }

    @Test
    void clusterNameLinkOpensClusterDetails() {
        try (Page page = context.newPage()) {
            page.navigate(clustersUrl());

            Locator link = clusterLink(page, CONNECT_CLUSTER);
            assertThat(link).isVisible();
            assertThat(link).hasAttribute(
                    "href", Pattern.compile(".*/kafka/" + clusterId + "/connect/clusters/.+"));
        }
    }

    @Test
    void workersHeaderShowsHelpTooltip() {
        try (Page page = context.newPage()) {
            page.navigate(clustersUrl());

            // The tooltip sits on the help icon inside the header; hover the icon itself
            columnHeader(page, "Workers").first().locator("svg").hover();

            assertThat(page.getByRole(AriaRole.TOOLTIP)).containsText(ci("worker"));
        }
    }

    @Test
    void nameFilterShowsOnlyMatchingClusters() {
        try (Page page = context.newPage()) {
            page.navigate(clustersUrl());

            Locator filter = page.getByPlaceholder(ci("Filter by name"));
            assertThat(filter).isVisible();
            // Anchor: the row is rendered before filtering, so hasCount(0) is a real "present -> gone" check
            assertThat(clusterLink(page, CONNECT_CLUSTER)).isVisible();

            filter.fill("no-such-connect-cluster");
            filter.press("Enter");

            assertThat(clusterLink(page, CONNECT_CLUSTER)).hasCount(0);
        }
    }
}

