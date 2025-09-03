package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

public class TopicsTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicsTestUtils.class);
    private TopicsTestUtils() {}

    /**
     * Attempts to select a sorting option on the page by repeatedly checking and clicking until the
     * element identified by {@code selectorWithAttribute} has the specified {@code aria-sort} attribute value.
     *
     * The method tries up to {@link Constants#SELECTOR_RETRIES} times, waiting a short interval between attempts.
     * It clicks the element identified by {@code selectorSortButton} to change the sort order if the
     * current attribute does not match the desired {@code attributeVal}.
     *
     * @param tcc                   the test case configuration containing the page context
     * @param selectorWithAttribute CSS selector to locate the element whose 'aria-sort' attribute is checked
     * @param selectorSortButton    CSS selector to locate the button that triggers sorting when clicked
     * @param expectedAttr          the desired value of the 'aria-sort' attribute to confirm selection
     */
    public static void selectSortBy(TestCaseConfig tcc, String selectorWithAttribute, String selectorSortButton, String expectedAttr) {
        LOGGER.info("Select sort by with value {}", expectedAttr);
        Utils.retryAction("Ensure topics table is sorted correctly", () -> {
            PwUtils.removeFocus(tcc);
            Locator locator = tcc.page().locator(selectorWithAttribute);

            if (locator == null || locator.isHidden()) {
                PwUtils.screenshot(tcc, tcc.kafkaName(), "topicStatusFilterInvisible");
                throw new IllegalStateException("Locator was not visible");
            }

            String currentAttr = locator.getAttribute("aria-sort");
            if (!expectedAttr.equals(currentAttr)) {
                PwUtils.screenshot(tcc, tcc.kafkaName(), "topicStatusFilterIncorrect");
                PwUtils.waitForLocatorAndClick(tcc, selectorSortButton);
                throw new IllegalStateException(
                    "Locator had aria-sort=" + currentAttr + ", expected=" + expectedAttr
                );
            }

            LOGGER.info("Locator attribute matched expected value: {}", expectedAttr);
        }, Constants.SELECTOR_RETRIES);
    }


    /**
     * Selects a topic filter type from the filter dropdown in the UI.
     *
     * This method repeatedly attempts (up to {@link Constants#SELECTOR_RETRIES} times) to select the filter type
     * specified by {@code filterType}. It first checks if the desired filter is already selected by inspecting
     * the current dropdown text. If not selected, it opens the dropdown, waits for the desired filter option to be visible,
     * and clicks it.
     *
     * @param tcc         the test case configuration containing the page and context
     * @param filterType  the {@link FilterType} to select from the filter dropdown
     */
    public static void selectFilter(TestCaseConfig tcc, FilterType filterType) {
        LOGGER.debug("Selecting topic filter type [{}]", filterType.getName());
        PwUtils.waitForLocatorVisible(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN);

        Utils.retryAction("Select filter type " + filterType.getName(), () -> {
            String currentText = tcc.page().locator(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN).innerText();

            LOGGER.debug("Filter type contains [{}]", currentText);

            // Already correct - stop retrying
            if (currentText != null && currentText.contains(filterType.getName())) {
                LOGGER.debug("Filter [{}] already selected", filterType.getName());
                return;
            }

            // Try to select the filter
            PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN);

            String dropdownItem = new CssBuilder(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN_ITEMS)
                .nth(filterType.getPosition())
                .build();

            if (tcc.page().locator(dropdownItem).isVisible()) {
                PwUtils.waitForLocatorAndClick(tcc, dropdownItem);
            } else {
                LOGGER.warn("Filter type in dropdown [{}] not visible", filterType.getName());
            }

            // Fail this attempt so retryAction will try again
            throw new IllegalStateException("Filter [" + filterType.getName() + "] not yet selected");
        }, Constants.SELECTOR_RETRIES);
    }


    /**
     * Selects a topic status filter from the status dropdown in the UI.
     *
     * This method tries up to {@link Constants#SELECTOR_RETRIES} times to ensure the given
     * {@code topicStatus} is selected. It first checks the currently applied status filters,
     * and if the desired status is not selected, it opens the status dropdown,
     * waits for the corresponding item to be visible, and clicks it.
     *
     * @param tcc          the test case configuration with page and context information
     * @param topicStatus  the {@link TopicStatus} enum value representing the desired status to filter by
     */
    public static void selectTopicStatus(TestCaseConfig tcc, TopicStatus topicStatus) {
        LOGGER.debug("Selecting topic status [{}]", topicStatus.getName());
        PwUtils.waitForLocatorVisible(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);

        Utils.retryAction("Select topic status " + topicStatus.getName(), () -> {
            String currentFilters = tcc.page()
                .locator(TopicsPageSelectors.TPS_TOP_TOOLBAR_SEARCH_CURRENT_STATUS_ITEMS)
                .allInnerTexts()
                .toString();

            LOGGER.debug("Locator contains {}", currentFilters);

            // Already selected - success
            if (currentFilters != null && currentFilters.contains(topicStatus.getName())) {
                LOGGER.debug("Topic status [{}] selected", topicStatus.getName());
                return;
            }

            // Try selecting it
            tcc.page().focus(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
            PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
            Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);

            String dropdownItemInput = new CssBuilder(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
                .nth(topicStatus.getPosition())
                .withDesc()
                .withElementInput()
                .build();

            PwUtils.waitForLocatorAndClick(tcc, dropdownItemInput);
            Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);

            // Fail this attempt so retryAction retries if needed
            throw new IllegalStateException("Topic status [" + topicStatus.getName() + "] not yet selected");
        }, Constants.SELECTOR_RETRIES);
    }

}
