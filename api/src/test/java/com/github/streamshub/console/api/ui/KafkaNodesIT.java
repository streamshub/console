package com.github.streamshub.console.api.ui;

import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.TestHelper;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import jakarta.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaNodesIT {
    static final String K2_ID = "k2-id";

    static final Pattern NODES_TITLE      = ci("Kafka Nodes");
    static final Pattern OVERVIEW_TAB     = ci("Overview");
    static final Pattern REBALANCES_TAB   = ci("Rebalance");
    static final Pattern CC_NOT_ENABLED   = ci("Cruise Control");

    static final Pattern TOTAL_LABEL      = Pattern.compile("\\d+[\\s\\u00A0]*total");
    static final Pattern NUMBER           = Pattern.compile("\\d+");
    static final Pattern ONE              = Pattern.compile("1");
    static final Pattern LEAD_CONTROLLER_NODE = ci("Node\\s*0");
    static final Pattern PAGINATION_1_OF_1    = Pattern.compile("1\\s*-\\s*1\\s*of\\s*1");

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
    KafkaClusterService kafkaClusterService;

    TestHelper utils;
    URI bootstrapServers;
    String clusterId;

    @BeforeEach
    void setup() {
        context.setDefaultTimeout(15_000);

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        resetStrimziResources();   
        applyKafka();          
        awaitNodesReady();   

    }

    @AfterEach
    void cleanup() {
        resetStrimziResources();
    }

    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private void applyKafka() {
        clusterId = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();

        Kafka kafka = utils.apply(client, new KafkaBuilder(
                utils.buildKafkaResource("test-kafka1", clusterId, bootstrapServers))
                .editOrNewStatus()
                .addNewCondition()
                .withType("Ready")
                .withStatus("True")
                .endCondition()
                .endStatus()
                .build());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> configuredContexts.values().stream()
                        .map(KafkaContext::clusterConfig)
                        .map(KafkaClusterConfig::clusterKey)
                        .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));
    }
    
    private void resetStrimziResources() {
        client.resources(io.strimzi.api.kafka.model.nodepool.KafkaNodePool.class).inAnyNamespace().delete();
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    private void awaitNodesReady() {
        var request = context.request();
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = request.get(home + "api/kafkas/" + clusterId + "/nodes");
            String body = response.text();
            assertTrue(response.ok(), "status " + response.status());
            assertTrue(
                ci("controller").matcher(body).find(), body);
        });
    }

    private String nodesUrl(String suffix) {
        return nodesUrl(clusterId, suffix);
    }

    private String nodesUrl(String id, String suffix) {
        return home + "kafka/" + id + "/nodes" + suffix;
    }

    private static Locator ouia(Page page, String id) {
        return page.locator("[data-ouia-component-id='" + id + "']");
    }

    private static Locator tab(Page page, Pattern name) {
        return page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(name));
    }

    private static void json(Route route, String body) {
        route.fulfill(new Route.FulfillOptions()
                .setStatus(200).setContentType("application/json").setBody(body));
    }

    private AtomicInteger mockCruiseControl(Page page, boolean enabled) {
        AtomicInteger rebalanceRequests = new AtomicInteger();

        page.route(url -> url.contains("/api/kafkas/" + clusterId)
                        && url.contains("cruiseControlEnabled")
                        && !url.contains("/rebalances"),
                route -> json(route, """
                        {"data":{"id":"%s","type":"kafkas",
                          "attributes":{"cruiseControlEnabled":%b}}}
                        """.formatted(clusterId, enabled)));

        // Match API calls only: the page URL itself (/nodes/rebalances) also contains "/rebalances"
        page.route(url -> url.contains("/api/") && url.contains("/rebalances"),
                route -> {
                    rebalanceRequests.incrementAndGet();
                    json(route, "{\"data\":[],\"meta\":{\"page\":{\"total\":0}}}");
                });

        return rebalanceRequests;
    }

    @Test
    void nodesRootRedirectsToOverview() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl(""));

            assertThat(page).hasURL(Pattern.compile(".*/kafka/[^/]+/nodes/overview/?$"));
        }
    }

    @Test
    void rebalancesUrlOpensRebalancesTab() {
        try (Page page = context.newPage()) {
            mockCruiseControl(page, true);
            page.navigate(nodesUrl("/rebalances"));

            assertThat(tab(page, REBALANCES_TAB)).hasAttribute("aria-selected", "true");
            assertThat(tab(page, OVERVIEW_TAB)).hasAttribute("aria-selected", "false");
        }
    }

    @Test
    void headerShowsTitleAndNodeCountLabels() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl("/overview"));

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(NODES_TITLE))).isVisible();
            assertThat(ouia(page, "label-total")).containsText(TOTAL_LABEL);
            assertThat(ouia(page, "label-healthy")).containsText(NUMBER);
            assertThat(ouia(page, "label-unhealthy")).containsText(NUMBER);
        }
    }

    @Test
    void overviewTabIsSelectedByDefault() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl(""));
            page.waitForURL("**/kafka/*/nodes/overview");

            assertThat(tab(page, OVERVIEW_TAB)).hasAttribute("aria-selected", "true");
            assertThat(tab(page, REBALANCES_TAB)).hasAttribute("aria-selected", "false");
        }
    }

    @Test
    void switchingTabsUpdatesUrlAndSelection() {
        try (Page page = context.newPage()) {
            mockCruiseControl(page, false);
            page.navigate(nodesUrl("/overview"));

            tab(page, REBALANCES_TAB).click();
            page.waitForURL("**/kafka/*/nodes/rebalances");
            assertThat(tab(page, REBALANCES_TAB)).hasAttribute("aria-selected", "true");

            tab(page, OVERVIEW_TAB).click();
            page.waitForURL("**/kafka/*/nodes/overview");
            assertThat(tab(page, OVERVIEW_TAB)).hasAttribute("aria-selected", "true");
        }
    }

    @Test
    void overviewTabShowsSummaryCard() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl("/overview"));

            assertThat(ouia(page, "summary")).isVisible();
            assertThat(ouia(page, "total-node-count")).containsText(NUMBER);
            assertThat(ouia(page, "controller-node-count")).containsText(NUMBER);
            assertThat(ouia(page, "broker-node-count")).containsText(NUMBER);
            assertThat(ouia(page, "lead-controller")).isVisible();
        }
    }

    @Test
    void overviewTabShowsNodesTable() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl("/overview"));

            assertThat(page.getByRole(AriaRole.GRID).or(page.getByRole(AriaRole.TABLE)).first())
                    .isVisible();
        }
    }

    @Test
    void overviewTabSummaryCardShowsSingleNodeCounts() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl("/overview"));
            assertThat(ouia(page, "total-node-count")).containsText(ONE);
            assertThat(ouia(page, "controller-node-count")).containsText(ONE);
            assertThat(ouia(page, "broker-node-count")).containsText(ONE);
            assertThat(ouia(page, "lead-controller")).containsText(LEAD_CONTROLLER_NODE);
        }
    }

    @Test
    void overviewTabTableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl("/overview"));

            for (String header : new String[] {
                "Node ID", "Roles", "Status", "Kafka version",
                "Total Replicas", "Leader partitions", "Rack", "Node Pool" }) {
                assertThat(page.getByRole(AriaRole.COLUMNHEADER,
                        new Page.GetByRoleOptions().setName(ci(header))).first()).isVisible();
            }
        }
    }

    @Test
    void overviewTabTableShowsSingleControllerBrokerNode() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl("/overview"));

            Locator dataRows = page.getByRole(AriaRole.ROW)
                    .filter(new Locator.FilterOptions().setHasText(ci("Quorum Leader")));
            assertThat(dataRows).hasCount(1);

            Locator row = dataRows.first();
            assertThat(row).containsText(Pattern.compile("^\\s*0"));
            assertThat(row).containsText(ci("Lead controller"));
            assertThat(row).containsText(ci("Controller"));
            assertThat(row).containsText(ci("Broker"));
            assertThat(row).containsText(ci("Running"));
            assertThat(row).containsText(ci("Quorum Leader"));
            assertThat(row).containsText(ci("n/a"));
        }
    }

    @Test
    void overviewTabShowsFilterAndPagination() {
        try (Page page = context.newPage()) {
            page.navigate(nodesUrl("/overview"));

            assertThat(page.getByText(ci("Filter by node pool")).first()).isVisible();
            assertThat(page.getByText(PAGINATION_1_OF_1).locator("visible=true").first()).isVisible();
        }
    }

    @Test
    void rebalancesTabShowsEmptyStateWhenCruiseControlDisabled() {
        try (Page page = context.newPage()) {
            AtomicInteger rebalanceRequests = mockCruiseControl(page, false);
            page.navigate(nodesUrl("/rebalances"));

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(2).setName(CC_NOT_ENABLED))).isVisible();

            Locator link = page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(ci("get started")));
            assertThat(link).hasAttribute("target", "_blank");
            assertEquals(0, rebalanceRequests.get());
        }
    }
}
