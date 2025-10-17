package com.github.streamshub.systemtests.kafka;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.MessageStore;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.enums.ConditionStatus;
import com.github.streamshub.systemtests.enums.ResourceStatus;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.NodesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.PodUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.kafka.JbodStorageBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.PersistentClaimStorageBuilder;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        LOGGER.debug("Open pop-up modal for pause reconciliation");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, MessageStore.pauseReconciliationButton(), false);
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON);

        LOGGER.debug("Check pop-up modal for pause reconciliation");
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.PAGES_POPUP_MODAL);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_POPUP_MODAL_HEADER, MessageStore.pauseReconciliation(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_POPUP_MODAL_BODY, MessageStore.pauseReconciliationText(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CANCEL_BUTTON, MessageStore.reconciliationCancel(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON, MessageStore.reconciliationConfirm(), false);

        LOGGER.debug("Confirm pause reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

        // Check aftermath
        LOGGER.info("Verify UI state after pausing Kafka reconciliation");
        PwUtils.waitForLocatorVisible(tcc, ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION, MessageStore.reconciliationPausedWarning(), false);

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
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, MessageStore.resumeReconciliation(), true);
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON);

        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CANCEL_BUTTON, MessageStore.reconciliationCancel(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON, MessageStore.reconciliationConfirm(), false);

        LOGGER.debug("Confirm resume reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

        // Reconciliation is resumed and button should display Pause
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON, MessageStore.pauseReconciliationButton(), true);

        // Check annotation
        LOGGER.debug("Verify Kafka has pause reconciliation annotation set back to false");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");
        // Resuming reconciliation should trigger scaling
        LOGGER.debug("Verify Kafka finally scaled brokers");
        WaitUtils.waitForPodsReadyAndStable(tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()), scaledBrokersCount, true);

        // Check UI displays the broker count change
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            scaledBrokersCount + "/" + scaledBrokersCount, PodUtils.getTimeoutForPodOperations(scaledBrokersCount - Constants.REGULAR_BROKER_REPLICAS), true);

        // Now verify resume from top notification and just check the annotation on Kafka cluster
        LOGGER.info("Pause Kafka reconciliation using UI");
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_POPUP_MODAL_HEADER, MessageStore.pauseReconciliation(), false);

        LOGGER.debug("Confirm pause reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set to true");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        LOGGER.info("Resume Kafka reconciliation using button from top notification");
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_RECONCILIATION_PAUSED_NOTIFICATION_RESUME_BUTTON);

        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CANCEL_BUTTON, MessageStore.reconciliationCancel(), false);
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON, MessageStore.reconciliationConfirm(), false);

        LOGGER.debug("Confirm resume reconciliation");
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_MODAL_CONFIRM_BUTTON);

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

        LOGGER.info("Verify that default Kafka broker count is {} and controller count is {}", Constants.REGULAR_BROKER_REPLICAS, Constants.REGULAR_CONTROLLER_REPLICAS);

        LOGGER.debug("Verify default Kafka broker count on OverviewPage");
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS, true);

        LOGGER.debug("Verify default Kafka node count on Nodes page");
        tcc.page().navigate(PwPageUrls.getNodesPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        // Header
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_TOTAL_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT, "0", true);
        // Page infobox
        // total nodes
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(1).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with controller role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(2).build(), Integer.toString(Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with broker role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(3).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS), true);
        // Node table
        assertEquals(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS, tcc.page().locator(NodesPageSelectors.NPS_TABLE_BODY).all().size());

        // Scale brokers
        LOGGER.debug("Scale Kafka brokers to {}", scaledBrokersCount);

        // Need to edit kafka bootstrap
        KafkaUtils.scaleBrokerReplicasWithWait(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);

        // Check Overview and Nodes page
        LOGGER.info("Verify newly added Kafka brokers are displayed in UI");

        LOGGER.debug("Verify new Kafka broker count on OverviewPage is {}", scaledBrokersCount);
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            scaledBrokersCount + "/" + scaledBrokersCount, PodUtils.getTimeoutForPodOperations(scaledBrokersCount - Constants.REGULAR_BROKER_REPLICAS), true);

        LOGGER.debug("Verify new Kafka node count on Nodes page");
        tcc.page().navigate(PwPageUrls.getNodesPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        // Header
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_TOTAL_COUNT, Integer.toString(scaledBrokersCount + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT, Integer.toString(scaledBrokersCount + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT, "0", true);
        // Page infobox
        // total nodes
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(1).build(), Integer.toString(scaledBrokersCount + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with controller role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(2).build(), Integer.toString(Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with broker role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(3).build(), Integer.toString(scaledBrokersCount), true);
        // Node table
        assertEquals(scaledBrokersCount + Constants.REGULAR_CONTROLLER_REPLICAS, tcc.page().locator(NodesPageSelectors.NPS_TABLE_BODY).all().size());

        // Scale brokers down
        // Note: It is not possible to scale controllers down due to inability to change dynamically quorums https://github.com/strimzi/strimzi-kafka-operator/issues/9429
        KafkaUtils.scaleBrokerReplicasWithWait(tcc.namespace(), tcc.kafkaName(), Constants.REGULAR_BROKER_REPLICAS);

        LOGGER.debug("Verify current Kafka broker count on OverviewPage is {}", Constants.REGULAR_BROKER_REPLICAS);
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        // broker scaling needs longer to be displayed
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS, PodUtils.getTimeoutForPodOperations(Constants.REGULAR_BROKER_REPLICAS), true);

        LOGGER.debug("Verify current Kafka broker count on NodesPage is {}", Constants.REGULAR_BROKER_REPLICAS);
        tcc.page().navigate(PwPageUrls.getNodesPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        // Header
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_TOTAL_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT, Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT, "0", true);
        // Page infobox
        // total nodes
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(1).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with controller role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(2).build(), Integer.toString(Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with broker role
        PwUtils.waitForContainsText(tcc, new CssBuilder(NodesPageSelectors.NPS_OVERVIEW_NODE_ITEMS).nth(3).build(), Integer.toString(Constants.REGULAR_BROKER_REPLICAS), true);
        // Node table
        assertEquals(Constants.REGULAR_BROKER_REPLICAS + Constants.REGULAR_CONTROLLER_REPLICAS, tcc.page().locator(NodesPageSelectors.NPS_TABLE_BODY).all().size());
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

        LOGGER.info("Verify default Kafka state - expecting no warnings or errors");
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        // Open warnings
        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNINGS_DROPDOWN_BUTTON);

        // Check warnings list
        LOGGER.debug("Verify warnings list contains only one row with `No messages` text");
        PwUtils.waitForLocatorVisible(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS);
        PwUtils.waitForLocatorCount(tcc, 1,  ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS).nth(1).build(), MessageStore.clusterCardNoMessages(), true);

        // Make kafka fail
        LOGGER.info("Cause Kafka status to display Warning state by setting DeprecatedFields");
        KubeResourceManager.get().replaceResource(ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName()),
            kafka -> {
                KafkaClusterSpec spec = kafka.getSpec().getKafka();
                spec.setStorage(new JbodStorageBuilder()
                    .addToVolumes(new PersistentClaimStorageBuilder().withId(0).withSize("1Gi").withDeleteClaim(true).build())
                    .build());
            });

        WaitUtils.waitForKafkaHasWarningStatus(tcc.namespace(), tcc.kafkaName());

        // Expect a warning message
        String warningMessage = ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName()).getStatus().getConditions().stream()
            .filter(condition -> condition.getType().equals(ResourceStatus.WARNING.toString()) && condition.getStatus().equals(ConditionStatus.TRUE.toString()))
            .toList().get(0).getMessage();
        LOGGER.debug("Kafka currently contains warning message: [{}]", warningMessage);

        // Reload using on page button
        PwUtils.waitForLocatorAndClick(tcc, CssSelectors.PAGES_HEADER_RELOAD_BUTTON);

        // Check warnings list
        LOGGER.debug("Verify warnings list now contains one row with warning message");
        PwUtils.waitForLocatorVisible(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS);
        PwUtils.waitForLocatorCount(tcc, 1,  ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS, true);

        String kafkaWarningsString = tcc.page()
            .locator(new CssBuilder(ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS).nth(1).build())
            .allInnerTexts()
            .toString();

        assertTrue(PwUtils.getTrimmedText(kafkaWarningsString).contains(warningMessage));

        // Remove wrong config
        LOGGER.info("Remove incorrect Kafka config to get rid off the warning from UI status");
        KubeResourceManager.get().replaceResource(ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName()),
            kafka -> {
                kafka.getSpec().getKafka().setStorage(null);
            }
        );

        WaitUtils.waitForKafkaHasNoWarningStatus(tcc.namespace(), tcc.kafkaName());

        LOGGER.debug("Reload page and verify that there is `No messages` in the warnings list again");
        tcc.page().reload(PwUtils.getDefaultReloadOpts());

        PwUtils.waitForLocatorAndClick(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNINGS_DROPDOWN_BUTTON);

        PwUtils.waitForLocatorVisible(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS);
        PwUtils.waitForLocatorCount(tcc, 1,  ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS).nth(1).build(), MessageStore.clusterCardNoMessages(), true);
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
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
