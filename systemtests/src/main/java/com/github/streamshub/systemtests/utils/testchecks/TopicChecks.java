package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.playwright.locators.CssSelectors;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TopicChecks {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicChecks.class);
    private TopicChecks() {}

    public static void checkOverviewPageTopicState(TestCaseConfig tcc, String kafkaName, int total, int partitions, int fullyReplicated, int underReplicated, int unavailable) {
        LOGGER.info("Verify Overview Page topic status [{} total topics] [{} Partitions]", total, partitions);
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, kafkaName), PwUtils.getDefaultNavigateOpts());

        // Status
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_PAGE_TOPICS_CARD_TOTAL_TOPICS, total + " topics", true);
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_PAGE_TOPICS_CARD_TOTAL_PARTITIONS, partitions + " partitions", true);

        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_PAGE_TOPICS_CARD_FULLY_REPLICATED, fullyReplicated + " Fully replicated", true);
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_PAGE_TOPICS_CARD_UNDER_REPLICATED, underReplicated + " Under replicated", true);
        PwUtils.waitForContainsText(tcc, CssSelectors.C_OVERVIEW_PAGE_TOPICS_CARD_UNAVAILABLE, unavailable + " Unavailable", true);
    }

    public static void checkTopicsPageTopicState(TestCaseConfig tcc, String kafkaName, int total, int fullyReplicated, int underReplicated, int unavailable) {
        LOGGER.info("Verify Overview Page topic status [{} total topics] [FullyReplicated: {}] [UnderReplicated: {}] [Unavailabe: {}]", total, fullyReplicated, underReplicated, unavailable);
        // Total topic count
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, kafkaName), PwUtils.getDefaultNavigateOpts());
        PwUtils.waitForContainsText(tcc, CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_HEADER_TOTAL_TOPICS_BADGE), total + " total", true);
        // Status
        PwUtils.waitForContainsText(tcc, CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_HEADER_BADGE_STATUS_SUCCESS), Integer.toString(fullyReplicated), true);
        PwUtils.waitForContainsText(tcc, CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_HEADER_BADGE_STATUS_WARNING), Integer.toString(underReplicated), true);
        PwUtils.waitForContainsText(tcc, CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_HEADER_BADGE_STATUS_ERROR), Integer.toString(unavailable), true);
    }

    public static void checkPaginationPage(TestCaseConfig tcc, int topicsCount, List<Integer> topicsPerPageList,
        String dropdownButtonSelector, String dropdownItemsSelector, String paginationTextSelector, String previousButtonSelector, String nextButtonSelector) {
        for (Integer topicsPerPage : topicsPerPageList) {
            int lowBoundary;
            int highBoundary;
            int perPageItemIndex = topicsPerPageList.indexOf(topicsPerPage);
            int topicsOnPage;

            // Go to topics page
            tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

            // Click on topics per page selection dropdown
            LOGGER.debug("Click on topics per page selection");
            PwUtils.waitForLocatorVisible(tcc, dropdownButtonSelector);
            tcc.page().click(dropdownButtonSelector);

            LOGGER.debug("Select topics per page dropdown item");
            PwUtils.waitForLocatorVisible(CssSelectors.getLocator(tcc, dropdownItemsSelector).nth(perPageItemIndex));
            CssSelectors.getLocator(tcc, dropdownItemsSelector).nth(perPageItemIndex).click();

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

    private static void checkPaginationContent(TestCaseConfig tcc, int pageNum, int numOfFinalPage, int topicsOnPage, int lowBoundary, int highBoundary, int topicsCount, String paginationTextSelector, String moveButtonSelector) {
        LOGGER.debug("Checking page {}/{}", pageNum, numOfFinalPage);
        // Check that correct number of topics is displayed
        PwUtils.waitForLocatorVisible(CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_TABLE_ROWS).nth(2));
        assertEquals(topicsOnPage, CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_TABLE_ROWS).count());

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
        PwUtils.waitForLocatorVisible(tcc, moveButtonSelector);
        tcc.page().click(moveButtonSelector);
    }


    public static void checkTopicsFilterByName(TestCaseConfig tcc, List<String> topicNames) {
        LOGGER.info("Filter topics by name");
        TopicsTestUtils.selectFilter(tcc, FilterType.NAME);
        for (String topicName : topicNames) {
            LOGGER.debug("Verify topic name {}", topicName);
            tcc.page().fill(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_SEARCH, topicName);
            tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
            PwUtils.waitForContainsText(tcc, CssSelectors.getTopicsPageTableRowItems(0), topicName, false);
        }
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
        tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
    }

    public static void checkTopicsFilterById(TestCaseConfig tcc, List<String> topicNames) {
        LOGGER.info("Filter topics by id");
        TopicsTestUtils.selectFilter(tcc, FilterType.TOPIC_ID);
        for (String topicName : topicNames) {
            String topicId = ResourceUtils.getKubeResource(KafkaTopic.class, tcc.namespace(), topicName).getStatus().getTopicId();
            LOGGER.debug("Verify topic {} with id {}", topicName, topicId);
            tcc.page().fill(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_SEARCH, topicId);
            tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_SEARCH_BUTTON);
            PwUtils.waitForContainsText(tcc, CssSelectors.getTopicsPageTableRowItems(0), topicName, false);
        }
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
        tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
    }

    public static void checkTopicsFilterByStatus(TestCaseConfig tcc, List<String> topicNames, TopicStatus status) {
        LOGGER.info("Filter topics by status");
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, status);

        PwUtils.waitForLocatorCount(tcc, topicNames.size(), CssSelectors.TOPICS_PAGE_TABLE_ROWS, false);

        for (String topicName : topicNames) {
            LOGGER.debug("Verify topic {} status {}", topicName, status.getName());
            assertTrue(CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_TABLE_ROWS).allInnerTexts().toString().contains(topicName));
        }

        PwUtils.waitForLocatorVisible(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
        tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);
    }
}
