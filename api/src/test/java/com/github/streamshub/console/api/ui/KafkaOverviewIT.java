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
import io.strimzi.api.ResourceAnnotations;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaOverviewIT {

    static final Pattern OVERVIEW_HEADER      = ci("Cluster overview");
    static final Pattern CONNECTION_BTN       = ci("Cluster connection details");
    static final Pattern CLUSTER_CARD_TITLE   = ci("Kafka cluster details");
    static final Pattern PAUSE_BTN            = ci("Pause Reconciliation");
    static final Pattern RESUME_BTN           = ci("Resume Reconciliation");
    static final Pattern ERRORS_WARNINGS_LIST = ci("Cluster errors and warnings");
    static final Pattern NO_MESSAGES          = ci("No errors or warnings");
    static final String PAUSED_WARNING =
            "Cluster reconciliation paused. Changes to the Kafka resource will not be applied.";

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

    @BeforeEach
    void setup() {
        // Fail fast instead of waiting 30s per step
        context.setDefaultTimeout(15_000);

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        // Start every test from a clean slate. Each test applies the Kafka CR it needs.
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    @AfterEach
    void cleanup() {
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }

    private static Locator main(Page page) {
        return page.getByRole(AriaRole.MAIN);
    }

    private static Locator cardTitle(Page page, String title) {
        return page.locator(".pf-v6-c-card__title-text")
                .filter(new Locator.FilterOptions().setHasText(title))
                .first();
    }

    /** "Resume" button in the reconciliation controls (top of the page). */
    private static Locator bannerResume(Page page) {
        return page.locator("[data-ouia-component-id='reconciliation-controls-resume']");
    }

    /** Inline "Resume" button in the "Cluster errors and warnings" row. */
    private static Locator conditionResume(Page page) {
        return page.locator("[data-ouia-component-id='cluster-card-condition-resume']");
    }

    private void openOverviewViaClusterName(Page page, String clusterName) {
        page.navigate(home.toString());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(clusterName))
                .first()
                .click();
        page.waitForURL("**/kafka/*/overview");
    }

    private Kafka applyManagedKafka(boolean paused) {
        String clusterId = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();

        KafkaBuilder builder = new KafkaBuilder(
                utils.buildKafkaResource("test-kafka1", clusterId, bootstrapServers))
                .editOrNewStatus()
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus("True")
                    .endCondition()
                .endStatus();

        if (paused) {
            builder.editMetadata()
                    .addToAnnotations(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true")
                   .endMetadata()
                   .editStatus()
                    .addNewCondition()
                        .withType("ReconciliationPaused")
                        .withStatus("True")
                        .withMessage("Reconciliation is paused")
                    .endCondition()
                   .endStatus();
        }

        Kafka kafka = utils.apply(client, builder.build());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> configuredContexts.values().stream()
                        .map(KafkaContext::clusterConfig)
                        .map(KafkaClusterConfig::clusterKey)
                        .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));

        return kafka;
    }

    @Test
    void clusterNameLinkOpensOverviewPage() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page).hasURL(Pattern.compile(".*/kafka/[^/]+/overview$"));
            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(OVERVIEW_HEADER).setLevel(1))).isVisible();
        }
    }

    @Test
    void viewButtonOpensOverviewPage() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            page.navigate(home.toString());
            page.getByRole(AriaRole.ROW)
                    .filter(new Locator.FilterOptions().setHasText("test-kafka1"))
                    .locator("td[data-label='Actions']")
                    .getByRole(AriaRole.BUTTON,
                            new Locator.GetByRoleOptions().setName("View").setExact(true))
                    .click();

            page.waitForURL("**/kafka/*/overview");
            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(OVERVIEW_HEADER).setLevel(1))).isVisible();
        }
    }

    @Test
    void headerAndConnectionButtonAreDisplayed() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(OVERVIEW_HEADER).setLevel(1))).isVisible();
            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(CONNECTION_BTN))).isEnabled();
        }
    }

    @Test
    void connectionDetailsButtonOpensDrawer() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CONNECTION_BTN)).click();
            assertThat(page.locator(".pf-v6-c-drawer__panel-main")).isVisible();
        }
    }

    @Test
    void clusterCardShowsNameAndStatistics() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(CLUSTER_CARD_TITLE))).isVisible();
            assertThat(main(page).getByRole(AriaRole.HEADING,
                    new Locator.GetByRoleOptions().setName("test-kafka1").setLevel(2))).isVisible();

            assertThat(main(page).getByText("Online brokers")).isVisible();
            assertThat(main(page).getByText("Groups", new Locator.GetByTextOptions().setExact(true))).isVisible();
            assertThat(main(page).getByText("Kafka version")).isVisible();

            assertThat(main(page).getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName(Pattern.compile("^\\d+\\/\\d+$")))).isVisible();
        }
    }

    @Test
    void onlineBrokersLinkNavigatesToNodes() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            main(page).locator("a[href$='/nodes']").first().click();

            page.waitForURL("**/kafka/*/nodes/overview");
            assertThat(page).hasURL(Pattern.compile(".*/kafka/[^/]+/nodes/overview/?$"));
        }
    }

    @Test
    void consumerGroupsLinkNavigatesToGroups() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            main(page).locator("a[href$='/groups']").first().click();

            page.waitForURL("**/kafka/*/groups");
        }
    }

    @Test
    void errorsAndWarningsSectionShowsNoMessagesWhenClusterIsHealthy() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            Locator toggleButton = page.locator(".pf-v6-c-expandable-section__toggle button").first();
            if (toggleButton.count() > 0 && "false".equals(toggleButton.getAttribute("aria-expanded"))) {
                toggleButton.click();
            }

            assertThat(page.getByRole(AriaRole.LIST,
                    new Page.GetByRoleOptions().setName(ERRORS_WARNINGS_LIST))).isVisible();
            assertThat(page.getByText(NO_MESSAGES)).isVisible();
        }
    }

    @Test
    void pauseReconciliationButtonIsShownForManagedCluster() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(PAUSE_BTN))).isVisible();
        }
    }

    @Test
    void pauseReconciliationModalCanBeOpenedAndCancelled() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(PAUSE_BTN)).click();

            Locator modal = page.getByRole(AriaRole.DIALOG);
            assertThat(modal).isVisible();

            modal.getByRole(AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName(ci("cancel"))).click();

            assertThat(modal).isHidden();
        }
    }

    @Test
    void pausedReconciliationShowsWarningAndResumeAction() {
        applyManagedKafka(true);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(RESUME_BTN))).isVisible();
            assertThat(bannerResume(page)).isVisible();

            assertThat(page.getByText("ReconciliationPaused").first()).isVisible();
            assertThat(page.getByText(PAUSED_WARNING).first()).isVisible();
            assertThat(conditionResume(page)).isVisible();
        }
    }

    @Test
    void overviewCardsAreRendered() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");
            System.out.println("Card titles: "
                    + page.locator(".pf-v6-c-card__title-text").allTextContents());

            assertThat(cardTitle(page, "Topics")).isVisible();
        }
    }

    @Test
    void chartsCardsAreRendered() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(cardTitle(page, "Cluster metrics")).isVisible();
            assertThat(cardTitle(page, "Topic metrics")).isVisible();
        }
    }
}
