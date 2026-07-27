package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.locators.pages.TopicsPage;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import org.apache.logging.log4j.Logger;

public class TopicsTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicsTestUtils.class);
    private TopicsTestUtils() {}

    /**
     * Attempts to select a sorting option on the page by repeatedly checking and clicking until the
     * given column header has the specified {@code aria-sort} attribute value.
     *
     * The method tries up to {@link Constants#SELECTOR_RETRIES} times, waiting a short interval between attempts.
     * It clicks the sort button within {@code header} to change the sort order if the
     * current attribute does not match the desired {@code expectedAttr}.
     *
     * @param tcc          the test case configuration containing the page context
     * @param header       the column header {@link Locator} whose {@code aria-sort} attribute is checked
     * @param expectedAttr the desired value of the 'aria-sort' attribute to confirm selection
     */
    public static void selectSortBy(TestCaseConfig tcc, Locator header, String expectedAttr) {
        LOGGER.info("Sorting topics table using header [{}] until aria-sort={}", header, expectedAttr);
        Utils.retryAction("Ensure topics table is sorted correctly", () -> {
            if (header == null || header.isHidden()) {
                PwUtils.screenshot(tcc, tcc.kafkaName(), "topicStatusFilterInvisible");
                throw new IllegalStateException("Locator was not visible");
            }

            String currentAttr = header.getAttribute("aria-sort");
            if (!expectedAttr.equals(currentAttr)) {
                PwUtils.waitForLocatorAndClick(header.getByRole(AriaRole.BUTTON));
                LOGGER.warn("Locator had incorrect aria-sort={}, expected={}, clicking sort button again", currentAttr, expectedAttr);
                return false;
            }

            LOGGER.debug("Locator attribute matched expected value: {}", expectedAttr);
            return true;

        }, Constants.SELECTOR_RETRIES);
    }


    /**
     * Selects a topic filter type from the filter dropdown in the UI.
     *
     * This method first checks if the desired filter is already selected by inspecting
     * the current dropdown text. If not selected, it opens the dropdown, waits for the desired filter option
     * to be visible, and clicks it.
     *
     * @param tcc         the test case configuration containing the page and context
     * @param filterType  the {@link FilterType} to select from the filter dropdown
     */
    public static void selectFilter(TestCaseConfig tcc, FilterType filterType) {
        LOGGER.info("Selecting topic filter type [{}]", filterType.getName());
        PwUtils.waitForLocatorVisible(TopicsPage.sortByNameHeader(tcc.page()));

        String currentText = TopicsPage.filterTypeDropdownButton(tcc.page()).innerText();
        LOGGER.debug("Filter type contains [{}]", currentText);

        // Already correct
        if (currentText != null && currentText.contains(filterType.getName())) {
            LOGGER.debug("Filter [{}] already selected", filterType.getName());
            return;
        }

        PwUtils.waitForLocatorAndClick(TopicsPage.filterTypeDropdownButton(tcc.page()));
        PwUtils.waitForLocatorAndClick(TopicsPage.filterTypeDropdownItems(tcc.page()).nth(filterType.getPosition() - 1));
    }


    /**
     * Selects a topic status filter from the status dropdown in the UI.
     *
     * @param tcc          the test case configuration with page and context information
     * @param topicStatus  the {@link TopicStatus} enum value representing the desired status to filter by
     */
    public static void selectTopicStatus(TestCaseConfig tcc, TopicStatus topicStatus) {
        LOGGER.info("Selecting topic status filter [{}]", topicStatus.getName());
        PwUtils.waitForLocatorAndClick(TopicsPage.statusFilterDropdownButton(tcc.page()));
        PwUtils.waitForLocatorAndClick(TopicsPage.statusFilterDropdownItems(tcc.page()).nth(topicStatus.getPosition() - 1));
    }

}
