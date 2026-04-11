package com.github.streamshub.systemtests.kafka;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.MessageStore;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.testchecks.KafkaNodesChecks;
import com.microsoft.playwright.Locator.ClickOptions;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.api.kafka.model.kafka.JbodStorageBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.PersistentClaimStorageBuilder;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static com.github.streamshub.systemtests.utils.playwright.PwUtils.getContainsOpts;
import static com.github.streamshub.systemtests.utils.playwright.PwUtils.getDefaultVisibleOpts;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.REGRESSION)
public class KafkaST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaST.class);

    /**
     * Tests the pause and resume functionality of Kafka reconciliation via the UI.
     *
     * <p>The test verifies the initial state where reconciliation is not paused, then pauses reconciliation using the UI modal.</p>
     * <p>It checks that the pause notification appears and that the Kafka resource annotation reflects the paused state.</p>
     * <p>After pausing, it attempts to scale Kafka brokers, which should not apply until reconciliation is resumed.</p>
     * <p>After that the test resumes reconciliation through the UI and verifies that the annotation is cleared and scaling proceeds as expected.</p>
     * <p>Finally, the test tries pausing reconciliation again and this time checks only for Kafka annotation. Later Kafka is resumed by another Resume button on top of the page.</p>
     *
     * <p>This ensures that reconciliation pause/resume works correctly, blocking changes when paused and applying them upon resuming.</p>
     */
    @Test
    void testPauseAndResumeReconciliation() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int scaledBrokersCount = 6;

        LOGGER.debug("Check that Kafka does not contain paused reconciliation");
        assertEquals("false", ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName())
            .getMetadata()
            .getAnnotations()
            .getOrDefault(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false"));

        LOGGER.info("Pause Kafka reconciliation using UI");
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        LOGGER.debug("Open pop-up modal for pause reconciliation");

        assertThat(tcc.page().locator(ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON))
            .containsText(MessageStore.pauseReconciliationButton());

        tcc.page().locator(ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON).click();

        LOGGER.debug("Check pop-up modal for pause reconciliation");
        assertThat(tcc.page().locator(CssSelectors.PAGES_POPUP_MODAL)).isVisible(getDefaultVisibleOpts());
        assertThat(tcc.page().locator(CssSelectors.PAGES_POPUP_MODAL_HEADER)).containsText(MessageStore.pauseReconciliation());
        assertThat(tcc.page().locator(CssSelectors.PAGES_POPUP_MODAL_BODY)).containsText(MessageStore.pauseReconciliationText());
        assertThat(tcc.page().locator(CssSelectors.PAGES_MODAL_CANCEL_BUTTON)).containsText(MessageStore.reconciliationCancel());
        assertThat(tcc.page().locator(CssSelectors.PAGES_MODAL_CONFIRM_BUTTON)).containsText(MessageStore.reconciliationConfirm());

        LOGGER.debug("Confirm pause reconciliation");
        tcc.page().locator(CssSelectors.PAGES_MODAL_CONFIRM_BUTTON).click();

        // Check aftermath
        LOGGER.info("Verify UI state after pausing Kafka reconciliation");
        assertThat(tcc.page().locator(ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION)).containsText(MessageStore.reconciliationPausedWarning());

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set to true");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        // Scale brokers (without wait) and expect nothing happens because of paused reconciliation
        LOGGER.info("Trying to fail scaling Kafka brokers count to {}", scaledBrokersCount);

        KafkaUtils.scaleBrokerReplicas(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);

        // Check replicas are changed, but actual count stayed the same
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, tcc.namespace(), KafkaNamingUtils.brokerPoolName(tcc.kafkaName()));
        assertEquals(scaledBrokersCount, knp.getSpec().getReplicas());
        // Node IDs should remain the same
        assertEquals(Constants.REGULAR_BROKER_REPLICAS, knp.getStatus().getNodeIds().size());

        // Kafka should have original Broker Pod count, but in spec there should be the new count
        LOGGER.debug("Verify UI displays old broker count of {}", Constants.REGULAR_BROKER_REPLICAS);
        WaitUtils.waitForKafkaBrokerNodePoolReplicasInSpec(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);
        WaitUtils.waitForPodsReadyAndStable(tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()), Constants.REGULAR_BROKER_REPLICAS, true);

        LOGGER.info("Resume Kafka reconciliation using UI");

        assertThat(tcc.page().locator(ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON))
            .containsText(MessageStore.resumeReconciliation());

        tcc.page().locator(ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON).click();

        assertThat(tcc.page().locator(CssSelectors.PAGES_MODAL_CANCEL_BUTTON))
            .containsText(MessageStore.reconciliationCancel());

        assertThat(tcc.page().locator(CssSelectors.PAGES_MODAL_CONFIRM_BUTTON))
            .containsText(MessageStore.reconciliationConfirm());

        LOGGER.debug("Confirm resume reconciliation");
        tcc.page().locator(CssSelectors.PAGES_MODAL_CONFIRM_BUTTON).click();

        // Reconciliation is resumed and button should display Pause
        assertThat(tcc.page().locator(ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON))
            .containsText(MessageStore.pauseReconciliationButton());

        // Check annotation
        LOGGER.debug("Verify Kafka has pause reconciliation annotation set back to false");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");
        // Resuming reconciliation should trigger scaling
        LOGGER.debug("Verify Kafka finally scaled brokers");
        WaitUtils.waitForPodsReadyAndStable(tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()), scaledBrokersCount, true);

        // Check UI displays the broker count change
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            scaledBrokersCount + "/" + scaledBrokersCount,
            TimeConstants.ACTION_WAIT_LONG);

        // Now verify resume from top notification and just check the annotation on Kafka cluster
        LOGGER.info("Pause Kafka reconciliation using UI");
        tcc.page().locator(ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON).click();
        assertThat(tcc.page().locator(CssSelectors.PAGES_POPUP_MODAL_HEADER))
            .containsText(MessageStore.pauseReconciliation());

        LOGGER.debug("Confirm pause reconciliation");
        tcc.page().locator(CssSelectors.PAGES_MODAL_CONFIRM_BUTTON).click();

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set to true");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        LOGGER.info("Resume Kafka reconciliation using button from top notification");
        tcc.page().locator(ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION_RESUME_BUTTON).click();

        assertThat(tcc.page().locator(CssSelectors.PAGES_MODAL_CANCEL_BUTTON))
            .containsText(MessageStore.reconciliationCancel());

        assertThat(tcc.page().locator(CssSelectors.PAGES_MODAL_CONFIRM_BUTTON))
            .containsText(MessageStore.reconciliationConfirm());

        LOGGER.debug("Confirm resume reconciliation");
        tcc.page().locator(CssSelectors.PAGES_MODAL_CONFIRM_BUTTON).click();

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set back to false");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");
    }

    /**
     * Tests scaling Kafka broker and controller node pools and verifies the changes via the UI.
     *
     * <p>The test starts by verifying the default number of broker and controller nodes on both the Overview and Nodes pages.</p>
     * <p>It then scales the Kafka brokers up and verifies that the updated count appears correctly in the UI, including the header, info box, and table.</p>
     * <p>Finally, the test scales brokers back to their default replica counts and confirms that the UI reflects the original state.</p>
     *
     * <p>This ensures that Kafka node scaling is correctly reflected in the UI and that related components react appropriately to changes.</p>
     */
    @Test
    void testAddRemoveKafkaNodes() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int scaledBrokersCount = 7;

        final int warningsCount = KafkaUtils.warningConditions(tcc.namespace(), tcc.kafkaName())
                .stream()
                .map(Condition::getMessage)
                .toList().size();

        LOGGER.info("Verify that default Kafka broker count is {} and controller count is {}", Constants.REGULAR_BROKER_REPLICAS, Constants.REGULAR_CONTROLLER_REPLICAS);

        LOGGER.debug("Verify default Kafka broker count on OverviewPage");
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        assertThat(tcc.page().locator(ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT))
                .containsText(Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS);

        LOGGER.debug("Verify default Kafka node count on Nodes page");
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getNodesPage(tcc, tcc.kafkaName()));

        KafkaNodesChecks.checkNodesPage(tcc.page(), Constants.REGULAR_BROKER_REPLICAS, Constants.REGULAR_CONTROLLER_REPLICAS, warningsCount);

        // Scale brokers
        LOGGER.debug("Scale Kafka brokers to {}", scaledBrokersCount);

        // Need to edit kafka bootstrap
        KafkaUtils.scaleBrokerReplicasWithWait(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);

        // Check Overview and Nodes page
        LOGGER.info("Verify newly added Kafka brokers are displayed in UI");

        LOGGER.debug("Verify new Kafka broker count on OverviewPage is {}", scaledBrokersCount);
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        assertThat(tcc.page().locator(ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT))
                .containsText(scaledBrokersCount + "/" + scaledBrokersCount,
                    getContainsOpts(true, TimeConstants.ELEMENT_VISIBILITY_TIMEOUT_MEDIUM));

        LOGGER.debug("Verify new Kafka node count on Nodes page");
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getNodesPage(tcc, tcc.kafkaName()));

        KafkaNodesChecks.checkNodesPage(tcc.page(), scaledBrokersCount, Constants.REGULAR_CONTROLLER_REPLICAS, warningsCount);

        // Scale brokers down
        // Note: It is not possible to scale controllers down due to inability to change dynamically quorums https://github.com/strimzi/strimzi-kafka-operator/issues/9429
        KafkaUtils.scaleBrokerReplicasWithWait(tcc.namespace(), tcc.kafkaName(), Constants.REGULAR_BROKER_REPLICAS);

        LOGGER.debug("Verify current Kafka broker count on OverviewPage is {}", Constants.REGULAR_BROKER_REPLICAS);
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        // Note: At this time the metrics do not update fast enough and need longer to be displayed correctly, remove once better solution is present
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS,
            TimeConstants.ACTION_WAIT_LONG);

        LOGGER.debug("Verify current Kafka broker count on NodesPage is {}", Constants.REGULAR_BROKER_REPLICAS);
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getNodesPage(tcc, tcc.kafkaName()));

        KafkaNodesChecks.checkNodesPage(tcc.page(), Constants.REGULAR_BROKER_REPLICAS, Constants.REGULAR_CONTROLLER_REPLICAS, warningsCount);
    }

    /**
     * Tests Kafka warnings display in the Console UI.
     *
     * <p>Verifies that initially no warnings are shown, then injects a faulty config to trigger a warning,
     * checks the warning appears in the UI, and finally removes the fault to confirm warnings clear.</p>
     *
     * <p>This ensures the UI properly reflects Kafka’s warning status changes after config updates.</p>
     */
    @Test
    void testDisplayKafkaWarnings() {
        final TestCaseConfig tcc = getTestCaseConfig();

        List<String> initialWarningMessages = KafkaUtils.warningConditions(tcc.namespace(), tcc.kafkaName())
                .stream()
                .map(Condition::getMessage)
                .toList();

        LOGGER.info("Verify default Kafka state - expecting {} warnings/errors", initialWarningMessages.size());
        PwUtils.navigateAndWaitForUrl(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        // Open warnings
        var warningsToggle = tcc.page().locator(ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNINGS_DROPDOWN_BUTTON);
        assertThat(warningsToggle).isVisible();

        // Check warnings list
        var messageItems = tcc.page().locator(ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS);
        Runnable initialStateAssertions;

        /*
         * Two possible initial states:
         * - the list has warnings and is expanded by default
         * - the list is empty and is collapsed by default
         */
        if (!initialWarningMessages.isEmpty()) {
            initialStateAssertions = () -> {
                messageItems.all()
                    .stream()
                    .map(PlaywrightAssertions::assertThat)
                    .forEach(LocatorAssertions::isVisible);
                assertThat(messageItems)
                    .hasCount(initialWarningMessages.size());
                assertThat(messageItems)
                    .not()
                    .containsText(MessageStore.clusterCardNoMessages());
            };
        } else {
            initialStateAssertions = () -> {
                LOGGER.debug("Verify warnings list contains only one row with `No messages` text");
                warningsToggle.click(new ClickOptions().setForce(true));
                assertThat(messageItems)
                    .isVisible();
                assertThat(messageItems)
                    .hasCount(1);
                assertThat(messageItems)
                    .containsText(MessageStore.clusterCardNoMessages());
            };
        }

        initialStateAssertions.run();

        // Make kafka fail
        LOGGER.info("Cause Kafka status to display Warning state by setting DeprecatedFields");
        KubeResourceManager.get().replaceResource(ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName()),
            kafka -> {
                KafkaClusterSpec spec = kafka.getSpec().getKafka();
                spec.setStorage(new JbodStorageBuilder()
                    .addToVolumes(new PersistentClaimStorageBuilder().withId(0).withSize("1Gi").withDeleteClaim(true).build())
                    .build());
            });

        WaitUtils.waitForKafkaCondition(tcc.namespace(), tcc.kafkaName(), k -> {
            var warningCount = KafkaUtils.warningConditions(k).size();
            return warningCount == initialWarningMessages.size() + 1;
        });

        // Expect a warning message
        List<String> warningMessages = KafkaUtils.warningConditions(tcc.namespace(), tcc.kafkaName())
                .stream()
                .map(Condition::getMessage)
                .toList();
        LOGGER.debug("Kafka currently contains warning messages: {}", warningMessages);

        // Reload using on page button
        tcc.page().locator(CssSelectors.PAGES_HEADER_RELOAD_BUTTON).click();

        // Check warnings list
        LOGGER.debug("Verify warnings list now contains {} row(s) with warning messages", warningMessages.size());

        assertThat(messageItems)
            .hasCount(warningMessages.size());
        messageItems.all()
            .stream()
            .map(PlaywrightAssertions::assertThat)
            .forEach(LocatorAssertions::isVisible);
        assertThat(messageItems)
            .containsText(warningMessages.toArray(String[]::new));

        // Remove wrong config
        LOGGER.info("Remove incorrect Kafka config to get rid off the warning from UI status");
        KubeResourceManager.get().replaceResource(ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName()),
            kafka -> {
                kafka.getSpec().getKafka().setStorage(null);
            }
        );

        WaitUtils.waitForKafkaCondition(tcc.namespace(), tcc.kafkaName(), k -> {
            var warningCount = KafkaUtils.warningConditions(k).size();
            return warningCount == initialWarningMessages.size();
        });

        LOGGER.debug("Reload page and verify the initial assertions are again true");
        tcc.page().reload(PwUtils.getDefaultReloadOpts());
        initialStateAssertions.run();
    }

    @AfterEach
    void testCaseTeardown() {
        getTestCaseConfig().playwright().close();
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }
}
