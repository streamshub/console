package com.github.streamshub.systemtests.kafka;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.locators.components.Modal;
import com.github.streamshub.systemtests.locators.pages.ClusterOverviewPage;
import com.github.streamshub.systemtests.locators.pages.NodesPage;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.testchecks.KafkaNodePoolChecks;
import com.github.streamshub.systemtests.utils.testutils.KafkaTestUtils;

import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBroker;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfigurationBrokerBuilder;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.REGRESSION)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KafkaST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaST.class);
    private static final String ADDITIONAL_BRK_KNP_NAME = "additional-brk";
    private static final int ADDITIONAL_BRK_NODES = 2;

    protected TestCaseConfig tcc;

    /**
     * Tests the pause and resume functionality of Kafka reconciliation via the UI, warning display, and that
     * scaling Kafka broker nodes up is correctly reflected in the UI.
     *
     * <p>The test verifies the default counts of 3 broker nodes and 3 controller nodes on both the Overview page
     * (broker count badge) and the Nodes page (header total/working/warning badges, overview info box items, and
     * node table row count), and that reconciliation is not paused, then pauses reconciliation using the UI modal.</p>
     * <p>It checks that the pause notification appears and that the Kafka resource annotation
     * {@code strimzi.io/pause-reconciliation} is set to {@code true}.</p>
     * <p>It also verifies that pausing reconciliation triggers a warning condition that is displayed in the warnings dropdown.</p>
     * <p>After pausing, it attempts to scale Kafka brokers from the default 3 replicas up to 6. The {@code KafkaNodePool}
     * spec is updated to 6, but the actual broker node IDs and running pods remain at 3 while reconciliation stays paused.</p>
     * <p>The test then resumes reconciliation through the UI and verifies that the annotation is cleared back to
     * {@code false} and that the brokers are finally scaled to 6, with this same scale-up doubling as the "nodes added"
     * check: both the Overview page's broker count badge and the Nodes page (header, info box, and table) are verified
     * to reflect the new count, with the controller count remaining unchanged at 3.</p>
     * <p>Finally, it pauses reconciliation again and this time checks only the Kafka annotation. Reconciliation
     * is resumed via the Resume button on the top notification banner, and the annotation is verified back to {@code false}.
     *
     * <p>This ensures that reconciliation pause/resume works correctly, blocking changes when paused and applying them
     * upon resuming, that warnings are properly displayed in the UI, and that Kafka node scale-up is correctly reflected
     * in the UI.</p>
     */
    @Test
    @Order(Order.DEFAULT)
    void testPauseResumeReconciliationAndScaleKafkaNodes() {
        final int scaledBrokersCount = 6;

        LOGGER.info("Verify that default Kafka broker count is {} and controller count is {}",
            Constants.REGULAR_BROKER_REPLICAS, Constants.REGULAR_CONTROLLER_REPLICAS);

        LOGGER.debug("Verify default Kafka broker count on OverviewPage");
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(ClusterOverviewPage.brokerCount(tcc.page()),
            Constants.REGULAR_BROKER_REPLICAS + "/" + Constants.REGULAR_BROKER_REPLICAS, true);

        LOGGER.debug("Verify default Kafka node count on Nodes page");
        PwUtils.navigate(tcc, PwPageUrls.getNodesPage(tcc, tcc.kafkaName()));
        checkNodesPageNodeCounts(Constants.REGULAR_BROKER_REPLICAS);

        LOGGER.debug("Check that Kafka does not contain paused reconciliation");
        assertEquals("false", ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName())
            .getMetadata()
            .getAnnotations()
            .getOrDefault(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false"));

        LOGGER.info("Pause Kafka reconciliation using UI");
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        LOGGER.debug("Open pop-up modal for pause reconciliation");
        PwUtils.waitForContainsText(ClusterOverviewPage.pauseReconciliationButton(tcc.page()), "Pause Reconciliation", false);
        PwUtils.waitForLocatorAndClick(ClusterOverviewPage.pauseReconciliationButton(tcc.page()));

        LOGGER.debug("Check pop-up modal for pause reconciliation");
        PwUtils.waitForLocatorVisible(Modal.root(tcc.page()));
        PwUtils.waitForContainsText(Modal.heading(tcc.page()), "Pause cluster reconciliation?", false);
        PwUtils.waitForContainsText(Modal.root(tcc.page()), "While paused, updates to the cluster are ignored until reconciliation is resumed.", false);
        PwUtils.waitForContainsText(Modal.cancelButton(tcc.page()), "Cancel", false);
        PwUtils.waitForContainsText(Modal.confirmButton(tcc.page()), "Confirm", false);

        LOGGER.debug("Confirm pause reconciliation");
        PwUtils.waitForLocatorAndClick(Modal.confirmButton(tcc.page()));

        // Check aftermath
        LOGGER.info("Verify UI state after pausing Kafka reconciliation");
        PwUtils.waitForLocatorVisible(ClusterOverviewPage.reconciliationPausedBanner(tcc.page()));
        PwUtils.waitForContainsText(ClusterOverviewPage.reconciliationPausedBanner(tcc.page()),
            "Cluster reconciliation paused. Changes to the Kafka resource will not be applied.", false);

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set to true");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        // Verify warning is displayed in UI
        LOGGER.info("Verify ReconciliationPaused warning appears in UI");

        // Check the reconciliation paused warning is displayed in the warnings card
        if (ClusterOverviewPage.warningsList(tcc.page()).isHidden()) {
            LOGGER.debug("Warnings dropdown list is hidden, opening it to reveal warning messages");
            PwUtils.waitForLocatorAndClick(ClusterOverviewPage.warningsToggle(tcc.page()));
        }

        PwUtils.waitForContainsText(ClusterOverviewPage.warningMessages(tcc.page()),
            "Cluster reconciliation paused. Changes to the Kafka resource will not be applied.",
            false);

        // Scale brokers (without wait) and expect nothing happens because of paused reconciliation
        LOGGER.info("Scaling Kafka brokers from {} to {} while reconciliation is paused, expecting no effect on running pods", Constants.REGULAR_BROKER_REPLICAS, scaledBrokersCount);

        KafkaUtils.scaleBrokerReplicas(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);

        // Check replicas are changed, but actual count stayed the same
        LOGGER.debug("Verify KafkaNodePool spec replicas were updated to {} while node ID count still reports {}", scaledBrokersCount, Constants.REGULAR_BROKER_REPLICAS);
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, tcc.namespace(), KafkaNamingUtils.brokerPoolName(tcc.kafkaName()));
        assertEquals(scaledBrokersCount, knp.getSpec().getReplicas());
        // Node IDs should remain the same
        assertEquals(Constants.REGULAR_BROKER_REPLICAS, knp.getStatus().getNodeIds().size());

        // Kafka should have original Broker Pod count, but in spec there should be the new count
        LOGGER.debug("Verify KafkaNodePool spec reports {} replicas while broker pods remain stable at {}", scaledBrokersCount, Constants.REGULAR_BROKER_REPLICAS);
        WaitUtils.waitForKafkaBrokerNodePoolReplicasInSpec(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);
        WaitUtils.waitForPodsReadyAndStable(tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()), Constants.REGULAR_BROKER_REPLICAS, true);

        LOGGER.info("Resume Kafka reconciliation using UI");
        PwUtils.waitForContainsText(ClusterOverviewPage.pauseReconciliationButton(tcc.page()), "Resume Reconciliation", true);
        PwUtils.waitForLocatorAndClick(ClusterOverviewPage.pauseReconciliationButton(tcc.page()));

        PwUtils.waitForContainsText(Modal.cancelButton(tcc.page()), "Cancel", false);
        PwUtils.waitForContainsText(Modal.confirmButton(tcc.page()), "Confirm", false);

        LOGGER.debug("Confirm resume reconciliation");
        PwUtils.waitForLocatorAndClick(Modal.confirmButton(tcc.page()));

        // Reconciliation is resumed and button should display Pause
        PwUtils.waitForContainsText(ClusterOverviewPage.pauseReconciliationButton(tcc.page()), "Pause Reconciliation", true);

        // Check annotation
        LOGGER.debug("Verify Kafka has pause reconciliation annotation set back to false");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");
        // Resuming reconciliation should trigger scaling
        LOGGER.debug("Verify Kafka brokers finally scaled to {}", scaledBrokersCount);
        WaitUtils.waitForPodsReadyAndStable(tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()), scaledBrokersCount, true);

        // Check UI displays the broker count change - this scale-up also serves as the "nodes added" check,
        // reusing it instead of triggering a separate scale-up
        LOGGER.info("Verify newly added Kafka brokers are displayed in UI");

        LOGGER.debug("Verify new Kafka broker count on OverviewPage is {}", scaledBrokersCount);
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(ClusterOverviewPage.brokerCount(tcc.page()),
            scaledBrokersCount + "/" + scaledBrokersCount, TimeConstants.ACTION_WAIT_LONG);

        LOGGER.debug("Verify new Kafka node count on Nodes page");
        PwUtils.navigate(tcc, PwPageUrls.getNodesPage(tcc, tcc.kafkaName()));
        checkNodesPageNodeCounts(scaledBrokersCount);

        // Now verify resume from top notification and just check the annotation on Kafka cluster
        LOGGER.info("Pause Kafka reconciliation using UI");
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        PwUtils.waitForLocatorAndClick(ClusterOverviewPage.pauseReconciliationButton(tcc.page()));
        PwUtils.waitForContainsText(Modal.heading(tcc.page()), "Pause cluster reconciliation?", false);

        LOGGER.debug("Confirm pause reconciliation");
        PwUtils.waitForLocatorAndClick(Modal.confirmButton(tcc.page()));

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set to true");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "true");

        LOGGER.info("Resume Kafka reconciliation using button from top notification");
        PwUtils.waitForLocatorAndClick(ClusterOverviewPage.reconciliationResumeButton(tcc.page()));

        PwUtils.waitForContainsText(Modal.cancelButton(tcc.page()), "Cancel", false);
        PwUtils.waitForContainsText(Modal.confirmButton(tcc.page()), "Confirm", false);

        LOGGER.debug("Confirm resume reconciliation");
        PwUtils.waitForLocatorAndClick(Modal.confirmButton(tcc.page()));

        LOGGER.debug("Verify Kafka has pause reconciliation annotation set back to false");
        WaitUtils.waitForKafkaHasAnnotationWithValue(tcc.namespace(), tcc.kafkaName(), ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, "false");
    }

    /**
     * Checks the Nodes page header (total/working/warning badges), overview info box items, and node
     * table row count for the given broker count, with the controller count fixed at
     * {@link Constants#REGULAR_CONTROLLER_REPLICAS}.
     *
     * @param brokerCount the expected broker node count to verify against
     */
    private void checkNodesPageNodeCounts(int brokerCount) {
        LOGGER.debug("Verify Nodes page header shows total={}, working={}, warning=0",
            brokerCount + Constants.REGULAR_CONTROLLER_REPLICAS, brokerCount + Constants.REGULAR_CONTROLLER_REPLICAS);
        PwUtils.waitForContainsText(NodesPage.totalCountBadge(tcc.page()),
            Integer.toString(brokerCount + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(NodesPage.workingNodesBadge(tcc.page()),
            Integer.toString(brokerCount + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        PwUtils.waitForContainsText(NodesPage.warningNodesBadge(tcc.page()), "0", true);
        // Page infobox
        // total nodes
        PwUtils.waitForContainsText(NodesPage.totalNodesCount(tcc.page()),
            Integer.toString(brokerCount + Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with controller role
        PwUtils.waitForContainsText(NodesPage.controllerRoleCount(tcc.page()),
            Integer.toString(Constants.REGULAR_CONTROLLER_REPLICAS), true);
        // with broker role
        PwUtils.waitForContainsText(NodesPage.brokerRoleCount(tcc.page()),
            Integer.toString(brokerCount), true);
        // Node table
        LOGGER.debug("Verify Nodes page table row count equals {}", brokerCount + Constants.REGULAR_CONTROLLER_REPLICAS);
        assertEquals(brokerCount + Constants.REGULAR_CONTROLLER_REPLICAS, NodesPage.table(tcc.page()).rowCount());
    }

    /**
     * Verifies filtering of Kafka Node Pools by role in the UI.
     *
     * <p>This test validates that both the default Kafka Node Pools (3 brokers, 3 controllers) and an
     * additional broker node pool ({@code additional-brk} with 2 extra broker nodes) are correctly
     * displayed and filtered in the Console.</p>
     *
     * <p>The test performs the following steps:</p>
     * <ul>
     *     <li>Sets up the additional broker node pool for testing filters</li>
     *     <li>Retrieves broker node IDs (default + additional) and controller node IDs from the node pools</li>
     *     <li>Verifies the default node state contains all expected nodes</li>
     *     <li><b>Filters by role:</b>
     *         <ul>
     *             <li>Broker role - displays all broker nodes (default + additional)</li>
     *             <li>Controller role - displays all default controller nodes</li>
     *         </ul>
     *     </li>
     *     <li>Resets the filter between the two role checks and verifies the total node count is restored</li>
     * </ul>
     *
     * <p>This ensures that Kafka Node Pool filtering by role correctly reflects the combined state of
     * multiple node pools in the UI.</p>
     */
    @Test
    @Order(Integer.MAX_VALUE)
    void testFilterKafkaNodes() {
        // Add additional KNP for filtering
        setupAdditionalBrokerNodePool();

        LOGGER.debug("Fetching default broker and controller node IDs");
        List<Integer> defaultBrokerIds = ResourceUtils.getKubeResource(KafkaNodePool.class, tcc.namespace(), KafkaNamingUtils.brokerPoolName(tcc.kafkaName()))
            .getStatus().getNodeIds();

        List<Integer> defaultControllerIds = ResourceUtils.getKubeResource(KafkaNodePool.class, tcc.namespace(), KafkaNamingUtils.controllerPoolName(tcc.kafkaName()))
            .getStatus().getNodeIds();

        LOGGER.debug("Fetching additional broker node IDs");
        List<Integer> addedBrokerIds = ResourceUtils.getKubeResource(KafkaNodePool.class, tcc.namespace(), ADDITIONAL_BRK_KNP_NAME)
            .getStatus().getNodeIds();

        List<Integer> brokerIds = Stream.of(defaultBrokerIds, addedBrokerIds).flatMap(List::stream).toList();
        int totalNodeCount = brokerIds.size() + defaultControllerIds.size();

        LOGGER.debug("Verifying default node state with {} broker node(s) and {} controller node(s), total {}", brokerIds.size(), defaultControllerIds.size(), totalNodeCount);
        KafkaNodePoolChecks.checkDefaultNodeState(tcc, brokerIds, defaultControllerIds);

        // Test filtering by role
        LOGGER.info("Testing Kafka node pool filtering by role for {} broker node(s) and {} controller node(s)", brokerIds.size(), defaultControllerIds.size());

        LOGGER.debug("Filtering Kafka nodes by role: {}", ProcessRoles.BROKER.toValue());
        KafkaTestUtils.filterKnpByRole(tcc, ProcessRoles.BROKER.toValue());
        KafkaNodePoolChecks.checkFilterTypeResults(tcc, brokerIds, ProcessRoles.BROKER.toValue());

        LOGGER.debug("Resetting Kafka node pool filters, expecting total node count {}", totalNodeCount);
        KafkaTestUtils.resetKnpFilters(tcc, totalNodeCount);

        LOGGER.debug("Filtering Kafka nodes by role: {}", ProcessRoles.CONTROLLER.toValue());
        KafkaTestUtils.filterKnpByRole(tcc, ProcessRoles.CONTROLLER.toValue());
        KafkaNodePoolChecks.checkFilterTypeResults(tcc, defaultControllerIds, ProcessRoles.CONTROLLER.toValue());
        LOGGER.info("Kafka node pool role-based filtering by broker and controller roles verified successfully");
    }

    /**
     * Sets up an additional broker node pool for filtering tests.
     *
     * <p>Due to quorum voters, it's currently only possible to add broker role node pools.
     * Controller node pools cause crash with: Configuration can't be updated dynamically
     * because its scope is ready only: AlterConfigOp(name=controller.quorum.voters)</p>
     */
    private void setupAdditionalBrokerNodePool() {
        // Skip if already created
        if (ResourceUtils.getKubeResourceClient(KafkaNodePool.class).inNamespace(tcc.namespace()).withName(ADDITIONAL_BRK_KNP_NAME).get() != null) {
            LOGGER.debug("Additional broker node pool {} already exists, skipping setup", ADDITIONAL_BRK_KNP_NAME);
            return;
        }

        LOGGER.info("Setting up additional broker node pool: {}", ADDITIONAL_BRK_KNP_NAME);

        // Update kafka to accept new brokers
        Kafka currentKafka = ResourceUtils.getKubeResource(Kafka.class, tcc.namespace(), tcc.kafkaName());

        // Get existing broker configuration
        List<GenericKafkaListenerConfigurationBroker> existingBrokers = currentKafka.getSpec().getKafka().getListeners().stream()
            .filter(l -> l.getName().equals(Constants.SECURE_LISTENER_NAME))
            .findFirst()
            .map(l -> l.getConfiguration() != null && l.getConfiguration().getBrokers() != null
                ? l.getConfiguration().getBrokers()
                : new java.util.ArrayList<GenericKafkaListenerConfigurationBroker>())
            .orElse(new java.util.ArrayList<>());

        // Create new broker hosts for additional brokers
        List<GenericKafkaListenerConfigurationBroker> newBrokerHosts =
            KafkaUtils.getNewNodePoolNodeIds(tcc.namespace(), tcc.kafkaName(),
                Constants.REGULAR_BROKER_REPLICAS, Constants.REGULAR_BROKER_REPLICAS + ADDITIONAL_BRK_NODES)
                .stream()
                .sorted()
                .map(id -> new GenericKafkaListenerConfigurationBrokerBuilder()
                    .withBroker(id)
                    .withHost(String.join(".", "broker-" + id, Utils.hashStub(tcc.namespace()), tcc.kafkaName(), ClusterUtils.getClusterDomain()))
                    .build())
                .toList();

        // Combine existing and new brokers
        List<GenericKafkaListenerConfigurationBroker> allBrokers = new java.util.ArrayList<>(existingBrokers);
        allBrokers.addAll(newBrokerHosts);

        KubeResourceManager.get().updateResource(
            new KafkaBuilder(currentKafka)
                .editSpec()
                    .editKafka()
                        .editMatchingListener(l -> l.getName().equals(Constants.SECURE_LISTENER_NAME))
                            .editConfiguration()
                                .withBrokers(allBrokers)
                            .endConfiguration()
                        .endListener()
                    .endKafka()
                .endSpec()
                .build());

        KafkaNodePool addedBrokerPool = KafkaSetup.getDefaultBrokerNodePools(tcc.namespace(), tcc.kafkaName(), ADDITIONAL_BRK_NODES)
            .editMetadata()
                .withName(ADDITIONAL_BRK_KNP_NAME)
            .endMetadata()
            .build();

        KubeResourceManager.get().createOrUpdateResourceWithWait(addedBrokerPool);
        WaitUtils.waitForPodsReadyAndStable(tcc.namespace(), Labels.getKnpLabelSelector(tcc.kafkaName(), ADDITIONAL_BRK_KNP_NAME, ProcessRoles.BROKER), ADDITIONAL_BRK_NODES, true);
        WaitUtils.waitForKafkaReady(tcc.namespace(), tcc.kafkaName());
    }

    @BeforeEach
    void testCaseSetup() {
        tcc = Utils.getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }

    @AfterEach
    void testCaseTeardown() {
        tcc.playwright().close();
    }
}
