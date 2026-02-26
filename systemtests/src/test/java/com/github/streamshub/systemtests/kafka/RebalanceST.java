package com.github.streamshub.systemtests.kafka;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.NodesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
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

    @Test
    void testKafkaRebalance() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int imbalancedPartitions = 20;
        final int scaledBrokersCount = 5;
        final String rebalanceName = "testrebalance";
        final String rebalanceTopicName = "rebalance-topic";

        LOGGER.info("Create imbalance by creating topics and scaling brokers afterwards");
        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), rebalanceTopicName, 5, true, imbalancedPartitions, 1, 1);
        KafkaUtils.scaleBrokerReplicasWithWait(tcc.namespace(), tcc.kafkaName(), scaledBrokersCount);

        LOGGER.info("Create basic rebalance CR");
        KubeResourceManager.get().createOrUpdateResourceWithWait(KafkaSetup.getKafkaRebalance(tcc.namespace(), tcc.kafkaName(), rebalanceName).build());
        WaitUtils.waitForKafkaRebalanceProposalStatus(tcc.namespace(), rebalanceName, KafkaRebalanceState.PendingProposal);

        LOGGER.info("Verify rebalance proposals table");
        tcc.page().navigate(PwPageUrls.getKafkaRebalancePage(tcc, tcc.kafkaName()));
        PwUtils.waitForLocatorCount(tcc, 1, NodesPageSelectors.NPS_REBALANCE_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_REBALANCE_PROPOSAL_STATUS, KafkaRebalanceState.PendingProposal.name(), true);
        assertTrue(tcc.page().locator(NodesPageSelectors.NPS_REBALANCE_PROPOSAL_NAME).allInnerTexts().toString().contains(rebalanceName));

        LOGGER.info("Inspect rebalance proposal");
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_REBALANCE_PROPOSAL_DROPDOWN_BUTTON);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_REBALANCE_PROPOSAL_DROPDOWN_AUTO_APPROVAL_ENABLED, "false", true);
        assertTrue(tcc.page().locator(NodesPageSelectors.NPS_REBALANCE_PROPOSAL_DROPDOWN_MODE).allInnerTexts().toString().toLowerCase(Locale.ENGLISH).contains(KafkaRebalanceMode.FULL.toValue()));

        LOGGER.info("Wait for proposal to be in Ready state and check values");
        WaitUtils.waitForKafkaRebalanceProposalStatus(tcc.namespace(), rebalanceName, KafkaRebalanceState.ProposalReady);
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_REBALANCE_PROPOSAL_NAME);
        // table values
        Map<String, Object> status = ResourceUtils.getKubeResource(KafkaRebalance.class, tcc.namespace(), rebalanceName).getStatus().getOptimizationResult();
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_PROPOSAL_MODAL_DATA_TO_MOVE, status.get("dataToMoveMB").toString(), false);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_PROPOSAL_MODAL_DATA_MONITORED_PARTITIONS_PERCENTAGE, status.get("monitoredPartitionsPercentage").toString(), false);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_PROPOSAL_MODAL_DATA_NUMBER_REPLICA_MOVEMENTS, status.get("numReplicaMovements").toString(), false);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_BEFORE, status.get("onDemandBalancednessScoreBefore").toString(), false);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_AFTER, status.get("onDemandBalancednessScoreAfter").toString(), false);
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_PROPOSAL_MODAL_CLOSE_BUTTON);

        LOGGER.info("Approve rebalance using UI");
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_PROPOSAL_ACTION_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_PROPOSAL_ACTION_APPROVE_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_PROPOSAL_MODAL_CONFIRM_BUTTON);

        LOGGER.info("Verify that UI caused rebalancing");
        WaitUtils.waitForKafkaRebalanceProposalStatus(tcc.namespace(), rebalanceName, KafkaRebalanceState.Rebalancing);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.NPS_REBALANCE_PROPOSAL_STATUS, KafkaRebalanceState.Rebalancing.name(), true);
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
