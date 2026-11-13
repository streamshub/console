package com.github.streamshub.console.api.ui;

import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils.ConsumerType;
import com.github.streamshub.console.test.TestHelper;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaGroupsResetOffsetIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;

    static final int PARTITIONS       = 2;
    static final int RECORDS_PER_PART = 10;
    static final long INITIAL_OFFSET  = 5;

    static final int TOPIC_TOGGLE     = 0;
    static final int PARTITION_TOGGLE = 1;
    static final int OFFSET_TOGGLE    = 2;

    static final Pattern RESET_ACTION = ci("reset");
    static final Pattern RESET_BUTTON = ci("^reset");
    static final Pattern DRY_RUN      = ci("dry ?run");
    static final Pattern CANCEL       = ci("cancel");

    static final Pattern EARLIEST = ci("earliest");
    static final Pattern LATEST   = ci("latest");
    static final Pattern CUSTOM   = ci("custom");
    static final Pattern DELETE   = ci("delete");
    static final Pattern EPOCH    = ci("epoch|unix");
    static final Pattern ISO      = ci("iso|8601");

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
    ConsumerUtils consumerUtils;
    URI bootstrapServers;
    String clusterId;

    long beforeRecordsMs;

    final String prefix     = "ui-offsets-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topicName  = prefix + "topic";
    final String emptyGroup = prefix + "group";

    @BeforeEach
    void setup() {
        PlaywrightTimeouts.configure(context);
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        consumerUtils = new ConsumerUtils(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        applyKafka();

        createEmptyGroupAtOffset(INITIAL_OFFSET);
    }

    @AfterEach
    void cleanup() {
        consumerUtils.deleteGroups();
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

    private void createEmptyGroupAtOffset(long offset) {
        beforeRecordsMs = System.currentTimeMillis() - 60_000;

        consumerUtils.request(ConsumerType.CLASSIC)
                .groupId(emptyGroup)
                .topic(topicName, PARTITIONS)
                .messagesPerTopic(PARTITIONS * RECORDS_PER_PART)
                .autoClose(true)
                .consume();

        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (int partition = 0; partition < PARTITIONS; partition++) {
            offsets.put(new TopicPartition(topicName, partition), new OffsetAndMetadata(offset));
        }
        consumerUtils.alterConsumerGroupOffsets(emptyGroup, offsets);
    }

    private KafkaConsumer<byte[], byte[]> joinedConsumer(String groupId) {
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(Map.<String, Object>of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString(),
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName()));
        consumer.subscribe(List.of(topicName));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (consumer.assignment().isEmpty()) {
            consumer.poll(Duration.ofMillis(200));
            if (System.nanoTime() > deadline) {
                consumer.close();
                throw new IllegalStateException("Consumer " + groupId + " was not assigned a partition in time");
            }
        }
        return consumer;
    }

    private Map<Integer, Long> committedOffsets() {
        return consumerUtils.consumerGroupOffsets(emptyGroup)
                .entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().partition(), e -> e.getValue().offset()));
    }

    private void awaitCommittedOffsets(Map<Integer, Long> expected) {
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> assertEquals(expected, committedOffsets()));
    }

    private String groupsUrl() {
        return home + "kafka/" + clusterId + "/groups";
    }

    private static Locator rowFor(Page page, String groupId) {
        return page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(groupId));
    }

    private static Locator resetDialog(Page page) {
        return page.getByRole(AriaRole.DIALOG).first();
    }

    private static Locator dryRunDialog(Page page) {
        return page.getByRole(AriaRole.DIALOG).last();
    }

    private static Locator button(Page page, Pattern name) {
        return resetDialog(page).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name));
    }

    private static Locator successAlert(Page page) {
        return page.locator(".pf-m-inline.pf-m-success");
    }

    private static Locator errorAlert(Page page) {
        return resetDialog(page).locator(".pf-m-inline.pf-m-danger");
    }

    private void openResetModal(Page page) {
        page.navigate(groupsUrl());
        assertThat(rowFor(page, emptyGroup)).hasCount(1);

        rowFor(page, emptyGroup)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("kebab|actions")))
                .click();
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(RESET_ACTION)).click();

        assertThat(resetDialog(page)).isVisible();
    }

    private static Locator toggle(Page page, int index) {
        return resetDialog(page).locator("button.pf-v6-c-menu-toggle").nth(index);
    }

    private static Locator menuOption(Page page, String exactName) {
        Page.GetByRoleOptions options = new Page.GetByRoleOptions().setName(exactName).setExact(true);
        return page.getByRole(AriaRole.OPTION, options).or(page.getByRole(AriaRole.MENUITEM, options));
    }

    private static Locator menuOption(Page page, Pattern name) {
        Page.GetByRoleOptions options = new Page.GetByRoleOptions().setName(name);
        return page.getByRole(AriaRole.OPTION, options).or(page.getByRole(AriaRole.MENUITEM, options));
    }

    private void selectTopic(Page page) {
        toggle(page, TOPIC_TOGGLE).click();
        menuOption(page, topicName).first().click();
    }

    private void selectPartition(Page page, int partition) {
        toggle(page, PARTITION_TOGGLE).click();
        menuOption(page, String.valueOf(partition)).first().click();
    }

    private void selectOffset(Page page, Pattern optionName) {
        toggle(page, OFFSET_TOGGLE).click();
        menuOption(page, optionName).first().click();
    }

    private static Locator dateTimeInput(Page page) {
        return resetDialog(page).locator("#datetime-input");
    }

    private static void awaitDryRunRows(Page page, String... expected) {
        assertThat(dryRunDialog(page).locator("tbody tr"))
                .hasText(expected, new LocatorAssertions.HasTextOptions().setUseInnerText(true));
    }

    @Test
    void rowActionMenuOpensModalForTheGroup() {
        try (Page page = context.newPage()) {
            openResetModal(page);

            assertThat(resetDialog(page)).containsText(emptyGroup);
            assertThat(button(page, RESET_BUTTON)).isVisible();
            assertThat(button(page, DRY_RUN)).isVisible();
            assertThat(button(page, CANCEL)).isVisible();
        }
    }

    @Test
    void cancelClosesModal() {
        try (Page page = context.newPage()) {
            openResetModal(page);

            button(page, CANCEL).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }
    }

    @Test
    void closeIconClosesModal() {
        try (Page page = context.newPage()) {
            openResetModal(page);

            resetDialog(page).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("close"))).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }
    }

    @Test
    void reopeningAfterCancelStartsWithAFreshForm() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EARLIEST);
            assertThat(button(page, DRY_RUN)).isEnabled();
            button(page, CANCEL).click();
            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);

            rowFor(page, emptyGroup)
                    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("kebab|actions")))
                    .click();
            page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(RESET_ACTION)).click();

            assertThat(resetDialog(page)).isVisible();
            assertThat(button(page, DRY_RUN)).isDisabled();
            assertThat(button(page, RESET_BUTTON)).isDisabled();
        }
    }

    @Test
    void actionsAreDisabledUntilAnOffsetValueIsChosen() {
        try (Page page = context.newPage()) {
            openResetModal(page);

            assertThat(button(page, RESET_BUTTON)).isDisabled();
            assertThat(button(page, DRY_RUN)).isDisabled();

            selectOffset(page, EARLIEST);

            assertThat(button(page, RESET_BUTTON)).isEnabled();
            assertThat(button(page, DRY_RUN)).isEnabled();
        }
    }

    @Test
    void topicSelectorOffersTheTopicOfTheGroup() {
        try (Page page = context.newPage()) {
            openResetModal(page);

            toggle(page, TOPIC_TOGGLE).click();

            assertThat(menuOption(page, topicName).first()).isVisible();
        }
    }

    @Test
    void topicSearchFiltersTheTopicList() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            toggle(page, TOPIC_TOGGLE).click();
            Locator search = page.getByPlaceholder(ci("search"));

            // Anchor: the list is loaded and the topic is present before we filter
            assertThat(menuOption(page, topicName).first()).isVisible();

            search.fill("zzz-no-such-topic");
            assertThat(menuOption(page, topicName)).hasCount(0);

            search.fill(prefix);
            assertThat(menuOption(page, topicName).first()).isVisible();
        }
    }

    @Test
    void partitionSelectorIsDisabledUntilATopicIsSelected() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            assertThat(toggle(page, PARTITION_TOGGLE)).isDisabled();

            selectTopic(page);

            assertThat(toggle(page, PARTITION_TOGGLE)).isEnabled();
            toggle(page, PARTITION_TOGGLE).click();
            assertThat(menuOption(page, "0").first()).isVisible();
            assertThat(menuOption(page, "1").first()).isVisible();
        }
    }

    @Test
    void offsetOptionsDependOnTopicAndPartitionSelection() {
        try (Page page = context.newPage()) {
            openResetModal(page);

            // all topics, all partitions: no custom, no delete
            toggle(page, OFFSET_TOGGLE).click();
            assertThat(menuOption(page, EARLIEST).first()).isVisible();
            assertThat(menuOption(page, LATEST).first()).isVisible();
            assertThat(menuOption(page, CUSTOM)).hasCount(0);
            assertThat(menuOption(page, DELETE)).hasCount(0);
            menuOption(page, EARLIEST).first().click();

            // a selected topic adds "delete"
            selectTopic(page);
            toggle(page, OFFSET_TOGGLE).click();
            assertThat(menuOption(page, DELETE).first()).isVisible();
            assertThat(menuOption(page, CUSTOM)).hasCount(0);
            menuOption(page, LATEST).first().click();

            // a selected partition adds "custom"
            selectPartition(page, 0);
            toggle(page, OFFSET_TOGGLE).click();
            assertThat(menuOption(page, CUSTOM).first()).isVisible();
        }
    }

    @Test
    void customOffsetFieldAppearsWhenCustomIsChosen() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectTopic(page);
            selectPartition(page, 0);

            assertThat(resetDialog(page).locator("#custom-offset-input")).hasCount(0);
            selectOffset(page, CUSTOM);

            assertThat(resetDialog(page).locator("#custom-offset-input")).isVisible();
        }
    }

    @Test
    void dateTimeEpochUseCurrentFillsTheInput() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EPOCH);
            assertThat(dateTimeInput(page)).isVisible();

            button(page, ci("current")).click();

            assertThat(dateTimeInput(page)).hasValue(Pattern.compile("\\d{10,}"));
            assertThat(button(page, DRY_RUN)).isEnabled();
        }
    }

    @Test
    void dateTimeIsoUseCurrentFillsTheInputWithAnIsoTimestamp() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, ISO);

            button(page, ci("current")).click();

            assertThat(dateTimeInput(page)).hasValue(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
        }
    }

    @Test
    void dryRunToEarliestShowsCurrentAndNewOffsetsPerPartition() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EARLIEST);

            button(page, DRY_RUN).click();

            assertThat(dryRunDialog(page).locator("table").first()).isVisible();
            assertThat(dryRunDialog(page)).containsText(topicName);
            awaitDryRunRows(page, "0 5 0", "1 5 0");
        }

        // dry run: Kafka is untouched
        assertEquals(Map.of(0, INITIAL_OFFSET, 1, INITIAL_OFFSET), committedOffsets());
    }

    @Test
    void dryRunToLatestShowsEndOffsets() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, LATEST);

            button(page, DRY_RUN).click();

            awaitDryRunRows(page, "0 5 10", "1 5 10");
        }
    }

    @Test
    void dryRunForOnePartitionShowsOnlyThatPartition() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectTopic(page);
            selectPartition(page, 1);
            selectOffset(page, LATEST);

            button(page, DRY_RUN).click();

            awaitDryRunRows(page, "1 5 10");
        }
    }

    @Test
    void dryRunShowsTheCliCommandWhenExpanded() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EARLIEST);
            button(page, DRY_RUN).click();
            assertThat(dryRunDialog(page).locator("table").first()).isVisible();

            dryRunDialog(page).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("cli")))
                    .first().click();

            assertThat(dryRunDialog(page).locator("pre")).containsText(emptyGroup);
        }
    }

    @Test
    void closingDryRunKeepsModalOpenAndResetCanBeAppliedAfterwards() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EARLIEST);
            button(page, DRY_RUN).click();
            assertThat(dryRunDialog(page).locator("table").first()).isVisible();
            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(2);   // anchor: reset modal + dry-run modal

            dryRunDialog(page).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("cancel|close")))
                    .last().click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(1);   // only the reset modal is left
            assertThat(button(page, RESET_BUTTON)).isEnabled();

            button(page, RESET_BUTTON).click();
            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
            assertThat(successAlert(page)).isVisible();
        }

        awaitCommittedOffsets(Map.of(0, 0L, 1, 0L));
    }

    @Test
    void resetToEarliestResetsAllPartitionsClosesModalAndShowsSuccess() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EARLIEST);

            button(page, RESET_BUTTON).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
            assertThat(successAlert(page)).isVisible();
            assertThat(successAlert(page)).containsText(emptyGroup);
        }

        awaitCommittedOffsets(Map.of(0, 0L, 1, 0L));
    }

    @Test
    void resetToLatestMovesAllPartitionsToTheEnd() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, LATEST);

            button(page, RESET_BUTTON).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }

        awaitCommittedOffsets(Map.of(0, (long) RECORDS_PER_PART, 1, (long) RECORDS_PER_PART));
    }

    @Test
    void resetOfOneSelectedPartitionLeavesTheOtherUntouched() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectTopic(page);
            selectPartition(page, 1);
            selectOffset(page, EARLIEST);

            button(page, RESET_BUTTON).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }

        awaitCommittedOffsets(Map.of(0, INITIAL_OFFSET, 1, 0L));
    }

    @Test
    void resetToCustomOffsetOfOnePartition() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectTopic(page);
            selectPartition(page, 0);
            selectOffset(page, CUSTOM);
            resetDialog(page).locator("#custom-offset-input").fill("3");

            button(page, RESET_BUTTON).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }

        awaitCommittedOffsets(Map.of(0, 3L, 1, INITIAL_OFFSET));
    }

    @Test
    void resetToEpochTimestampBeforeTheFirstRecordMovesToTheBeginning() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EPOCH);
            dateTimeInput(page).fill(String.valueOf(beforeRecordsMs));

            button(page, RESET_BUTTON).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }

        awaitCommittedOffsets(Map.of(0, 0L, 1, 0L));
    }

    @Test
    void resetToIsoTimestampBeforeTheFirstRecordMovesToTheBeginning() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, ISO);
            dateTimeInput(page).fill(Instant.ofEpochMilli(beforeRecordsMs).toString());

            button(page, RESET_BUTTON).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }

        awaitCommittedOffsets(Map.of(0, 0L, 1, 0L));
    }

    @Test
    void deleteRemovesTheCommittedOffsetsOfTheSelectedTopic() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectTopic(page);
            selectOffset(page, DELETE);

            button(page, RESET_BUTTON).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).hasCount(0);
        }

        awaitCommittedOffsets(Map.of());
    }

    @Test
    void resetFailsWhenGroupBecameActiveAndShowsErrorInModal() {
        try (Page page = context.newPage()) {
            openResetModal(page);
            selectOffset(page, EARLIEST);

            // A member joins between opening the modal and clicking Reset: Kafka rejects the reset
            KafkaConsumer<byte[], byte[]> activeMember = joinedConsumer(emptyGroup);
            try {
                button(page, RESET_BUTTON).click();

                assertThat(errorAlert(page)).isVisible();
                assertThat(resetDialog(page)).isVisible();        // modal stays open
                assertThat(successAlert(page)).hasCount(0);
            } finally {
                activeMember.close();
            }
        }

        awaitCommittedOffsets(Map.of(0, INITIAL_OFFSET, 1, INITIAL_OFFSET));
    }
}
