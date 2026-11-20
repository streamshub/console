package io.streamshub.console.api.ui;

import io.streamshub.console.kafka.systemtest.TestPlainProfile;
import io.streamshub.console.test.TopicHelper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaTopicPartitionsIT extends BaseKafkaIT {
    static final int PARTITIONS = 3;
    static final int MANY_PARTITIONS = 25;

    static final List<Pattern> HEADERS = List.of(
            ci("Partition"), ci("Status"), ci("Preferred leader"),
            ci("Leader"), ci("Replicas"), ci("Size|Storage"));

    @TestHTTPResource("/")
    URL home;

    TopicHelper topicUtils;

    final String prefix = "ui-partitions-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topic     = prefix + "topic";
    final String manyTopic = prefix + "many";

    @BeforeEach
    void partitionsSetup() {
        topicUtils = new TopicHelper(bootstrapServers, config);

        applyKafka();

        topicUtils.createTopics(List.of(topic), PARTITIONS);
    }

    @AfterEach
    void partitionsCleanup() {
        topicUtils.deleteTopics(topic, manyTopic);
    }

    private String topicsUrl() {
        return home + "kafka/" + clusterId + "/topics";
    }

    private static Locator rowFor(Page page, String topicName) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(topicName));
    }

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

    private static Locator cell(Locator row, int column) {
        return row.locator("td").nth(column);
    }

    @Test
    void partitionCountLinkOpensPartitionsTab() {
        try (Page page = context.newPage()) {
            openPartitions(page, topic, PARTITIONS);

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
            assertThat(partitionRows(page)).hasCount(20);

            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(ci("next page"))).first().click();

            assertThat(partitionRows(page)).hasCount(MANY_PARTITIONS - 20);
            assertThat(cell(partitionRows(page).first(), 0)).hasText("20");
        }
    }
}
