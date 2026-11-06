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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full-stack UI tests for the Groups page against the real "test-kafka1" cluster
 * (id "k1-id"). Nothing is mocked: the test creates a topic and two real consumer
 * groups in the test broker, then checks that the React UI shows what the backend
 * reports.
 *
 *  - stable group: a consumer that is still connected      -> state STABLE, 1 member
 *  - empty group:  a consumer that committed and then left -> state EMPTY, 0 members
 *
 * Every name has a random per-test prefix so tests never collide.
 */
@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaGroupsIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;

    static final Pattern GROUPS_TITLE   = ci("Groups");
    static final Pattern STABLE_STATE   = ci("stable");
    static final Pattern EMPTY_STATE    = ci("empty");
    static final Pattern RESET_ACTION   = ci("reset");

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
    final String prefix      = "ui-groups-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topicName   = prefix + "topic";
    final String stableGroup = prefix + "stable";
    final String emptyGroup  = prefix + "empty";

    KafkaConsumer<byte[], byte[]> stableConsumer;

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

        createTopic();
        createEmptyGroup();
        stableConsumer = joinedConsumer(stableGroup);   // stays connected until cleanup()
    }

    @AfterEach
    void cleanup() {
        if (stableConsumer != null) {
            try {
                stableConsumer.close(Duration.ofSeconds(10));
            } catch (Exception e) {
                // best effort
            }
        }

        try (Admin admin = admin()) {
            admin.deleteConsumerGroups(List.of(stableGroup, emptyGroup)).all().get(15, TimeUnit.SECONDS);
            admin.deleteTopics(List.of(topicName)).all().get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            // best effort: names are unique, so leftovers cannot affect other tests
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

    private void createTopic() throws Exception {
        try (Admin admin = admin()) {
            admin.createTopics(List.of(new NewTopic(topicName, 1, (short) 1)))
                .all().get(30, TimeUnit.SECONDS);
        }
    }

    private KafkaConsumer<byte[], byte[]> consumer(String groupId) {
        return new KafkaConsumer<>(Map.<String, Object>of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap(),
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName()));
    }

    /** Creates a consumer, joins the group and waits for a partition assignment. */
    private KafkaConsumer<byte[], byte[]> joinedConsumer(String groupId) {
        KafkaConsumer<byte[], byte[]> consumer = consumer(groupId);
        consumer.subscribe(List.of(topicName));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (consumer.assignment().isEmpty()) {
            consumer.poll(Duration.ofMillis(200));
            if (System.nanoTime() > deadline) {
                consumer.close(Duration.ZERO);
                throw new IllegalStateException("Consumer " + groupId + " was not assigned a partition in time");
            }
        }
        return consumer;
    }

    /** Joins, commits an offset (so the group survives) and leaves: the group is EMPTY. */
    private void createEmptyGroup() {
        KafkaConsumer<byte[], byte[]> consumer = joinedConsumer(emptyGroup);
        try {
            consumer.commitSync();
        } finally {
            consumer.close(Duration.ofSeconds(10));
        }
    }

    // ---------------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------------

    private String groupsUrl() {
        return home + "kafka/" + clusterId + "/groups";
    }

    private static Locator rowFor(Page page, String groupId) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(groupId));
    }

    // ---------------------------------------------------------------------
    // Page and table
    // ---------------------------------------------------------------------

    @Test
    void pageShowsTitle() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(GROUPS_TITLE))).isVisible();
        }
    }

    @Test
    void tableShowsExpectedColumns() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            for (String header : new String[] {
                "Group ID", "Type", "Protocol", "State", "lag", "Members", "Topics" }) {
                assertThat(page.getByRole(AriaRole.COLUMNHEADER,
                        new Page.GetByRoleOptions().setName(ci(header))).first()).isVisible();
            }
        }
    }

    @Test
    void tableListsBothGroups() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            assertThat(rowFor(page, stableGroup)).hasCount(1);
            assertThat(rowFor(page, emptyGroup)).hasCount(1);
        }
    }

    @Test
    void groupStatesMatchBackend() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            assertThat(rowFor(page, stableGroup)).containsText(STABLE_STATE);
            assertThat(rowFor(page, emptyGroup)).containsText(EMPTY_STATE);
        }
    }

    @Test
    void groupIdLinksToGroupPage() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            Locator link = rowFor(page, stableGroup)
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(stableGroup));
            assertThat(link).hasAttribute("href", Pattern.compile(".*/kafka/" + clusterId + "/groups/.+"));
        }
    }

    @Test
    void groupRowShowsSubscribedTopicAsLinkToTopicPage() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());

            Locator topicLink = rowFor(page, stableGroup)
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(topicName));
            assertThat(topicLink)
                    .hasAttribute("href", Pattern.compile(".*/kafka/" + clusterId + "/topics/[^/]+$"));
        }
    }

    // ---------------------------------------------------------------------
    // Filtering
    // ---------------------------------------------------------------------

    @Test
    void filteringByGroupIdShowsOnlyMatchingGroup() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());
            assertThat(rowFor(page, emptyGroup)).hasCount(1);

            // The first text input on the page is the group id filter (the default filter attribute)
            Locator filter = page.getByRole(AriaRole.TEXTBOX).first();
            filter.fill(stableGroup);
            filter.press("Enter");

            assertThat(rowFor(page, stableGroup)).hasCount(1);
            assertThat(rowFor(page, emptyGroup)).hasCount(0);
        }
    }

    // ---------------------------------------------------------------------
    // Reset offset action
    // ---------------------------------------------------------------------

    private Locator resetAction(Page page, String groupId) {
        rowFor(page, groupId)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("kebab|actions")))
                .click();
        return page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(RESET_ACTION));
    }

    @Test
    void resetOffsetIsEnabledOnlyForEmptyGroups() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());
            assertThat(rowFor(page, emptyGroup)).hasCount(1);

            assertThat(resetAction(page, emptyGroup)).isEnabled();
            page.keyboard().press("Escape");

            assertThat(resetAction(page, stableGroup)).isDisabled();
        }
    }

    @Test
    void resetOffsetOnEmptyGroupOpensModal() {
        try (Page page = context.newPage()) {
            page.navigate(groupsUrl());
            assertThat(rowFor(page, emptyGroup)).hasCount(1);

            resetAction(page, emptyGroup).click();

            assertThat(page.getByRole(AriaRole.DIALOG)).isVisible();
        }
    }
}
