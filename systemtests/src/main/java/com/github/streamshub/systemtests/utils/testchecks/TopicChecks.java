package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopicChecks {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicChecks.class);
    private TopicChecks() {}

    /**
     * Verifies the topic state metrics displayed on the Kafka Overview page.
     * It navigates to the overview page for the specified Kafka cluster and
     * waits for the expected counts of total topics, partitions, and their replication statuses.
     *
     * @param tcc           the test case configuration holding the Playwright page context
     * @param kafkaName     the name of the Kafka cluster
     * @param total         the expected total number of topics
     * @param partitions    the expected total number of partitions across all topics
     * @param fullyReplicated the expected count of fully replicated partitions
     * @param underReplicated the expected count of under-replicated partitions
     * @param unavailable   the expected count of unavailable partitions
     */
    public static void checkOverviewPageTopicState(TestCaseConfig tcc, String kafkaName, int total, int partitions, int fullyReplicated, int underReplicated, int unavailable) {
        LOGGER.info("Verify Overview Page topic status [{} total topics] [{} Partitions]", total, partitions);
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, kafkaName), PwUtils.getDefaultNavigateOpts());

        // Status
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_TOTAL_TOPICS, total + " topics", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_TOTAL_PARTITIONS, partitions + " partitions", true);

        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_FULLY_REPLICATED, fullyReplicated + " Fully replicated", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_UNDER_REPLICATED, underReplicated + " Under replicated", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPICS_CARD_UNAVAILABLE, unavailable + " Unavailable", true);
    }

    /**
     * Verifies the topic state metrics displayed on the Kafka Topics page.
     * It navigates to the topics page for the specified Kafka cluster and
     * waits for the expected counts of total topics and their replication statuses.
     *
     * @param tcc            the test case configuration holding the Playwright page context
     * @param kafkaName      the name of the Kafka cluster
     * @param total          the expected total number of topics
     * @param fullyReplicated the expected count of fully replicated partitions
     * @param underReplicated the expected count of under-replicated partitions
     * @param unavailable    the expected count of unavailable partitions
     */
    public static void checkTopicsPageTopicState(TestCaseConfig tcc, String kafkaName, int total, int fullyReplicated, int underReplicated, int unavailable) {
        LOGGER.info("Verify Overview Page topic status [{} total topics] [FullyReplicated: {}] [UnderReplicated: {}] [Unavailabe: {}]", total, fullyReplicated, underReplicated, unavailable);
        // Total topic count
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, kafkaName), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_HEADER_TOTAL_TOPICS_BADGE, total + " total", true);
        // Status
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_HEADER_BADGE_STATUS_SUCCESS, Integer.toString(fullyReplicated), true);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_HEADER_BADGE_STATUS_WARNING, Integer.toString(underReplicated), true);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_HEADER_BADGE_STATUS_ERROR, Integer.toString(unavailable), true);
    }

    /**
     * Checks the pagination functionality on the topics page for different
     * "topics per page" settings. It verifies that navigating forward and backward
     * through the pages displays the correct range of topics and pagination text.
     * For each configured topics per page value, the method:
     *  - Navigates to the topics page
     *  - Selects the topics per page dropdown value
     *  - Iterates forward through all pages, verifying content and pagination info
     *  - Iterates backward through all pages, verifying content and pagination info
     *
     * @param tcc                     the test case configuration with page context
     * @param topicsCount             the total number of topics present
     * @param topicsPerPageList       list of integers specifying topics per page options to test
     * @param dropdownButtonSelector  CSS selector for the dropdown button controlling topics per page
     * @param dropdownItemsSelector   CSS selector for the dropdown items in the topics per page selector
     * @param paginationTextSelector  CSS selector for the pagination summary text (e.g., "1-10 of 57")
     * @param previousButtonSelector  CSS selector for the pagination "previous page" button
     * @param nextButtonSelector      CSS selector for the pagination "next page" button
     */
    public static void checkPaginationPage(TestCaseConfig tcc, int topicsCount, List<Integer> topicsPerPageList,
        String dropdownButtonSelector, String dropdownItemsSelector, String paginationTextSelector, String previousButtonSelector, String nextButtonSelector) {
        for (Integer topicsPerPage : topicsPerPageList) {
            int lowBoundary;
            int highBoundary;
            int perPageItemIndex = topicsPerPageList.indexOf(topicsPerPage);
            int topicsOnPage;

            // Go to topics page
            tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

            LOGGER.debug("Click on topics per page selection");
            PwUtils.waitForLocatorAndClick(tcc, dropdownButtonSelector);

            LOGGER.debug("Select topics per page dropdown item");
            PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(dropdownItemsSelector).nth(perPageItemIndex + 1).build());

            // Check pages
            int pageOverflow = topicsCount % topicsPerPage;
            int numOfPages = (topicsCount / topicsPerPage) + (pageOverflow > 0 ? 1 : 0);
            int finalPageSize = pageOverflow > 0 ? pageOverflow : topicsPerPage;

            // Forward movement
            for (int pageNum = 1; pageNum <= numOfPages; pageNum++) {
                lowBoundary = (topicsPerPage * (pageNum - 1)) + 1;
                highBoundary = Integer.min(topicsPerPage * pageNum, topicsCount);
                topicsOnPage = pageNum == numOfPages ? finalPageSize : topicsPerPage;
                checkPaginationContent(tcc, pageNum, numOfPages, topicsOnPage, lowBoundary, highBoundary, topicsCount, paginationTextSelector, nextButtonSelector);
            }
            // Backwards movement
            for (int pageNum = numOfPages; pageNum >= 1; pageNum--) {
                lowBoundary = (topicsPerPage * (pageNum - 1)) + 1;
                highBoundary = Integer.min(topicsPerPage * pageNum, topicsCount);
                topicsOnPage = pageNum == numOfPages ? finalPageSize : topicsPerPage;
                checkPaginationContent(tcc, pageNum, 1, topicsOnPage, lowBoundary, highBoundary, topicsCount, paginationTextSelector, previousButtonSelector);
            }
        }
    }

    /**
     * Helper method to check the pagination content on a specific page.
     * It verifies the number of topics shown on the page, the pagination summary text,
     * and clicks the button to move forward or backward unless on the last page.
     *
     * @param tcc                   the test case configuration with page context
     * @param pageNum               the current page number being checked
     * @param numOfFinalPage        the total number of pages in the current direction (forward or backward)
     * @param topicsOnPage          the expected number of topics displayed on the current page
     * @param lowBoundary           the lowest topic index shown on the current page (1-based)
     * @param highBoundary          the highest topic index shown on the current page
     * @param topicsCount           the total number of topics in all pages
     * @param paginationTextSelector CSS selector for the pagination summary text element
     * @param moveButtonSelector    CSS selector for the button used to navigate to the next/previous page
     */
    private static void checkPaginationContent(TestCaseConfig tcc, int pageNum, int numOfFinalPage, int topicsOnPage, int lowBoundary, int highBoundary, int topicsCount, String paginationTextSelector, String moveButtonSelector) {
        LOGGER.debug("Checking page {}/{}", pageNum, numOfFinalPage);
        // Check that correct number of topics is displayed
        PwUtils.waitForLocatorVisible(tcc, new CssBuilder(TopicsPageSelectors.TPS_TABLE_ROWS).nth(1).build());
        assertEquals(topicsOnPage, tcc.page().locator(TopicsPageSelectors.TPS_TABLE_ROWS).all().size());

        // Check pagination details
        String paginationOf = String.format("%s - %s of %s", lowBoundary, highBoundary, topicsCount);
        LOGGER.debug("Checking pagination to contain {}", paginationOf);
        PwUtils.waitForContainsText(tcc, paginationTextSelector, paginationOf, true);

        // Click to move to next page
        if (pageNum == numOfFinalPage) {
            LOGGER.debug("Pagination check completed");
            return;
        }

        LOGGER.debug("Checking page");
        PwUtils.waitForLocatorAndClick(tcc, moveButtonSelector);
    }

    /**
     * Checks the filtering functionality on the topics page by topic name.
     * For each topic name in the list, this method applies the name filter,
     * searches for the topic, and verifies that the first table row contains the expected topic name.
     * After all checks, it clears all filters.
     *
     * @param tcc         the test case configuration with page context
     * @param topicNames  a list of topic names to filter and verify
     */
    public static void checkTopicsFilterByName(TestCaseConfig tcc, List<String> topicNames) {
        LOGGER.info("Filter topics by name");
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);
        for (String topicName : topicNames) {
            LOGGER.debug("Verify topic name {}", topicName);
            PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, topicName);
            PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
            PwUtils.waitForContainsText(tcc, TopicsPageSelectors.getTableRowItems(1), topicName, false);
        }
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
    }

    /**
     * Checks the filtering functionality on the topics page by topic ID.
     * For each topic name, it retrieves the corresponding topic ID from the cluster,
     * applies the ID filter, searches using the topic ID, and verifies that the first table row
     * contains the expected topic name. After all checks, it clears all filters.
     *
     * @param tcc         the test case configuration with page context
     * @param topicNames  a list of topic names whose IDs will be used for filtering and verification
     */
    public static void checkTopicsFilterById(TestCaseConfig tcc, List<String> topicNames) {
        LOGGER.info("Filter topics by id");
        TopicsTestUtils.selectFilter(tcc, FilterType.TOPIC_ID);
        for (String topicName : topicNames) {
            String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
            LOGGER.debug("Verify topic {} with id {}", topicName, topicId);
            PwUtils.waitForLocatorAndFill(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH, topicId);
            PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
            PwUtils.waitForContainsText(tcc, TopicsPageSelectors.getTableRowItems(1), topicName, false);
        }
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
    }

    /**
     * Checks the filtering functionality on the topics page by topic status.
     * Applies the status filter and verifies that the filtered topics match the expected list of topic names.
     * Waits until the number of displayed topic rows matches the expected count before verification.
     * After verification, clears all applied filters.
     *
     * @param tcc         the test case configuration providing page context
     * @param topicNames  the list of topic names expected to be visible after filtering by status
     * @param status      the {@link TopicStatus} to filter topics by
     */
    public static void checkTopicsFilterByStatus(TestCaseConfig tcc, List<String> topicNames, TopicStatus status) {
        LOGGER.info("Filter topics by status");
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, status);
        // Use default max results per page or actual topic count if it's less than the maximum per page
        PwUtils.waitForLocatorCount(tcc, Math.min(topicNames.size(), Constants.DEFAULT_TOPICS_PER_PAGE), TopicsPageSelectors.TPS_TABLE_ROWS, false);

        for (String topicName : topicNames) {
            LOGGER.debug("Verify topic {} status {}", topicName, status.getName());
            PwUtils.waitForContainsText(tcc, TopicsPageSelectors.TPS_TABLE_ROWS, topicName, true);
        }

        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
    }
}
