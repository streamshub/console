package com.github.streamshub.systemtests.utils.testutils;

import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.ConsoleLocators;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;

public class KafkaTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaTestUtils.class);
    private KafkaTestUtils() {}

    /**
     * Applies filtering on the Kafka Nodes page by node role.
     *
     * <p>Selects the {@code Role} filter type and chooses the specified
     * role (e.g. Broker or Controller) from the filter dropdown options.</p>
     *
     * @param tcc      the test case configuration
     * @param roleName the node role to filter by
     */
    public static void filterKnpByRole(TestCaseConfig tcc, String roleName) {
        LOGGER.info("Filtering Kafka Node Pool table by role {}", roleName);
        var nodesDataView = ConsoleLocators.of(tcc.page()).nodesOverview().dataView();
        var searchToolbar = nodesDataView.toolbar();

        PwUtils.waitForLocatorAndClick(searchToolbar.filtersToggle());
        PwUtils.waitForLocatorAndClick(searchToolbar.filterItem("Role"));
        // Filter is now "Filter by role"
        PwUtils.waitForLocatorAndClick(searchToolbar.checkboxFilter().toggle());
        PwUtils.waitForLocatorAndClick(searchToolbar.checkboxFilter().checkbox(roleName));
    }

    /**
     * Resets all Kafka Node Pool (KNP) filters in the UI and verifies
     * that the default number of nodes is displayed in the node table.
     *
     * @param tcc               the test case configuration with page context
     * @param defaultNodeCount  the expected total number of nodes after clearing filters
     */
    public static void resetKnpFilters(TestCaseConfig tcc, int defaultNodeCount) {
        LOGGER.info("Clearing all Kafka Node Pool filters, expecting {} nodes to be listed", defaultNodeCount);
        var nodesDataView = ConsoleLocators.of(tcc.page()).nodesOverview().dataView();
        PwUtils.waitForLocatorAndClick(nodesDataView.toolbar().clearFilters());
        PwUtils.waitForLocatorCount(tcc, defaultNodeCount, nodesDataView.table().body().rows().locator(), false);
        PwUtils.reload(tcc);
    }
}
