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
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceBuilder;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceMode;
import jakarta.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
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
class KafkaRebalancesIT {
    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;
    static final String NAMESPACE  = "default";

    static final String REBALANCE_ANNOTATION = "strimzi.io/rebalance";

    // Column order: expander, name, status, data to move, partitions to move,
    // leadership updates, last updated, actions
    static final int COL_NAME = 1;
    static final int COL_STATUS = 2;
    static final int COL_DATA = 3;
    static final int COL_PARTITIONS = 4;
    static final int COL_LEADERSHIP = 5;

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

    final String prefix = "ui-rebalance-" + UUID.randomUUID().toString().substring(0, 8) + "-";

    @BeforeEach
    void setup() {
        context.setDefaultTimeout(15_000);
        PlaywrightAssertions.setDefaultAssertionTimeout(15_000);

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        client.resources(KafkaRebalance.class).inAnyNamespace().delete();
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    @AfterEach
    void cleanup() {
        client.resources(KafkaRebalance.class).inAnyNamespace().delete();
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private void applyKafka(boolean cruiseControlEnabled) {
        clusterId = consoleConfig.getKafka().getCluster(KAFKA_KEY).get().getId();

        KafkaBuilder builder = new KafkaBuilder(utils.buildKafkaResource(KAFKA_NAME, clusterId, bootstrapServers));

        if (cruiseControlEnabled) {
            builder.editOrNewSpec().withNewCruiseControl().endCruiseControl().endSpec();
        }

        Kafka kafka = utils.apply(client, builder
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

    /** Creates a KafkaRebalance whose status is already in the given state (no operator runs in tests). */
    private KafkaRebalance createRebalance(String name, String state, Map<String, Object> optimizationResult) {
        return utils.apply(client, new KafkaRebalanceBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(NAMESPACE)
                    .addToLabels("strimzi.io/cluster", KAFKA_NAME)
                .endMetadata()
                .withNewSpec()
                    .withMode(KafkaRebalanceMode.FULL)
                .endSpec()
                .withNewStatus()
                    .addNewCondition()
                        .withType(state)
                        .withStatus("True")
                        .withLastTransitionTime("2026-01-01T00:00:00Z")
                    .endCondition()
                    .withOptimizationResult(optimizationResult)
                .endStatus()
                .build());
    }

    private String rebalanceAnnotation(String name) {
        KafkaRebalance kr = client.resources(KafkaRebalance.class)
                .inNamespace(NAMESPACE)
                .withName(name)
                .get();

        if (kr == null || kr.getMetadata().getAnnotations() == null) {
            return null;
        }
        return kr.getMetadata().getAnnotations().get(REBALANCE_ANNOTATION);
    }

    private String nodesUrl() {
        return home + "kafka/" + clusterId + "/nodes";
    }

    /** Opens the Rebalances tab the way a user would: nodes page -> Rebalance tab. */
    private void openRebalances(Page page) {
        page.navigate(nodesUrl());

        page.getByRole(AriaRole.TAB,
                new Page.GetByRoleOptions().setName(ci("Rebalanc"))).click();

        assertThat(page).hasURL(Pattern.compile(".*/nodes/rebalances$"));
    }

    private static Locator rowFor(Page page, String rebalanceName) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(rebalanceName));
    }

    private static Locator cell(Locator row, int column) {
        return row.locator("td").nth(column);
    }

    private static Locator actionsToggle(Locator row) {
        return row.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(ci("kebab toggle|actions")));
    }

    private static Locator menuItem(Page page, String name) {
        return page.getByRole(AriaRole.MENUITEM,
                new Page.GetByRoleOptions().setName(ci("^" + name + "$")));
    }

    private static Locator confirmationModal(Page page) {
        return page.locator("[data-ouia-component-id='rebalance-confirmation-modal']");
    }

    private static Locator modalButton(Page page, String ouiaId) {
        return confirmationModal(page).locator("[data-ouia-component-id='" + ouiaId + "']");
    }


    @Test
    void notEnabledMessageShownWithoutCruiseControl() {
        applyKafka(false);

        try (Page page = context.newPage()) {
            openRebalances(page);

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(ci("cruise control")))).isVisible();
            assertThat(page.getByRole(AriaRole.ROW)
                    .filter(new Locator.FilterOptions().setHasText(prefix))).hasCount(0);
        }
    }

    @Test
    void rebalanceIsListedWithOptimizationResult() {
        applyKafka(true);
        String name = prefix + "list";
        createRebalance(name, "ProposalReady", Map.of(
                "dataToMoveMB", 12,
                "numReplicaMovements", 5,
                "numLeaderMovements", 2));

        try (Page page = context.newPage()) {
            openRebalances(page);

            Locator row = rowFor(page, name);
            assertThat(row).hasCount(1);

            assertThat(cell(row, COL_NAME)).hasText(name);
            assertThat(cell(row, COL_STATUS)).containsText(ci("proposal\\s*ready"));
            assertThat(cell(row, COL_DATA)).containsText("12 MB");
            assertThat(cell(row, COL_PARTITIONS)).hasText("5");
            assertThat(cell(row, COL_LEADERSHIP)).hasText("2");
        }
    }

    @Test
    void multipleRebalancesAreListed() {
        applyKafka(true);
        String first = prefix + "first";
        String second = prefix + "second";
        createRebalance(first, "ProposalReady", Map.of());
        createRebalance(second, "New", Map.of());

        try (Page page = context.newPage()) {
            openRebalances(page);

            assertThat(rowFor(page, first)).hasCount(1);
            assertThat(rowFor(page, second)).hasCount(1);
        }
    }

    @Test
    void rebalanceNameLinksToDetailPage() {
        applyKafka(true);
        String name = prefix + "detail";
        createRebalance(name, "ProposalReady", Map.of());

        try (Page page = context.newPage()) {
            openRebalances(page);

            cell(rowFor(page, name), COL_NAME).getByRole(AriaRole.LINK).click();

            assertThat(page).hasURL(Pattern.compile(".*/kafka/" + clusterId + "/nodes/rebalances/[^/]+$"));
            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setLevel(1).setName(name))).isVisible();
        }
    }

    @Test
    void detailPageEnablesOnlyAllowedActionsForProposalReady() {
        applyKafka(true);
        String name = prefix + "actions";
        createRebalance(name, "ProposalReady", Map.of());

        try (Page page = context.newPage()) {
            openRebalances(page);
            cell(rowFor(page, name), COL_NAME).getByRole(AriaRole.LINK).click();

            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(ci("^approve$")))).isEnabled();
            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(ci("refresh")))).isEnabled();
            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(ci("^stop$")))).isDisabled();
        }
    }

    @Test
    void approveFromListSetsRebalanceAnnotation() {
        applyKafka(true);
        String name = prefix + "approve";
        createRebalance(name, "ProposalReady", Map.of());

        try (Page page = context.newPage()) {
            openRebalances(page);

            actionsToggle(rowFor(page, name)).click();
            menuItem(page, "approve").click();

            assertThat(confirmationModal(page)).isVisible();
            modalButton(page, "confirm").click();
            assertThat(confirmationModal(page)).isHidden();
        }

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> "approve".equals(rebalanceAnnotation(name)));
    }

    @Test
    void cancellingConfirmationDoesNotChangeRebalance() {
        applyKafka(true);
        String name = prefix + "cancel";
        createRebalance(name, "ProposalReady", Map.of());

        try (Page page = context.newPage()) {
            openRebalances(page);

            actionsToggle(rowFor(page, name)).click();
            menuItem(page, "approve").click();

            assertThat(confirmationModal(page)).isVisible();
            modalButton(page, "cancel").click();
            assertThat(confirmationModal(page)).isHidden();
        }

        // Give a (wrongly) issued patch a moment to land before asserting that nothing happened
        await().during(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> rebalanceAnnotation(name) == null);
    }

    @Test
    void approveFromDetailPageSetsRebalanceAnnotation() {
        applyKafka(true);
        String name = prefix + "detail-approve";
        createRebalance(name, "ProposalReady", Map.of());

        try (Page page = context.newPage()) {
            openRebalances(page);
            cell(rowFor(page, name), COL_NAME).getByRole(AriaRole.LINK).click();

            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(ci("^approve$"))).click();

            assertThat(confirmationModal(page)).isVisible();
            modalButton(page, "confirm").click();
        }

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> "approve".equals(rebalanceAnnotation(name)));
    }

    @Test
    void stopIsDisabledInListForProposalReady() {
        applyKafka(true);
        String name = prefix + "stop-disabled";
        createRebalance(name, "ProposalReady", Map.of());

        try (Page page = context.newPage()) {
            openRebalances(page);

            actionsToggle(rowFor(page, name)).click();

            assertThat(menuItem(page, "approve")).isEnabled();
            assertThat(menuItem(page, "stop")).isDisabled();
        }
    }
}
