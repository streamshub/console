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
import com.microsoft.playwright.assertions.PlaywrightAssertions;
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
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the Topics page against the real "test-kafka1" cluster (id "k1-id")
 * from config-testplain.yaml. Topics are created directly in the test broker with the
 * Kafka Admin client, using a random per-test name prefix so tests never collide.
 */
@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaTopicsIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;

    static final Pattern TOPICS_TITLE   = ci("Topics");
    static final Pattern NAME_COLUMN    = ci("Name");
    static final Pattern STATUS_COLUMN  = ci("Status");
    static final Pattern PARTITIONS_COL = ci("Partitions");
    static final Pattern GROUPS_COLUMN  = ci("Group");
    static final Pattern STORAGE_COLUMN = ci("Storage");
    static final Pattern MANAGED_LABEL  = ci("Managed");

    static final Pattern TOTAL_LABEL    = Pattern.compile("\\d+[\\s\\u00A0]*total");

    static final String LISTING_FIELDS  = "name,status,numPartitions,totalLeaderLogBytes,groups";

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

    final String prefix = "ui-topics-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String ordersTopic   = prefix + "orders";    
    final String paymentsTopic = prefix + "payments";  
    final String eventsTopic   = prefix + "events";    

    @BeforeEach
    void setup() throws Exception {
        context.setDefaultTimeout(15_000);
        PlaywrightAssertions.setDefaultAssertionTimeout(15_000);

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        applyKafka();
        createTopics();
    }

    @AfterEach
    void cleanup() {
        try (Admin admin = admin()) {
            admin.deleteTopics(List.of(ordersTopic, paymentsTopic, eventsTopic)).all().get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            // best effort: topics have unique names, so leftovers cannot affect other tests
        }
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private void applyKafka() {
        clusterId = consoleConfig.getKafka().getCluster(KAFKA_KEY).get().getId(); // "k1-id"

        Kafka kafka = utils.apply(client, new KafkaBuilder(
                utils.buildKafkaResource(KAFKA_NAME, clusterId, bootstrapServers))
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

    private Admin admin() {
        return Admin.create(Map.of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class)));
    }

    private void createTopics() throws Exception {
        try (Admin admin = admin()) {
            admin.createTopics(List.of(
                    new NewTopic(ordersTopic, 3, (short) 1),
                    new NewTopic(paymentsTopic, 1, (short) 1),
                    new NewTopic(eventsTopic, 2, (short) 1)))
                .all().get(30, TimeUnit.SECONDS);
        }
    }

    private String topicsUrl() {
        return home + "kafka/" + clusterId + "/topics";
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
            page.navigate(topicsUrl());

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(TOPICS_TITLE))).isVisible();
            assertThat(page.getByText(TOTAL_LABEL).first()).isVisible();
        }
    }

    // ---------------------------------------------------------------------
    // Table
    // ---------------------------------------------------------------------

    @Test
    void tableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            for (Pattern header : List.of(NAME_COLUMN, STATUS_COLUMN, PARTITIONS_COL,
                    GROUPS_COLUMN, STORAGE_COLUMN)) {
                assertThat(page.getByRole(AriaRole.COLUMNHEADER,
                        new Page.GetByRoleOptions().setName(header)).first()).isVisible();
            }
        }
    }

    @Test
    void tableListsCreatedTopics() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(rowFor(page, ordersTopic)).hasCount(1);
            assertThat(rowFor(page, paymentsTopic)).hasCount(1);
            assertThat(rowFor(page, eventsTopic)).hasCount(1);
        }
    }

    @Test
    void topicRowLinksPointToTopicPages() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            Locator row = rowFor(page, ordersTopic);
            assertThat(row).hasCount(1);

            // name -> topic page (the id segment is the Kafka topic id, so match any)
            assertThat(row.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(ordersTopic)))
                    .hasAttribute("href", Pattern.compile(".*/kafka/" + clusterId + "/topics/[^/]+$"));

            // 3 partitions, linked to the partitions tab
            assertThat(row.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("3").setExact(true)))
                    .hasAttribute("href", Pattern.compile(".*/topics/[^/]+/partitions$"));

            // no consumer groups yet, linked to the groups tab
            assertThat(row.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("0").setExact(true)))
                    .hasAttribute("href", Pattern.compile(".*/topics/[^/]+/groups$"));
        }
    }

    @Test
    void partitionCountsMatchCreatedTopics() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(rowFor(page, paymentsTopic).getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName("1").setExact(true))).hasCount(1);
            assertThat(rowFor(page, eventsTopic).getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName("2").setExact(true))).hasCount(1);
        }
    }

    @Test
    void unmanagedTopicsHaveNoManagedLabel() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            assertThat(rowFor(page, ordersTopic)).hasCount(1);
            assertThat(rowFor(page, ordersTopic)).not().containsText(MANAGED_LABEL);
        }
    }

    // ---------------------------------------------------------------------
    // Requests and filtering
    // ---------------------------------------------------------------------

    @Test
    void listRequestAsksForListingFields() {
        try (Page page = context.newPage()) {
            List<String> requests = new CopyOnWriteArrayList<>();
            page.onRequest(r -> requests.add(URLDecoder.decode(r.url(), StandardCharsets.UTF_8)));

            page.navigate(topicsUrl());
            assertThat(rowFor(page, ordersTopic)).hasCount(1);

            assertTrue(requests.stream().anyMatch(u -> u.contains("/api/kafkas/" + clusterId + "/topics")
                            && u.contains(LISTING_FIELDS)),
                    "expected a topics request with fields=" + LISTING_FIELDS + " but got " + requests);
        }
    }

    @Test
    void filteringByNameShowsOnlyMatchingTopics() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());
            assertThat(rowFor(page, paymentsTopic)).hasCount(1);

            // The first text input on the page is the name filter (the default filter attribute)
            Locator filter = page.getByRole(AriaRole.TEXTBOX).first();
            filter.fill(ordersTopic);
            filter.press("Enter");

            assertThat(rowFor(page, ordersTopic)).hasCount(1);
            assertThat(rowFor(page, paymentsTopic)).hasCount(0);
            assertThat(rowFor(page, eventsTopic)).hasCount(0);
        }
    }
}
