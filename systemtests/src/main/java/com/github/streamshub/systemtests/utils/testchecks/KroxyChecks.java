package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class KroxyChecks {
    private static final Logger LOGGER = LogWrapper.getLogger(KroxyChecks.class);

    // Visible text of the KroxyliciousClusterLabel badge (i18n key KroxyliciousCluster.label)
    private static final String VIRTUAL_CLUSTER_BADGE_TEXT = "Virtual";
    // Blue PatternFly label rendered by the KroxyliciousClusterLabel component
    private static final String VIRTUAL_CLUSTER_BADGE_CSS = ".pf-v6-c-label.pf-m-blue";

    private KroxyChecks() {}

    /**
     * Verifies that the Kafka cluster dropdown in the UI contains all expected Kafka cluster names.
     *
     * <p>Retrieves the list of visible Kafka clusters from the page and checks that each
     * expected name is present. If any expected name is missing, an {@link AssertionError} is thrown.</p>
     *
     * @param tcc the {@link TestCaseConfig} containing the test page and context
     * @param expectedKafkaNames a list of Kafka cluster names expected to appear in the dropdown
     * @throws AssertionError if any expected Kafka cluster name is not found in the dropdown
     */
    public static void checkKafkaClusterDropdownContains(TestCaseConfig tcc, List<String> expectedKafkaNames) {
        LOGGER.info("Checking Kafka cluster dropdown contains expected clusters: {}", expectedKafkaNames);

        List<String> visibleKafkaClusters = tcc.page().locator(CssSelectors.PAGES_NAV_KAFKA_CLUSTERS_LIST_ITEMS)
            .all()
            .stream()
            .map(locator -> PwUtils.getTrimmedText(locator.allInnerTexts().toString()))
            .toList();

        LOGGER.debug("Kafka cluster dropdown currently shows {} cluster(s): {}", visibleKafkaClusters.size(), visibleKafkaClusters);

        for (String expectedKafka : expectedKafkaNames) {
            if (visibleKafkaClusters.stream().noneMatch(clusterName -> clusterName.contains(expectedKafka))) {
                throw new AssertionError("Kafka cluster not found in UI expected: " + expectedKafka +
                        " - Visible clusters: " + visibleKafkaClusters
                );
            }
        }
    }

    /**
     * Verifies the Kroxylicious "Virtual" badge in the open Kafka cluster-switcher dropdown:
     * the badge must be present on the virtual cluster entry and absent on the physical cluster entry.
     *
     * <p>Assumes the cluster-switcher dropdown is already open. Does not reload the page, as a reload
     * would close the dropdown.</p>
     *
     * @param tcc the {@link TestCaseConfig} containing the test page and context
     * @param virtualClusterName the name of the virtual (Kroxylicious) cluster expected to show the badge
     * @param physicalClusterName the name of the physical (Strimzi) cluster expected to not show the badge
     * @throws AssertionError if the badge is missing on the virtual cluster or present on the physical cluster
     */
    public static void checkVirtualClusterBadgeInDropdown(TestCaseConfig tcc, String virtualClusterName, String physicalClusterName) {
        LOGGER.info("Checking Virtual badge is shown for [{}] and not for [{}] in the cluster dropdown", virtualClusterName, physicalClusterName);

        Locator virtualBadge = tcc.page().locator(CssSelectors.PAGES_NAV_KAFKA_CLUSTERS_LIST_ITEMS)
            .filter(new Locator.FilterOptions().setHasText(virtualClusterName))
            .locator(VIRTUAL_CLUSTER_BADGE_CSS);
        PwUtils.waitForContainsText(tcc, virtualBadge, VIRTUAL_CLUSTER_BADGE_TEXT, false, false);

        Locator physicalBadge = tcc.page().locator(CssSelectors.PAGES_NAV_KAFKA_CLUSTERS_LIST_ITEMS)
            .filter(new Locator.FilterOptions().setHasText(physicalClusterName))
            .locator(VIRTUAL_CLUSTER_BADGE_CSS);
        PwUtils.waitForLocatorCount(tcc, 0, physicalBadge, false);
    }

    /**
     * Verifies the Kroxylicious "Virtual" badge is shown in the cluster overview breadcrumb header.
     *
     * <p>Call only while viewing the virtual cluster's overview page.</p>
     *
     * @param tcc the {@link TestCaseConfig} containing the test page and context
     * @throws AssertionError if the badge is not present in the overview header
     */
    public static void checkVirtualClusterBadgeOnOverviewHeader(TestCaseConfig tcc) {
        LOGGER.info("Checking Virtual badge is shown in the cluster overview header");
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CLUSTER_HEADER_VIRTUAL_LABEL, VIRTUAL_CLUSTER_BADGE_TEXT, true, false);
    }
}
