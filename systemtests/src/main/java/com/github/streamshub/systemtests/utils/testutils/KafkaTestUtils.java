package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.NodesPageSelectors;
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
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_FILTER_TYPE_ROLE_DROPDOWN_BUTTON);
        List<Locator> knpItems = tcc.page().locator(NodesPageSelectors.NPS_FILTER_BY_NODEPOOL_ITEMS).all();
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
        LOGGER.debug("Resetting filters after default broker validation");
        PwUtils.waitForLocatorAndClick(tcc, NodesPageSelectors.NPS_FILTER_CLEAR_ALL_FILTERS_BUTTON);
        PwUtils.waitForLocatorCount(tcc, defaultNodeCount, NodesPageSelectors.NPS_TABLE_BODY, true);
        PwUtils.reload(tcc);
    }
}
