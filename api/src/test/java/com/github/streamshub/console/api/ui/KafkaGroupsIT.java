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
import org.apache.kafka.clients.consumer.Consumer;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    ConsumerUtils consumerUtils;
    URI bootstrapServers;
    String clusterId;

    // Unique per test instance (JUnit creates a new instance for every test method)
    final String prefix      = "ui-groups-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    final String topicName   = prefix + "topic";
    final String stableGroup = prefix + "stable";
    final String emptyGroup  = prefix + "empty";

    ConsumerResponse<Consumer<String, String>> stableConsumer;

    @BeforeEach
    void setup() {
        context.setDefaultTimeout(15_000);
        PlaywrightAssertions.setDefaultAssertionTimeout(15_000);

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        consumerUtils = new ConsumerUtils(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        applyKafka();

        stableConsumer = consumerUtils.request(ConsumerType.CLASSIC)
                .groupId(stableGroup)
                .topic(topicName)
                .messagesPerTopic(1)
                .consume();

        consumerUtils.request(ConsumerType.CLASSIC)
                .groupId(emptyGroup)
                .topic(topicName)
                .createTopic(false)
                .messagesPerTopic(1)
                .autoClose(true)
                .consume();
    }

    @AfterEach
    void cleanup() {
        if (stableConsumer != null) {
            stableConsumer.close(); // must close first: only empty groups can be deleted
        }

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

    private String groupsUrl() {
        return home + "kafka/" + clusterId + "/groups";
    }

    private static Locator rowFor(Page page, String groupId) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(groupId));
    }

    private Locator resetAction(Page page, String groupId) {
        rowFor(page, groupId)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ci("kebab|actions")))
                .click();
        return page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(RESET_ACTION));
    }

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
