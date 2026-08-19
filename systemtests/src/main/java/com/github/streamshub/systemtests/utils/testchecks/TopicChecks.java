package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.enums.TopicsPerPage;
import com.github.streamshub.systemtests.locators.pages.ClusterOverviewPage;
import com.github.streamshub.systemtests.locators.pages.TopicsPage;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import com.microsoft.playwright.Locator;

import org.apache.logging.log4j.Logger;

import java.util.List;

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
        LOGGER.info("Checking overview page topic status: {} total topics, {} partitions, {} fully replicated, {} under-replicated, {} unavailable", total, partitions, fullyReplicated, underReplicated, unavailable);
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, kafkaName));

        // Status
        // useTopics() (backing all counts below) fetches once with no refetchInterval, so a stale count
        // needs a page reload to re-fetch - a plain re-check of the same DOM would retry forever on stale data.
        PwUtils.waitForContainsText(ClusterOverviewPage.totalTopics(tcc.page()), total + " topics", true, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(ClusterOverviewPage.totalPartitions(tcc.page()), partitions + " partitions", true, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);

        PwUtils.waitForContainsText(ClusterOverviewPage.fullyReplicated(tcc.page()), fullyReplicated + " Fully replicated", true, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(ClusterOverviewPage.underReplicated(tcc.page()), underReplicated + " Under-replicated", true, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(ClusterOverviewPage.unavailable(tcc.page()), unavailable + " Unavailable", true, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
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
        LOGGER.info("Checking topics page topic status: {} total topics, {} fully replicated, {} under-replicated, {} unavailable", total, fullyReplicated, underReplicated, unavailable);
        // Total topic count
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, kafkaName));

        // Same non-polling useTopics() data source as the overview page - a reload is needed to see an updated count.
        PwUtils.waitForContainsText(TopicsPage.totalTopicsBadge(tcc.page()), total + " total", true, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(TopicsPage.fullyReplicatedBadge(tcc.page()), Integer.toString(fullyReplicated), true, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(TopicsPage.underReplicatedBadge(tcc.page()), Integer.toString(underReplicated), true, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(TopicsPage.offlineBadge(tcc.page()), Integer.toString(unavailable), true, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
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
     * @param tcc                the test case configuration with page context
     * @param topicsCount        the total number of topics present
     * @param topicsPerPageList  list of topics per page options to test
     * @param dropdownButton     the dropdown button controlling topics per page
     * @param dropdownItems      the dropdown items in the topics per page selector
     * @param paginationText     the pagination summary text (e.g., "1-10 of 57")
     * @param previousButton     the pagination "previous page" button
     * @param nextButton         the pagination "next page" button
     */
    public static void checkPaginationPage(TestCaseConfig tcc, int topicsCount, List<TopicsPerPage> topicsPerPageList,
        Locator dropdownButton, Locator dropdownItems, Locator paginationText, Locator previousButton, Locator nextButton) {
        for (TopicsPerPage topicsPerPageOption : topicsPerPageList) {
            int lowBoundary;
            int highBoundary;
            int topicsPerPage = topicsPerPageOption.getValue();
            int topicsOnPage;

            LOGGER.info("Checking pagination for {} topics using {} topics per page", topicsCount, topicsPerPage);

            // Go to topics page
            PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()));

            LOGGER.debug("Opening topics-per-page dropdown selector");
            PwUtils.waitForLocatorAndClick(dropdownButton);

            LOGGER.debug("Selecting topics-per-page dropdown item [{}] for value {}", topicsPerPageOption.getDropdownPosition(), topicsPerPage);
            PwUtils.waitForLocatorAndClick(dropdownItems.nth(topicsPerPageOption.getDropdownPosition() - 1));

            // Check pages
            int pageOverflow = topicsCount % topicsPerPage;
            int numOfPages = (topicsCount / topicsPerPage) + (pageOverflow > 0 ? 1 : 0);
            int finalPageSize = pageOverflow > 0 ? pageOverflow : topicsPerPage;

            // Forward movement
            for (int pageNum = 1; pageNum <= numOfPages; pageNum++) {
                lowBoundary = (topicsPerPage * (pageNum - 1)) + 1;
                highBoundary = Integer.min(topicsPerPage * pageNum, topicsCount);
                topicsOnPage = pageNum == numOfPages ? finalPageSize : topicsPerPage;
                checkPaginationContent(tcc, pageNum, numOfPages, topicsOnPage, lowBoundary, highBoundary, topicsCount, paginationText, nextButton);
            }
            // Backwards movement
            for (int pageNum = numOfPages; pageNum >= 1; pageNum--) {
                lowBoundary = (topicsPerPage * (pageNum - 1)) + 1;
                highBoundary = Integer.min(topicsPerPage * pageNum, topicsCount);
                topicsOnPage = pageNum == numOfPages ? finalPageSize : topicsPerPage;
                checkPaginationContent(tcc, pageNum, 1, topicsOnPage, lowBoundary, highBoundary, topicsCount, paginationText, previousButton);
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
     * @param topicsCount        the total number of topics in all pages
     * @param paginationText     the pagination summary text element
     * @param moveButton         the button used to navigate to the next/previous page
     */
    private static void checkPaginationContent(TestCaseConfig tcc, int pageNum, int numOfFinalPage, int topicsOnPage, int lowBoundary, int highBoundary, int topicsCount, Locator paginationText, Locator moveButton) {
        LOGGER.debug("Checking pagination page {}, expecting {} topics displayed", pageNum, topicsOnPage);
        // Check that correct number of topics is displayed
        PwUtils.waitForLocatorCount(topicsOnPage, TopicsPage.table(tcc.page()).rows(), false);
        // Check pagination details
        String paginationOf = String.format("%s - %s of %s", lowBoundary, highBoundary, topicsCount);
        LOGGER.debug("Verifying pagination summary text shows [{}]", paginationOf);
        PwUtils.waitForContainsText(paginationText, paginationOf, false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);

        // Click to move to next page
        if (pageNum == numOfFinalPage) {
            LOGGER.debug("Reached final page {} of pagination sequence", pageNum);
            return;
        }

        LOGGER.debug("Navigating to next pagination page via locator [{}]", moveButton);
        PwUtils.waitForLocatorAndClick(moveButton);
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
        LOGGER.info("Checking topics filter by name for {} topic(s): {}", topicNames.size(), topicNames);
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);
        for (String topicName : topicNames) {
            LOGGER.debug("Verifying filtered result shows topic name [{}]", topicName);
            PwUtils.waitForLocatorAndFill(TopicsPage.searchInput(tcc.page()), topicName);
            PwUtils.waitForLocatorAndClick(TopicsPage.searchButton(tcc.page()));
            PwUtils.waitForContainsText(TopicsPage.table(tcc.page()).row(topicName), topicName, false);
        }
        PwUtils.waitForLocatorAndClick(TopicsPage.clearAllFiltersButton(tcc.page()));
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
        LOGGER.info("Checking topics filter by ID for {} topic(s): {}", topicNames.size(), topicNames);
        TopicsTestUtils.selectFilter(tcc, FilterType.TOPIC_ID);
        for (String topicName : topicNames) {
            String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
            LOGGER.debug("Verifying filtered result shows topic [{}] for id [{}]", topicName, topicId);
            PwUtils.waitForLocatorAndFill(TopicsPage.searchInput(tcc.page()), topicId);
            PwUtils.waitForLocatorAndClick(TopicsPage.searchButton(tcc.page()));
            PwUtils.waitForContainsText(TopicsPage.table(tcc.page()).rows().first(), topicName, false);
        }
        PwUtils.waitForLocatorAndClick(TopicsPage.clearAllFiltersButton(tcc.page()));
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
        LOGGER.info("Checking topics filter by status [{}] returns {} topic(s): {}", status.getName(), topicNames.size(), topicNames);
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, status);
        // Use default max results per page or actual topic count if it's less than the maximum per page
        PwUtils.waitForLocatorCount(Math.min(topicNames.size(), Constants.DEFAULT_TOPICS_PER_PAGE), TopicsPage.table(tcc.page()).rows(), false);

        for (String topicName : topicNames) {
            LOGGER.debug("Verifying topic [{}] is present in results filtered by status [{}]", topicName, status.getName());
            PwUtils.waitForContainsText(TopicsPage.table(tcc.page()).rows(), topicName, true);
        }

        PwUtils.waitForLocatorAndClick(TopicsPage.clearAllFiltersButton(tcc.page()));
    }
}
