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
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the Topics page, run against the "test-kafka2" cluster (id "k2-id")
 * from config-testplain.yaml.
 *
 * test-kafka2 has no Strimzi `kind` (so no Kafka CR is needed) and its bootstrap
 * address is `console.test.random-bootstrap`, which is not a real broker. The topics
 * API is therefore mocked at the browser level so the data is deterministic.
 */
@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaTopicsIT {
    static final String KAFKA_KEY = "default/test-kafka2";

    static final Pattern TOPICS_TITLE   = ci("Topics");
    static final Pattern NAME_COLUMN    = ci("Name");
    static final Pattern STATUS_COLUMN  = ci("Status");
    static final Pattern PARTITIONS_COL = ci("Partitions");
    static final Pattern GROUPS_COLUMN  = ci("Group");
    static final Pattern STORAGE_COLUMN = ci("Storage");
    static final Pattern MANAGED_LABEL  = ci("Managed");

    static final Pattern THREE_TOTAL    = Pattern.compile("3[\\s\\u00A0]*total");
    static final Pattern ZERO_TOTAL     = Pattern.compile("0[\\s\\u00A0]*total");

    static final String LISTING_FIELDS  = "name,status,numPartitions,totalLeaderLogBytes,groups";

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL home;

    @Inject
    Config config;

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

        // "k2-id": a configured cluster with no Kafka CR behind it
        clusterId = consoleConfig.getKafka().getCluster(KAFKA_KEY).get().getId();

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> configuredContexts.values().stream()
                        .map(KafkaContext::clusterConfig)
                        .map(KafkaClusterConfig::getId)
                        .anyMatch(clusterId::equals));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private String topicsUrl() {
        return home + "kafka/" + clusterId + "/topics";
    }

    private static void json(Route route, String body) {
        route.fulfill(new Route.FulfillOptions()
                .setStatus(200).setContentType("application/json").setBody(body));
    }

    private static String topic(String id, String name, String status, int partitions,
            long leaderLogBytes, int groups, boolean managed) {
        return """
                {"id":"%s","type":"topics","meta":{"managed":%b},
                 "attributes":{"name":"%s","status":"%s","numPartitions":%d,"totalLeaderLogBytes":%d},
                 "relationships":{"groups":{"meta":{"count":%d}}}}
                """.formatted(id, managed, name, status, partitions, leaderLogBytes, groups);
    }

    private static String topicList(int total, String statuses, String... topics) {
        return """
                {"data":[%s],
                 "meta":{"page":{"total":%d},"summary":{"statuses":%s}}}
                """.formatted(String.join(",", topics), total, statuses);
    }

    private String threeTopics() {
        return topicList(3, "{\"FullyReplicated\":2,\"UnderReplicated\":1}",
                topic("id-orders", "orders", "FullyReplicated", 6, 2048, 2, true),
                topic("id-payments", "payments", "UnderReplicated", 3, 1024, 0, false),
                topic("id-events", "events", "FullyReplicated", 1, 512, 1, false));
    }

    /**
     * Mock the topics list API for this cluster only (never the page URL itself, which
     * is /kafka/&lt;id&gt;/topics and has no /api/). Returns the decoded URLs requested.
     * If {@code onlyOrders} is given, any request mentioning "orders" (the name filter)
     * gets that response instead of the default.
     */
    private List<String> mockTopics(Page page, String body, String filteredBody) {
        List<String> requests = new CopyOnWriteArrayList<>();
        Pattern listCall = Pattern.compile(".*/api/kafkas/" + Pattern.quote(clusterId) + "/topics(\\?.*)?$");

        page.route(url -> listCall.matcher(url).matches(), route -> {
            String url = URLDecoder.decode(route.request().url(), StandardCharsets.UTF_8);
            requests.add(url);
            json(route, filteredBody != null && url.contains("orders") ? filteredBody : body);
        });

        return requests;
    }

    private static Locator rowFor(Page page, String topicName) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(topicName));
    }

    // ---------------------------------------------------------------------
    // Header
    // ---------------------------------------------------------------------

    @Test
    void headerShowsTitleAndTotal() {
        try (Page page = context.newPage()) {
            mockTopics(page, threeTopics(), null);
            page.navigate(topicsUrl());

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(TOPICS_TITLE))).isVisible();
            assertThat(page.getByText(THREE_TOTAL).first()).isVisible();
        }
    }

    @Test
    void headerShowsZeroTotalWhenNoTopics() {
        try (Page page = context.newPage()) {
            mockTopics(page, topicList(0, "{}"), null);
            page.navigate(topicsUrl());

            assertThat(page.getByText(ZERO_TOTAL).first()).isVisible();
            assertThat(rowFor(page, "orders")).hasCount(0);
        }
    }

    // ---------------------------------------------------------------------
    // Table
    // ---------------------------------------------------------------------

    @Test
    void tableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            mockTopics(page, threeTopics(), null);
            page.navigate(topicsUrl());

            for (Pattern header : List.of(NAME_COLUMN, STATUS_COLUMN, PARTITIONS_COL,
                    GROUPS_COLUMN, STORAGE_COLUMN)) {
                assertThat(page.getByRole(AriaRole.COLUMNHEADER,
                        new Page.GetByRoleOptions().setName(header)).first()).isVisible();
            }
        }
    }

    @Test
    void tableListsAllTopics() {
        try (Page page = context.newPage()) {
            mockTopics(page, threeTopics(), null);
            page.navigate(topicsUrl());

            assertThat(rowFor(page, "orders")).hasCount(1);
            assertThat(rowFor(page, "payments")).hasCount(1);
            assertThat(rowFor(page, "events")).hasCount(1);
        }
    }

    @Test
    void topicRowLinksPointToTopicPages() {
        try (Page page = context.newPage()) {
            mockTopics(page, threeTopics(), null);
            page.navigate(topicsUrl());

            Locator row = rowFor(page, "orders");

            assertThat(row.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("orders")))
                    .hasAttribute("href", Pattern.compile(".*/kafka/" + clusterId + "/topics/id-orders$"));

            // partitions (6) and consumer groups (2) are links to the sub pages
            assertThat(row.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("6").setExact(true)))
                    .hasAttribute("href", Pattern.compile(".*/topics/id-orders/partitions$"));
            assertThat(row.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("2").setExact(true)))
                    .hasAttribute("href", Pattern.compile(".*/topics/id-orders/groups$"));
        }
    }

    @Test
    void managedTopicShowsManagedLabelOnlyForManagedTopics() {
        try (Page page = context.newPage()) {
            mockTopics(page, threeTopics(), null);
            page.navigate(topicsUrl());

            assertThat(rowFor(page, "orders")).containsText(MANAGED_LABEL);
            assertThat(rowFor(page, "payments")).not().containsText(MANAGED_LABEL);
        }
    }

    // ---------------------------------------------------------------------
    // Requests and filtering
    // ---------------------------------------------------------------------

    @Test
    void listRequestAsksForListingFieldsOnly() {
        try (Page page = context.newPage()) {
            List<String> requests = mockTopics(page, threeTopics(), null);
            page.navigate(topicsUrl());

            assertThat(rowFor(page, "orders")).hasCount(1);
            assertTrue(requests.stream().anyMatch(u -> u.contains(LISTING_FIELDS)),
                    "expected a topics request with fields=" + LISTING_FIELDS + " but got " + requests);
        }
    }

    @Test
    void filteringByNameSendsFilterAndShowsOnlyMatches() {
        String onlyOrders = topicList(1, "{\"FullyReplicated\":1}",
                topic("id-orders", "orders", "FullyReplicated", 6, 2048, 2, true));

        try (Page page = context.newPage()) {
            List<String> requests = mockTopics(page, threeTopics(), onlyOrders);
            page.navigate(topicsUrl());
            assertThat(rowFor(page, "payments")).hasCount(1);

            // First text input on the page is the name filter (the default filter attribute)
            Locator filter = page.getByRole(AriaRole.TEXTBOX).first();
            filter.fill("orders");
            filter.press("Enter");

            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertTrue(
                            requests.stream().anyMatch(u -> u.contains("filter") && u.contains("orders")),
                            "expected a request carrying the name filter but got " + requests));

            assertThat(rowFor(page, "orders")).hasCount(1);
            assertThat(rowFor(page, "payments")).hasCount(0);
            assertThat(rowFor(page, "events")).hasCount(0);
        }
    }
}
