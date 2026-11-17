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

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaConnectClustersIT implements ClientRequestFilter {

    static final String CONNECT_CLUSTER = "test-connect1";
    static final String CONNECT_VERSION = "4.0.0";

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
        try (InputStream in = KafkaConnectClustersIT.class.getResourceAsStream(
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
                    .withName(CONNECT_CLUSTER)
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
            assertThat(clusterLink(page, CONNECT_CLUSTER)).isVisible();

            filter.fill("no-such-connect-cluster");
            filter.press("Enter");

            assertThat(clusterLink(page, CONNECT_CLUSTER)).hasCount(0);
        }
    }
}
