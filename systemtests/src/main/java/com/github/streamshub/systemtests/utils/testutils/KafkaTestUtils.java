package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.pages.NodesPage;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

import java.util.List;

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
        PwUtils.waitForLocatorAndClick(NodesPage.roleFilterButton(tcc.page()));
        List<Locator> knpItems = NodesPage.filterMenuItems(tcc.page()).all();
        for (Locator knpItem : knpItems) {
            if (PwUtils.locatorContainsText(knpItem, roleName, false)) {
                PwUtils.waitForLocatorAndClick(knpItem);
                return;
            }
        }
        throw new AssertionError("In the role list there was no role named: " + roleName);
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
        PwUtils.waitForLocatorAndClick(NodesPage.clearAllFiltersButton(tcc.page()));
        PwUtils.waitForLocatorCount(defaultNodeCount, NodesPage.table(tcc.page()).rows(), true);
        PwUtils.reload(tcc);
    }
}
