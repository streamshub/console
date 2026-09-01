package com.github.streamshub.systemtests.utils.testchecks;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.ConsoleLocators;
import com.github.streamshub.systemtests.locators.ConsoleLocators.DataViewTableBody;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;

import io.strimzi.api.kafka.model.nodepool.ProcessRoles;

public class KafkaNodePoolChecks {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaNodePoolChecks.class);

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
        LOGGER.info("Verifying default node state (brokerIds: {}, controllerIds: {})", brokerIds, controllerIds);
        checkOverviewPageKafkaBrokerNodes(tcc, brokerIds.size());
        checkNodesPageKafkaNodes(tcc, brokerIds.size() + controllerIds.size());

        var nodesOverviewPage = ConsoleLocators.of(tcc.page()).nodesOverview();
        var tableBody = nodesOverviewPage.dataView().table().body();

        for (int brokerId : brokerIds) {
            KafkaNodePoolChecks.checkKnpTableRow(tcc, tableBody, brokerId, brokerId, ProcessRoles.BROKER.toValue());
        }

        for (int controllerId : controllerIds) {
            KafkaNodePoolChecks.checkKnpTableRow(tcc, tableBody, controllerId, controllerId, ProcessRoles.CONTROLLER.toValue());
        }
    }

    public static void checkOverviewPageKafkaBrokerNodes(TestCaseConfig tcc, int brokerCount) {
        LOGGER.info("Checking overview page shows broker node count {}/{}", brokerCount, brokerCount);
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT,
            brokerCount + "/" + brokerCount, TimeConstants.ACTION_WAIT_SHORT);
    }

    public static void checkNodesPageKafkaNodes(TestCaseConfig tcc, int totalNodeCount) {
        LOGGER.info("Checking nodes page shows total node count {}", totalNodeCount);
        PwUtils.navigate(tcc, PwPageUrls.getNodesPage(tcc, tcc.kafkaName()));
        var nodesOverviewPage = ConsoleLocators.of(tcc.page()).nodesOverview();
        PwUtils.waitForLocatorCount(tcc, totalNodeCount, nodesOverviewPage.dataView().table().body().rows().locator(), true);
    }

    /**
     * Verifies that the Kafka Nodes table displays the expected number of rows
     * and that each row contains the correct node ID, role, and optionally
     * the Kafka Node Pool (KNP) name.
     *
     * @param tcc          the test case configuration
     * @param nodeIds      the expected list of Kafka node IDs in the table
     * @param expectedRole the expected node role (e.g. Broker or Controller)
     */
    public static void checkFilterTypeResults(TestCaseConfig tcc, List<Integer> nodeIds, String expectedRole) {
        LOGGER.info("Verifying kafka node table filtered results for nodeIds {} with expected role [{}]", nodeIds, expectedRole);
        var nodesOverviewPage = ConsoleLocators.of(tcc.page()).nodesOverview();
        var tableBody = nodesOverviewPage.dataView().table().body();
        PwUtils.waitForLocatorCount(tcc, nodeIds.size(), tableBody.rows().locator(), true);

        for (int row = 0; row < nodeIds.size(); row++) {
            checkKnpTableRow(tcc, tableBody, row, nodeIds.get(row), expectedRole);
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
    private static void checkKnpTableRow(TestCaseConfig tcc, DataViewTableBody tableBody, int nthRow, int expectedNodeId, String expectedRole) {
        LOGGER.debug("Checking kafka node table row {} contains nodeId {} with role [{}]", nthRow, expectedNodeId, expectedRole);
        var row = tableBody.row(nthRow);
        PwUtils.waitForContainsText(tcc, row.cell("Node ID"), String.valueOf(expectedNodeId), false, false);
        PwUtils.waitForContainsText(tcc, row.cell("Roles"), expectedRole, false, false);
    }
}
