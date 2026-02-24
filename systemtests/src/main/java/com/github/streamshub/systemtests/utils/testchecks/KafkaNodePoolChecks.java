package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.NodesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.PodUtils;
import com.github.streamshub.systemtests.utils.testutils.KafkaTestUtils;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class KafkaNodePoolChecks {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaTestUtils.class);

    private KafkaNodePoolChecks() {}

    /**
     * Verifies the default Kafka node state in the UI.
     *
     * <p>Checks that:
     * <ul>
     *     <li>The overview page displays the correct number of broker nodes.</li>
     *     <li>The nodes page displays the total number of broker and controller nodes.</li>
     *     <li>Each broker and controller node row contains the correct node ID and role.</li>
     * </ul>
     *
     * @param tcc           the test case configuration
     * @param brokerIds     the list of expected broker node IDs
     * @param controllerIds the list of expected controller node IDs
     */
    public static void checkDefaultNodeState(TestCaseConfig tcc, List<Integer> brokerIds, List<Integer> controllerIds) {
        LOGGER.info("Verifying default node state (brokerIds: {}, controllerIds: {})", brokerIds.toString(), controllerIds.toString());
        checkOverviewPageKafkaBrokerNodes(tcc, brokerIds.size());
        checkNodesPageKafkaNodes(tcc, brokerIds.size() + controllerIds.size());

        for (int brokerId : brokerIds) {
            KafkaNodePoolChecks.checkKnpTableRow(tcc, brokerId + 1, brokerId, ProcessRoles.BROKER.toValue());
        }

        for (int controllerId : controllerIds) {
            KafkaNodePoolChecks.checkKnpTableRow(tcc, controllerId + 1, controllerId, ProcessRoles.CONTROLLER.toValue());
        }
    }

    public static void checkOverviewPageKafkaBrokerNodes(TestCaseConfig tcc, int brokerCount) {
        LOGGER.info("Verify kafka nodes on overview page");
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            brokerCount + "/" + brokerCount, PodUtils.getTimeoutForPodOperations(brokerCount), true, true);
    }

    public static void checkNodesPageKafkaNodes(TestCaseConfig tcc, int totalNodeCount) {
        LOGGER.info("Verify kafka nodes on nodes page");
        tcc.page().navigate(PwPageUrls.getNodesPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForLocatorCount(tcc, totalNodeCount, NodesPageSelectors.NPS_TABLE_BODY, true);
    }

    /**
     * Verifies that the Kafka Nodes table displays the expected number of rows
     * and that each row contains the correct node ID, role, and optionally
     * the Kafka Node Pool (KNP) name.
     *
     * @param tcc          the test case configuration
     * @param nodeIds      the expected list of Kafka node IDs in the table
     * @param expectedRole the expected node role (e.g. Broker or Controller)
     * @param knpName      the expected Kafka Node Pool name (nullable)
     */
    public static void checkFilterTypeResults(TestCaseConfig tcc, List<Integer> nodeIds, String expectedRole, String knpName) {
        LOGGER.info("Verify kafka node table results with nodeIds:{}, role: {} and knpName:{}", nodeIds.toString(), expectedRole, knpName);
        PwUtils.waitForLocatorCount(tcc, nodeIds.size(), NodesPageSelectors.NPS_TABLE_BODY, true);
        for (int row = 1; row < nodeIds.size(); row++) {
            checkKnpTableRow(tcc, row, nodeIds.get(row - 1), expectedRole);
            if (knpName != null) {
                PwUtils.waitForContainsText(tcc, NodesPageSelectors.getNodeTableRowItem(row, 7), knpName, false);
            }
        }
    }

    /**
     * Verifies that a specific row in the Kafka Nodes table contains
     * the expected node ID and role.
     *
     * @param tcc            the test case configuration
     * @param nthRow         the row index in the nodes table (1-based)
     * @param expectedNodeId the expected Kafka node ID
     * @param expectedRole   the expected node role (e.g. Broker or Controller)
     */
    public static void checkKnpTableRow(TestCaseConfig tcc, int nthRow, int expectedNodeId, String expectedRole) {
        LOGGER.info("Verify kafka node table contains row:{}, nodeID: {}, role: {}", nthRow, expectedNodeId, expectedRole);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.getNodeTableRowItem(nthRow, 2), String.valueOf(expectedNodeId), false, false);
        PwUtils.waitForContainsText(tcc, NodesPageSelectors.getNodeTableRowItem(nthRow, 3), expectedRole, false, false);
    }
}
