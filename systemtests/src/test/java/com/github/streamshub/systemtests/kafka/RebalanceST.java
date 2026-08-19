package com.github.streamshub.systemtests.kafka;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.components.Modal;
import com.github.streamshub.systemtests.locators.pages.NodesPage;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceMode;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceState;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.REGRESSION)
public class RebalanceST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(RebalanceST.class);

    /**
     * Tests the full lifecycle of a Kafka rebalance, from proposal generation to
     * manual approval through the UI.
     *
     * <p>The test first creates an imbalance by creating 5 topics (prefix {@code rebalance-topic})
     * with 20 partitions each, replication factor 1, and then scaling the Kafka brokers up to
     * 5 replicas. A {@code KafkaRebalance} custom resource ({@code testrebalance}) with a default
     * (full) rebalance mode is then created and awaited until it reaches the
     * {@code ProposalReady} state.</p>
     *
     * <p>The test then verifies the following through the UI:</p>
     * <ul>
     *   <li>The rebalance proposals table on the Kafka Rebalance page shows a single row with
     *       "Proposal Ready" status and the correct rebalance name.</li>
     *   <li>Expanding the proposal dropdown shows auto-approval disabled ({@code false}) and the
     *       rebalance mode set to {@code full}.</li>
     *   <li>Opening the proposal's data modal displays values (data to move, monitored partitions
     *       percentage, number of replica movements, and balancedness scores before/after) matching
     *       the {@code optimizationResult} reported in the {@code KafkaRebalance} status.</li>
     *   <li>Approving the proposal through the UI (action dropdown &gt; approve &gt; confirm) transitions
     *       the {@code KafkaRebalance} resource to the {@code Rebalancing} state, which is reflected
     *       both in the resource status and in the UI.</li>
     * </ul>
     *
     * <p>This ensures that rebalance proposals are accurately surfaced in the UI and that
     * approving a rebalance from the UI correctly triggers the underlying rebalance operation.</p>
     */
    @Test
    void testKafkaRebalance() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int imbalancedPartitions = 20;
        final int scaledBrokersCount = 5;
        final String rebalanceName = "testrebalance";
        final String rebalanceTopicName = "rebalance-topic";

        LOGGER.info("Creating imbalance on Kafka '{}' by creating 5 topics (prefix '{}') with {} partitions and replication factor 1", tcc.kafkaName(), rebalanceTopicName, imbalancedPartitions);
        KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), rebalanceTopicName, 5, imbalancedPartitions, 1, 1);
        LOGGER.info("Scaling Kafka '{}' brokers to {} replicas to trigger imbalance", tcc.kafkaName(), scaledBrokersCount);
        KafkaUtils.scaleBrokerReplicasWithWait(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);

        LOGGER.info("Creating KafkaRebalance resource '{}' with default (full) mode for Kafka '{}' in namespace '{}'", rebalanceName, tcc.kafkaName(), tcc.namespace());
        KubeResourceManager.get().createOrUpdateResourceWithWait(KafkaSetup.getKafkaRebalance(tcc.namespace(), tcc.kafkaName(), rebalanceName).build());
        LOGGER.info("Waiting for KafkaRebalance '{}' to reach '{}' state", rebalanceName, KafkaRebalanceState.ProposalReady);
        WaitUtils.waitForKafkaRebalanceProposalStatus(tcc.namespace(), rebalanceName, KafkaRebalanceState.ProposalReady);

        LOGGER.info("Verifying rebalance proposals table shows a single 'Proposal Ready' entry for rebalance '{}'", rebalanceName);
        PwUtils.navigate(tcc, PwPageUrls.getKafkaRebalancePage(tcc, tcc.kafkaName()));
        PwUtils.waitForLocatorCount(1, NodesPage.rebalanceTable(tcc.page()).rows(), true);
        Locator rebalanceRow = NodesPage.rebalanceTable(tcc.page()).rows().first();
        PwUtils.waitForContainsText(NodesPage.rebalanceTable(tcc.page()).cell(rebalanceRow, "Status"), "Proposal Ready", true);
        Locator rebalanceNameButton = rebalanceRow.getByRole(AriaRole.BUTTON);
        assertTrue(rebalanceNameButton.allInnerTexts().toString().contains(rebalanceName));

        LOGGER.info("Inspecting rebalance proposal dropdown for auto-approval flag and rebalance mode");
        PwUtils.waitForLocatorAndClick(NodesPage.rebalanceExpandToggle(rebalanceRow));
        PwUtils.waitForContainsText(NodesPage.rebalanceAutoApprovalValue(tcc.page()), "false", true);
        assertTrue(NodesPage.rebalanceModeValue(tcc.page()).allInnerTexts().toString().toLowerCase(Locale.ENGLISH).contains(KafkaRebalanceMode.FULL.toValue()));

        LOGGER.info("Opening proposal data modal for rebalance '{}' to verify optimization result values", rebalanceName);
        PwUtils.waitForLocatorAndClick(rebalanceNameButton);
        // table values
        Map<String, Object> status = ResourceUtils.getKubeResource(KafkaRebalance.class, tcc.namespace(), rebalanceName).getStatus().getOptimizationResult();
        LOGGER.debug("KafkaRebalance '{}' optimizationResult from status: {}", rebalanceName, status);
        Locator modalRoot = Modal.root(tcc.page());
        PwUtils.waitForContainsText(NodesPage.rebalanceDataToMoveMb(tcc.page()), status.get("dataToMoveMB").toString(), false);
        PwUtils.waitForContainsText(NodesPage.rebalanceMonitoredPartitionsPercentage(tcc.page()), status.get("monitoredPartitionsPercentage").toString(), false);
        PwUtils.waitForContainsText(NodesPage.rebalanceNumReplicaMovements(tcc.page()), status.get("numReplicaMovements").toString(), false);
        PwUtils.waitForContainsText(NodesPage.rebalanceBalancednessBefore(tcc.page()), status.get("onDemandBalancednessScoreBefore").toString(), false);
        PwUtils.waitForContainsText(NodesPage.rebalanceBalancednessAfter(tcc.page()), status.get("onDemandBalancednessScoreAfter").toString(), false);
        PwUtils.waitForLocatorAndClick(modalRoot.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close")));

        LOGGER.info("Approving rebalance proposal '{}' via UI action dropdown", rebalanceName);
        PwUtils.waitForLocatorAndClick(NodesPage.rebalanceActionsMenuButton(rebalanceRow));
        PwUtils.waitForLocatorAndClick(NodesPage.rebalanceMenuItem(tcc.page(), "Approve"));
        PwUtils.waitForLocatorAndClick(Modal.confirmButton(tcc.page()));

        LOGGER.info("Verifying that UI approval transitioned KafkaRebalance '{}' to '{}' state", rebalanceName, KafkaRebalanceState.Rebalancing);
        WaitUtils.waitForKafkaRebalanceProposalStatus(tcc.namespace(), rebalanceName, KafkaRebalanceState.Rebalancing);
        LOGGER.debug("Confirming '{}' state is reflected in the UI proposal status for rebalance '{}'", KafkaRebalanceState.Rebalancing, rebalanceName);
        PwUtils.waitForContainsText(NodesPage.rebalanceTable(tcc.page()).cell(NodesPage.rebalanceTable(tcc.page()).rows().first(), "Status"), KafkaRebalanceState.Rebalancing.name(), true);
    }

    @AfterEach
    void testCaseTeardown() {
        getTestCaseConfig().playwright().close();
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupKafkaWithCcIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }
}
