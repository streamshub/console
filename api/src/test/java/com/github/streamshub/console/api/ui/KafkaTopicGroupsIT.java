package com.github.streamshub.console.api.ui;

import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils.ConsumerResponse;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils.ConsumerType;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.Base64;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaTopicGroupsIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;

    static final int PARTITIONS = 2;

    static final List<Pattern> HEADERS = List.of(
            ci("Group ID"), ci("Type"), ci("Protocol"), ci("State"),
            ci("Overall lag"), ci("Members"), ci("Topics"));

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
    ConsumerUtils consumerUtils;
    URI bootstrapServers;
    String clusterId;

    final List<ConsumerResponse<?>> consumers = new ArrayList<>();

    final String prefix = "ui-groups-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topic      = prefix + "topic";
    final String otherTopic = prefix + "other";

    @BeforeEach
    void setup() {
        PlaywrightTimeouts.configure(context);


        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        topicUtils = new TopicHelper(bootstrapServers, config);
        consumerUtils = new ConsumerUtils(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        applyKafka();

        topicUtils.createTopics(List.of(topic), PARTITIONS);
    }

    @AfterEach
    void cleanup() {
        consumers.forEach(ConsumerResponse::close);
        consumers.clear();
        consumerUtils.deleteGroups();
        topicUtils.deleteTopics(topic, otherTopic);
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private void applyKafka() {
        clusterId = consoleConfig.getKafka().getCluster(KAFKA_KEY).get().getId();

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

    /** Starts a consumer on an already-created topic and keeps it open until cleanup. */
    private void startConsumer(ConsumerType type, String groupId, String topicName, int messages, boolean autoClose) {
        ConsumerResponse<AutoCloseable> response = consumerUtils.request(type)
                .groupId(groupId)
                .topic(topicName, PARTITIONS)
                .createTopic(false)
                .messagesPerTopic(messages)
                .autoClose(autoClose)
                .<AutoCloseable>consume();
        consumers.add(response);
    }

    private String topicsUrl() {
        return home + "kafka/" + clusterId + "/topics";
    }

    private static Locator rowFor(Page page, String text) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(text));
    }

    /** Opens the Groups tab the way a user would: topics list -> topic name link -> Groups tab. */
    private void openGroups(Page page, String topicName) {
        page.navigate(topicsUrl());

        rowFor(page, topicName).getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName(topicName)).click();

        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setLevel(1).setName(topicName))).isVisible();

        page.getByRole(AriaRole.TAB,
                new Page.GetByRoleOptions().setName(ci("Groups"))).click();

        assertThat(page).hasURL(Pattern.compile(".*/topics/[^/]+/groups$"));
        assertThat(page.getByRole(AriaRole.TAB,
                new Page.GetByRoleOptions().setName(ci("Groups"))))
                .hasAttribute("aria-selected", "true");
    }

    private static Locator groupsTable(Page page) {
        return page.locator("table[aria-label='Groups table']");
    }

    private static Locator groupRows(Page page) {
        return groupsTable(page).locator("tbody tr");
    }

    private static Locator groupRow(Page page, String groupId) {
        return groupRows(page).filter(new Locator.FilterOptions().setHasText(groupId));
    }

    /** Column order: group id, type, protocol, state, overall lag, members, topics. */
    private static Locator cell(Locator row, int column) {
        return row.locator("td").nth(column);
    }


    @Test
    void emptyStateShownWhenNoGroupConsumesTopic() {
        try (Page page = context.newPage()) {
            openGroups(page, topic);

            assertThat(page.getByText("No groups are currently consuming from this topic.")).isVisible();
            assertThat(groupsTable(page)).hasCount(0);
        }
    }

    @Test
    void tableShowsExpectedColumns() {
        startConsumer(ConsumerType.CLASSIC, prefix + "columns", topic, 2, false);

        try (Page page = context.newPage()) {
            openGroups(page, topic);

            Locator headers = groupsTable(page).locator("thead th");
            assertThat(headers).hasCount(HEADERS.size());

            for (int i = 0; i < HEADERS.size(); i++) {
                assertThat(headers.nth(i)).containsText(HEADERS.get(i));
            }
        }
    }

    @Test
    void activeClassicGroupShowsDetails() {
        String groupId = prefix + "active";
        startConsumer(ConsumerType.CLASSIC, groupId, topic, 4, false);

        try (Page page = context.newPage()) {
            openGroups(page, topic);

            Locator row = groupRow(page, groupId);
            assertThat(row).hasCount(1);

            assertThat(cell(row, 0)).hasText(groupId);
            assertThat(cell(row, 1)).containsText(ci("classic"));
            assertThat(cell(row, 2)).containsText(ci("consumer"));
            assertThat(cell(row, 3)).containsText("Stable");
            assertThat(cell(row, 4)).hasText("0");   // every produced message was consumed
            assertThat(cell(row, 5)).hasText("1");   // one member
            assertThat(cell(row, 6).getByText(topic)).isVisible();
        }
    }

    @Test
    void overallLagReflectsUnconsumedMessages() {
        String groupId = prefix + "lag";
        startConsumer(ConsumerType.CLASSIC, groupId, topic, 2, false);

        // The open consumer no longer polls, so these records stay unconsumed
        for (int i = 0; i < 3; i++) {
            topicUtils.produceRecord(topic, 0, null, Map.of(), "k" + i, "extra-" + i);
        }

        try (Page page = context.newPage()) {
            openGroups(page, topic);

            assertThat(cell(groupRow(page, groupId), 4)).hasText("3");
        }
    }

    @Test
    void groupWithoutMembersIsEmpty() {
        String groupId = prefix + "empty";
        // autoClose=true: the consumer leaves the group after reading, offsets stay committed
        startConsumer(ConsumerType.CLASSIC, groupId, topic, 2, true);

        try (Page page = context.newPage()) {
            openGroups(page, topic);

            Locator row = groupRow(page, groupId);
            assertThat(cell(row, 3)).containsText("Empty");
            assertThat(cell(row, 5)).hasText("0");
        }
    }

    @Test
    void onlyGroupsOfThisTopicAreListed() {
        topicUtils.createTopics(List.of(otherTopic), PARTITIONS);

        String ownGroup = prefix + "own";
        String foreignGroup = prefix + "foreign";

        startConsumer(ConsumerType.CLASSIC, ownGroup, topic, 2, false);
        startConsumer(ConsumerType.CLASSIC, foreignGroup, otherTopic, 2, false);

        try (Page page = context.newPage()) {
            openGroups(page, topic);

            assertThat(groupRows(page)).hasCount(1);
            assertThat(groupRow(page, ownGroup)).hasCount(1);
            assertThat(groupRow(page, foreignGroup)).hasCount(0);
        }
    }

    @Test
    void groupIdLinksToGroupDetailsPage() {
        String groupId = prefix + "link";
        startConsumer(ConsumerType.CLASSIC, groupId, topic, 2, false);

        String encodedGroupId = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(groupId.getBytes(StandardCharsets.UTF_8));

        try (Page page = context.newPage()) {
            openGroups(page, topic);

            cell(groupRow(page, groupId), 0).getByRole(AriaRole.LINK).click();

            assertThat(page).hasURL(
                home + "kafka/" + clusterId + "/groups/" + encodedGroupId + "/members");
        }
    }

    @Test
    void stateColumnTooltipShowsHelpText() {
        startConsumer(ConsumerType.CLASSIC, prefix + "tooltip", topic, 2, false);

        try (Page page = context.newPage()) {
            openGroups(page, topic);

            groupsTable(page).locator("thead th").nth(3).locator("svg").hover();

            assertThat(page.getByText("Reflects the current operational state of the group.")).isVisible();
        }
    }
}
