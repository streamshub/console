package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;

import java.util.List;

public class KroxyChecks {
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

        List<String> visibleKafkaClusters = tcc.page().locator(CssSelectors.PAGES_NAV_KAFKA_CLUSTERS_LIST_ITEMS)
            .all()
            .stream()
            .map(locator -> PwUtils.getTrimmedText(locator.allInnerTexts().toString()))
            .toList();

        for (String expectedKafka : expectedKafkaNames) {
            if (visibleKafkaClusters.stream().noneMatch(clusterName -> clusterName.contains(expectedKafka))) {
                throw new AssertionError("Kafka cluster not found in UI expected: " + expectedKafka +
                        " - Visible clusters: " + visibleKafkaClusters
                );
            }
        }
    }
}
