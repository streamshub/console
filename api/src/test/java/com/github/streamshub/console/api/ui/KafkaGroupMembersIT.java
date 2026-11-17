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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full-stack tests for the group "Members" tab (GroupMembersTab), opened by clicking the group id in
 * the groups table: /kafka/{clusterId}/groups/{groupId}/members. Nothing is mocked.
 *
 * Kafka state (all names carry a random per-test prefix):
 *  - topic with 2 partitions and 10 records per partition (end offsets 10)
 *  - activeGroup: one connected consumer (explicit client id), committed offset 5 on both partitions
 *      -> 1 real member, assigned partitions 2, overall lag 2 x (10 - 5) = 10
 *  - emptyGroup: joined, committed offset 5 and left
 *      -> no members: the tab shows one placeholder member "unknown" built from the committed offsets
 */
@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaGroupMembersIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;

    static final int PARTITIONS       = 2;
    static final int RECORDS_PER_PART = 10;
    static final long COMMITTED       = 5;
    static final String TOTAL_LAG     = String.valueOf(PARTITIONS * (RECORDS_PER_PART - COMMITTED));

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

    // Unique per test instance (JUnit creates a new instance for every test method)
    final String prefix      = "ui-members-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topicName   = prefix + "topic";
    final String activeGroup = prefix + "active";
    final String emptyGroup  = prefix + "empty";
    final String clientId    = prefix + "client";

    final List<KafkaConsumer<byte[], byte[]>> openConsumers = new ArrayList<>();
    final List<String> groupsToDelete = new ArrayList<>(List.of(activeGroup, emptyGroup));

    @BeforeEach
    void setup() throws Exception {
        PlaywrightTimeouts.configure(context);
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        applyKafka();

        createTopicWithRecords();
        createEmptyGroup();
        createActiveGroup();
    }

    @AfterEach
    void cleanup() {
        for (KafkaConsumer<byte[], byte[]> consumer : openConsumers) {
            try {
                consumer.close();
            } catch (Exception e) {
                // best effort
            }
        }

        // one by one: deleting a group that does not exist fails the whole request
        for (String group : groupsToDelete) {
            try (Admin admin = admin()) {
                admin.deleteConsumerGroups(List.of(group)).all().get(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                // best effort: names are unique, so leftovers cannot affect other tests
            }
        }

        try (Admin admin = admin()) {
            admin.deleteTopics(List.of(topicName)).all().get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            // best effort
        }

        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    // ---------------------------------------------------------------------
    // Backend state
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

    private String bootstrap() {
        return config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class);
    }

    private Admin admin() {
        return Admin.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap()));
    }

    private void createTopicWithRecords() throws Exception {
        try (Admin admin = admin()) {
            admin.createTopics(List.of(new NewTopic(topicName, PARTITIONS, (short) 1)))
                .all().get(30, TimeUnit.SECONDS);
        }

        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(Map.<String, Object>of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName()))) {
            for (int partition = 0; partition < PARTITIONS; partition++) {
                for (int i = 0; i < RECORDS_PER_PART; i++) {
                    producer.send(new ProducerRecord<>(topicName, partition, null, ("v" + i).getBytes()));
                }
            }
            producer.flush();
        }
    }

    private KafkaConsumer<byte[], byte[]> consumer(String groupId, String clientIdValue) {
        return new KafkaConsumer<>(Map.<String, Object>of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap(),
                CommonClientConfigs.CLIENT_ID_CONFIG, clientIdValue,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName()));
    }

    /** Creates a consumer, joins the group and waits for a partition assignment. */
    private KafkaConsumer<byte[], byte[]> joinedConsumer(String groupId, String clientIdValue) {
        KafkaConsumer<byte[], byte[]> consumer = consumer(groupId, clientIdValue);
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

    /** Commits {@link #COMMITTED} on every partition currently assigned to the consumer. */
    private static void commitOnAssignedPartitions(KafkaConsumer<byte[], byte[]> consumer) {
        Map<TopicPartition, OffsetAndMetadata> commits = new HashMap<>();
        consumer.assignment().forEach(tp -> commits.put(tp, new OffsetAndMetadata(COMMITTED)));
        consumer.commitSync(commits);
    }

    /** Joins, commits and leaves: the group is EMPTY but keeps its committed offsets. */
    private void createEmptyGroup() {
        KafkaConsumer<byte[], byte[]> consumer = joinedConsumer(emptyGroup, prefix + "gone");
        try {
            commitOnAssignedPartitions(consumer);
        } finally {
            consumer.close();
        }
    }

    /** One consumer with a known client id that stays connected until cleanup(). */
    private void createActiveGroup() {
        KafkaConsumer<byte[], byte[]> consumer = joinedConsumer(activeGroup, clientId);
        openConsumers.add(consumer);
        commitOnAssignedPartitions(consumer);
    }

    // ---------------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------------

    private String groupsUrl() {
        return home + "kafka/" + clusterId + "/groups";
    }

    private static Locator rowFor(Page page, String groupId) {
        return page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(groupId));
    }

    /** Opens the members tab the way a user does: click the group id in the groups table. */
    private void openMembersViaGroupsTable(Page page, String groupId) {
        page.navigate(groupsUrl());
        assertThat(rowFor(page, groupId)).hasCount(1);

        rowFor(page, groupId)
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(groupId))
                .click();

        assertThat(page).hasURL(Pattern.compile(".*/kafka/" + clusterId + "/groups/[^/]+/members/?$"));
    }

    /** The members table (its aria-label is a literal in GroupMembersTab). */
    private static Locator membersTable(Page page) {
        return page.getByRole(AriaRole.GRID, new Page.GetByRoleOptions().setName("Group members"))
                .or(page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName("Group members")))
                .first();
    }

    /** All cells of a column, addressed by the data-label of GroupMembersTab. */
    private static Locator cells(Page page, String dataLabel) {
        return membersTable(page).locator("td[data-label='" + dataLabel + "']");
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    @Test
    void clickingGroupIdInTableOpensMembersPage() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, activeGroup);

            assertThat(membersTable(page)).isVisible();
        }
    }

    @Test
    void membersPageCanBeOpenedByUrl() {
        // The {groupId} route segment is the group's resource id, which is not necessarily the Kafka
        // group name: take the URL the UI generates when the group is clicked in the table.
        String membersUrl;
        try (Page first = context.newPage()) {
            openMembersViaGroupsTable(first, activeGroup);
            membersUrl = first.url();
        }

        // Fresh page: a real deep link, not in-app navigation
        try (Page page = context.newPage()) {
            page.navigate(membersUrl);

            assertThat(page).hasURL(Pattern.compile(".*/kafka/" + clusterId + "/groups/[^/]+/members/?$"));
            assertThat(membersTable(page)).isVisible();
        }
    }

    // ---------------------------------------------------------------------
    // Table
    // ---------------------------------------------------------------------

    @Test
    void tableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, activeGroup);

            for (String header : new String[] {"Member ID", "Client ID", "Overall lag", "Assigned partitions" }) {
                assertThat(membersTable(page).getByRole(AriaRole.COLUMNHEADER,
                        new Locator.GetByRoleOptions().setName(ci(header))).first()).isVisible();
            }
        }
    }

    @Test
    void activeGroupShowsTheConnectedMember() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, activeGroup);

            assertThat(cells(page, "Member ID")).hasCount(1);
            assertThat(cells(page, "Member ID")).containsText(clientId);   // Kafka member id = client id + "-" + uuid
            assertThat(cells(page, "Member ID")).not().containsText("unknown");
            assertThat(cells(page, "Client ID")).hasText(clientId);
        }
    }

    @Test
    void activeMemberShowsOverallLagAndAssignedPartitions() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, activeGroup);

            assertThat(cells(page, "Assigned partitions")).hasText(String.valueOf(PARTITIONS));
            assertThat(cells(page, "Overall lag")).hasText(TOTAL_LAG);
        }
    }

    @Test
    void emptyGroupShowsUnknownMemberBuiltFromCommittedOffsets() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, emptyGroup);

            assertThat(cells(page, "Member ID")).hasCount(1);
            assertThat(cells(page, "Member ID")).hasText("unknown");
            assertThat(cells(page, "Client ID")).hasText("unknown");
            assertThat(cells(page, "Assigned partitions")).hasText(String.valueOf(PARTITIONS));
            assertThat(cells(page, "Overall lag")).hasText(TOTAL_LAG);
        }
    }

    @Test
    void twoMembersShareThePartitionsAndEachShowsItsOwnLag() {
        String sharedGroup = prefix + "shared";
        groupsToDelete.add(sharedGroup);

        KafkaConsumer<byte[], byte[]> first = consumer(sharedGroup, prefix + "client-a");
        KafkaConsumer<byte[], byte[]> second = consumer(sharedGroup, prefix + "client-b");
        openConsumers.add(first);
        openConsumers.add(second);
        first.subscribe(List.of(topicName));
        second.subscribe(List.of(topicName));

        // Both consumers must keep polling for the rebalance to complete: one partition each
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
        while (first.assignment().size() != 1 || second.assignment().size() != 1) {
            first.poll(Duration.ofMillis(100));
            second.poll(Duration.ofMillis(100));
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("Consumers did not split the partitions in time");
            }
        }
        commitOnAssignedPartitions(first);
        commitOnAssignedPartitions(second);

        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, sharedGroup);

            assertThat(cells(page, "Member ID")).hasCount(2);
            // the order of the members is not guaranteed: check that each client id has exactly one row
            assertThat(cells(page, "Client ID")
                    .filter(new Locator.FilterOptions().setHasText(prefix + "client-a"))).hasCount(1);
            assertThat(cells(page, "Client ID")
                    .filter(new Locator.FilterOptions().setHasText(prefix + "client-b"))).hasCount(1);
            assertThat(cells(page, "Assigned partitions")).hasText(new String[] {"1", "1" });
            assertThat(cells(page, "Overall lag")).hasText(new String[] {
                String.valueOf(RECORDS_PER_PART - COMMITTED), String.valueOf(RECORDS_PER_PART - COMMITTED) });
        }
    }

    // ---------------------------------------------------------------------
    // Expandable rows (LagTable)
    // ---------------------------------------------------------------------

    @Test
    void rowIsCollapsedInitiallyAndExpandsToShowTheLagTable() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, activeGroup);
            Locator toggle = membersTable(page).locator("button[aria-expanded]").first();

            assertThat(toggle).hasAttribute("aria-expanded", "false");
            assertThat(membersTable(page).getByText(topicName).first()).isHidden();

            toggle.click();

            assertThat(toggle).hasAttribute("aria-expanded", "true");
            assertThat(membersTable(page).getByText(topicName).first()).isVisible();
        }
    }

    @Test
    void collapsingTheRowHidesTheLagTableAgain() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, activeGroup);
            Locator toggle = membersTable(page).locator("button[aria-expanded]").first();

            toggle.click();
            assertThat(membersTable(page).getByText(topicName).first()).isVisible();

            toggle.click();

            assertThat(toggle).hasAttribute("aria-expanded", "false");
            assertThat(membersTable(page).getByText(topicName).first()).isHidden();
        }
    }

    @Test
    void emptyGroupPlaceholderMemberAlsoExpandsToItsOffsets() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, emptyGroup);

            membersTable(page).locator("button[aria-expanded]").first().click();

            assertThat(membersTable(page).getByText(topicName).first()).isVisible();
        }
    }

    // ---------------------------------------------------------------------
    // Help tooltips
    // ---------------------------------------------------------------------

    @Test
    void helpIconsExplainClientIdAndOverallLag() {
        try (Page page = context.newPage()) {
            openMembersViaGroupsTable(page, activeGroup);

            membersTable(page).getByRole(AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName(ci("Client ID")))
                    .first().locator("svg").hover();
            assertThat(page.getByRole(AriaRole.TOOLTIP)).containsText("user-specified string");

            membersTable(page).getByRole(AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName(ci("Overall lag")))
                    .first().locator("svg").hover();
            assertThat(page.getByRole(AriaRole.TOOLTIP)).containsText("cumulative lag");
        }
    }
}
