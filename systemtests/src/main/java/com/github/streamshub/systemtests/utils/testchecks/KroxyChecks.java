package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;

import java.util.List;

public class KroxyChecks {

    private KroxyChecks() {}

    public static void checkKafkaClusterDropdownContains(TestCaseConfig tcc, List<String> expectedKafkaNames) {

        List<String> visibleKafkaClusters = tcc.page().locator(CssSelectors.PAGES_NAV_KAFKA_CLUSTERS_LIST_ITEMS)
            .all()
            .stream()
            .map(locator -> PwUtils.getTrimmedText(locator.allInnerTexts().toString()))
            .toList();

        for (String expectedKafka : expectedKafkaNames) {
            if (visibleKafkaClusters.stream().noneMatch(clusterName -> clusterName.contains(expectedKafka))) {
                throw new AssertionError(
                        "Kafka cluster not found in UI expected: " + expectedKafka +
                        "\nVisible clusters: " + visibleKafkaClusters
                );
            }
        }
    }
}
