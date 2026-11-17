package com.github.streamshub.console.api.ui;

import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;
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
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import jakarta.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaTopicPartitionsIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;

    static final int PARTITIONS = 3;
    static final int MANY_PARTITIONS = 25; // more than the default page size of 20

    static final List<Pattern> HEADERS = List.of(
            ci("Partition"), ci("Status"), ci("Preferred leader"),
            ci("Leader"), ci("Replicas"), ci("Size|Storage"));

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
    TopicHelper topicUtils;
    URI bootstrapServers;
    String clusterId;

    final String prefix = "ui-partitions-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topic     = prefix + "topic";
    final String manyTopic = prefix + "many";

    @BeforeEach
    void setup() {
        PlaywrightTimeouts.configure(context);


        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        topicUtils = new TopicHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        applyKafka();

        topicUtils.createTopics(List.of(topic), PARTITIONS);
    }

    @AfterEach
    void cleanup() {
        // deleteTopics ignores topics that do not exist (e.g. manyTopic in most tests)
        topicUtils.deleteTopics(topic, manyTopic);
        client.resources(Kafka.class).inAnyNamespace().delete();
    }


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

    private String topicsUrl() {
        return home + "kafka/" + clusterId + "/topics";
    }

    private static Locator rowFor(Page page, String topicName) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(topicName));
    }

    /** Opens the Partitions tab through the partition-count link on the topics list. */
    private void openPartitions(Page page, String topicName, int partitionCount) {
        page.navigate(topicsUrl());

        rowFor(page, topicName).getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName(String.valueOf(partitionCount)).setExact(true))
                .click();

        assertThat(page).hasURL(Pattern.compile(".*/topics/[^/]+/partitions$"));
        assertThat(partitionsTable(page)).isVisible();
    }

    private static Locator partitionsTable(Page page) {
        return page.locator("table[aria-label='Partitions table']");
    }

    private static Locator partitionRows(Page page) {
        return partitionsTable(page).locator("tbody tr");
    }

    /** Column order: id, status, preferred leader, leader, replicas, size. */
    private static Locator cell(Locator row, int column) {
        return row.locator("td").nth(column);
    }


    @Test
    void partitionCountLinkOpensPartitionsTab() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            // The topic detail page header is shown above the tab content
            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(topic))).isVisible();
            assertThat(page.getByRole(AriaRole.TAB,
                    new Page.GetByRoleOptions().setName(ci("Partitions"))))
                    .hasAttribute("aria-selected", "true");
        }
    }

    @Test
    void topicNameLinkThenPartitionsTabShowsPartitions() {
        try (Page page = context.newPage()) {
            page.navigate(topicsUrl());

            rowFor(page, topic).getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName(topic)).click();

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(topic))).isVisible();

            page.getByRole(AriaRole.TAB,
                    new Page.GetByRoleOptions().setName(ci("Partitions"))).click();

            assertThat(page).hasURL(Pattern.compile(".*/topics/[^/]+/partitions$"));
            assertThat(partitionRows(page)).hasCount(PARTITIONS);
        }
    }

    @Test
    void tableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            Locator headers = partitionsTable(page).locator("thead th");
            assertThat(headers).hasCount(HEADERS.size());

            for (int i = 0; i < HEADERS.size(); i++) {
                assertThat(headers.nth(i)).containsText(HEADERS.get(i));
            }
        }
    }

    @Test
    void tableListsAllPartitionsInIdOrder() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            assertThat(partitionRows(page)).hasCount(PARTITIONS);

            for (int id = 0; id < PARTITIONS; id++) {
                assertThat(cell(partitionRows(page).nth(id), 0)).hasText(String.valueOf(id));
            }
        }
    }

    @Test
    void partitionsOfSingleReplicaTopicAreInSyncAndPreferredLeader() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            for (int i = 0; i < PARTITIONS; i++) {
                Locator row = partitionRows(page).nth(i);

                assertThat(cell(row, 1)).containsText("In-sync");
                assertThat(cell(row, 2)).hasText("Yes");
                // The leader is shown as a label holding the broker id
                assertThat(cell(row, 3)).hasText(Pattern.compile("\\d+"));
            }
        }
    }

    @Test
    void sizeColumnShowsStorageOfPartition() {
        topicUtils.produceRecord(topic, 0, null, Map.of(), "key", "value");

        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            // Either a formatted size or "N/A" when storage information is not available yet
            assertThat(cell(partitionRows(page).first(), 5))
                    .hasText(Pattern.compile("N/A|[\\d.]+ (B|KB|MB|GB|TB)"));
        }
    }

    @Test
    void filterButtonsShowPartitionCountsPerStatus() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            assertThat(page.locator("#all")).containsText("(" + PARTITIONS + ")");
            assertThat(page.locator("#in-sync")).containsText("(" + PARTITIONS + ")");
            assertThat(page.locator("#under-replicated")).containsText("(0)");
            assertThat(page.locator("#offline")).containsText("(0)");

            assertThat(page.locator("#all")).hasAttribute("aria-pressed", "true");
        }
    }

    @Test
    void inSyncFilterKeepsAllPartitions() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            page.locator("#in-sync").click();

            assertThat(page.locator("#in-sync")).hasAttribute("aria-pressed", "true");
            assertThat(partitionRows(page)).hasCount(PARTITIONS);
        }
    }

    @Test
    void offlineFilterWithoutMatchesHidesTableAndAllRestoresIt() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

            page.locator("#offline").click();

            // No offline partitions, so an empty state replaces the table
            assertThat(partitionsTable(page)).hasCount(0);

            page.locator("#all").click();

            assertThat(partitionRows(page)).hasCount(PARTITIONS);
        }
    }

    @Test
    void largeTopicIsPaginated() {
        topicUtils.createTopics(List.of(manyTopic), MANY_PARTITIONS);

        try (Page page = context.newPage()) {
            openPartitions(page, manyTopic, MANY_PARTITIONS);

            // Default page size is 20
            assertThat(partitionRows(page)).hasCount(20);

            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(ci("next page"))).first().click();

            assertThat(partitionRows(page)).hasCount(MANY_PARTITIONS - 20);
            assertThat(cell(partitionRows(page).first(), 0)).hasText("20");
        }
    }
}
