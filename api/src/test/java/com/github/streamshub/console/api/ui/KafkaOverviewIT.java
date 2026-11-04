package com.github.streamshub.console.api.ui;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.TestHelper;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@QuarkusTest
@WithPlaywright
@TestProfile(TestPlainProfile.class)
class KafkaOverviewIT {

    static final Pattern OVERVIEW_HEADER      = Pattern.compile("Cluster overview");       
    static final Pattern CONNECTION_BTN       = Pattern.compile("Cluster connection details");
    static final Pattern CLUSTER_CARD_TITLE   = Pattern.compile("Kafka cluster details");   // ClusterCard.Kafka_cluster_details
    static final Pattern ONLINE_BROKERS       = Pattern.compile("Online brokers");          // ClusterCard.online_brokers
    static final Pattern CONSUMER_GROUPS      = Pattern.compile("Groups");         // ClusterCard.consumer_groups
    static final Pattern KAFKA_VERSION        = Pattern.compile("Kafka version");           // ClusterCard.kafka_version
    static final Pattern PAUSE_BTN            = Pattern.compile("Pause Reconciliation");    // reconciliation.pause_reconciliation_button
    static final Pattern RESUME_BTN           = Pattern.compile("Resume Reconciliation");                  // reconciliation.resume_reconciliation
    static final Pattern ERRORS_WARNINGS_LIST = Pattern.compile("Cluster errors and warnings");     // ClusterCard.cluster_errors_and_warnings
    static final Pattern NO_MESSAGES          = Pattern.compile("No errors or warnings");             // ClusterCard.no_messages
    static final Pattern PAUSED_WARNING       = Pattern.compile("Cluster reconciliation paused. Changes to the Kafka resource will not be applied.");  // reconciliation.reconciliation_paused_warning

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

    TestHelper utils;
    URI bootstrapServers;

    @BeforeEach
    void setup() {
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
    }

    /** Opens home, clicks the cluster name link, and waits for the overview page. */
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

        return utils.apply(client, builder.build());
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    @Test
    void clusterNameLinkOpensOverviewPage() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page).hasURL(Pattern.compile(".*/kafka/[^/]+/overview$"));
            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(OVERVIEW_HEADER).setLevel(1))).isVisible();
        }
    }

    @Test
    void viewButtonOpensOverviewPage() {
        try (Page page = context.newPage()) {
            page.navigate(home.toString());

            // TODO: adjust if the row action is a link/menu item rather than a button named "View"
            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(Pattern.compile("test-kafka1")))
                    .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(Pattern.compile("(?i)view")))
                    .first()
                    .click();

            page.waitForURL("**/kafka/*/overview");
            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(OVERVIEW_HEADER))).isVisible();
        }
    }

    // ---------------------------------------------------------------------
    // Header + connection drawer
    // ---------------------------------------------------------------------

    @Test
    void headerAndConnectionButtonAreDisplayed() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(OVERVIEW_HEADER))).isVisible();
            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(CONNECTION_BTN))).isEnabled();
        }
    }

    @Test
    void connectionDetailsButtonOpensDrawer() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CONNECTION_BTN)).click();

            // PatternFly drawer panel
            assertThat(page.locator(".pf-v6-c-drawer__panel-main")).isVisible();
        }
    }

    // ---------------------------------------------------------------------
    // Cluster card
    // ---------------------------------------------------------------------

    @Test
    void clusterCardShowsNameAndStatistics() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(CLUSTER_CARD_TITLE))).isVisible();

            // Cluster name heading inside the card
            assertThat(page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("test-kafka1").setLevel(2))).isVisible();

            // Labels under the counters
            assertThat(page.getByText(ONLINE_BROKERS)).isVisible();
            assertThat(page.getByText(CONSUMER_GROUPS)).isVisible();
            assertThat(page.getByText(KAFKA_VERSION)).isVisible();

            // Broker counter renders as "<online>/<total>"
            assertThat(page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(Pattern.compile("^\\d+/\\d+$")))).isVisible();
        }
    }

    @Test
    void onlineBrokersLinkNavigatesToNodes() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(Pattern.compile("^\\d+/\\d+$"))).click();

            page.waitForURL("**/kafka/*/nodes");
        }
    }

    @Test
    void consumerGroupsLinkNavigatesToGroups() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            // The groups counter is the link located right above the "Consumer groups" label
            page.locator("a[href$='../groups'], a[href$='/groups']").first().click();

            page.waitForURL("**/kafka/*/groups");
        }
    }

    // ---------------------------------------------------------------------
    // Errors and warnings
    // ---------------------------------------------------------------------

    @Test
    void errorsAndWarningsSectionShowsNoMessagesWhenClusterIsHealthy() {
        applyManagedKafka(false);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            Locator toggle = page.locator(".pf-v6-c-expandable-section__toggle").first();
            if (toggle.count() > 0 && "false".equals(toggle.getAttribute("aria-expanded"))) {
                toggle.click();
            }

            assertThat(page.getByRole(AriaRole.LIST,
                    new Page.GetByRoleOptions().setName(ERRORS_WARNINGS_LIST))).isVisible();
            assertThat(page.getByText(NO_MESSAGES)).isVisible();
        }
    }

    // ---------------------------------------------------------------------
    // Reconciliation
    // ---------------------------------------------------------------------

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
                    new Locator.GetByRoleOptions().setName(Pattern.compile("(?i)cancel"))).click();

            assertThat(modal).isHidden();
        }
    }

    @Test
    void pausedReconciliationShowsWarningAndResumeAction() {
        applyManagedKafka(true);

        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(Pattern.compile("(?i)resume reconciliation")))).isVisible();

            assertThat(page.getByText("ReconciliationPaused").first()).isVisible();
            assertThat(page.getByText(PAUSED_WARNING).first()).isVisible();

            assertThat(page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName(RESUME_BTN).setExact(true))).isVisible();
        }
    }

    @Test
    void overviewCardsAreRendered() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByText(Pattern.compile("(?i)topics"))).not().hasCount(0);
            assertThat(page.getByText(Pattern.compile("(?i)recently viewed topics"))).isVisible();
            assertThat(page.getByText(Pattern.compile("(?i)partitions"))).not().hasCount(0);
        }
    }

    @Test
    void chartsCardsAreRendered() {
        try (Page page = context.newPage()) {
            openOverviewViaClusterName(page, "test-kafka1");

            assertThat(page.getByText(Pattern.compile("(?i)cluster metrics"))).isVisible();
            assertThat(page.getByText(Pattern.compile("(?i)topic metrics"))).isVisible();
        }
    }
}
