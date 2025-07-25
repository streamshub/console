package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
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
     * @param attributeVal          the desired value of the 'aria-sort' attribute to confirm selection
     */
    public static void selectSortBy(TestCaseConfig tcc, String selectorWithAttribute, String selectorSortButton, String attributeVal) {
        LOGGER.info("Select sort by with value {}", attributeVal);
        for (int i = 0; i < Constants.SELECTOR_RETRIES; i++) {
            Locator locator = CssSelectors.getLocator(tcc, selectorWithAttribute);
            // Case 1: Locator is null or hidden
            if (locator == null || locator.isHidden()) {
                LOGGER.warn("Locator is not present or its attribute is not present");
                PwUtils.sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
                continue;
            }

            // Case 2: Locator is present but attribute is not equal to expected
            String currentAttribute = locator.getAttribute("aria-sort");
            if (currentAttribute == null || !currentAttribute.equals(attributeVal)) {
                LOGGER.warn("Locator is present, but its attribute is not equal to {}", attributeVal);
                PwUtils.waitForLocatorAndClick(tcc, selectorSortButton);
                PwUtils.sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
                continue;
            }

            // Case 3: Matches desired value
            break;
        }
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

        for (int i = 0; i < Constants.SELECTOR_RETRIES; i++) {
            String currentText = CssSelectors.getLocator(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN).innerText();

            LOGGER.debug("Filter type contains [{}]", currentText);

            // Break if the filter type is already correctly set
            if (currentText != null && currentText.contains(filterType.getName())) {
                LOGGER.debug("Filter [{}] selected", filterType.getName());
                break;
            }

            // Attempt to open the dropdown
            tcc.page().focus(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN);
            PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN);

            // Select the item if it's visible
            Locator dropdownItem = CssSelectors.getLocator(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN_ITEMS).nth(filterType.getPosition());

            if (dropdownItem.isVisible()) {
                PwUtils.clickWithRetry(dropdownItem);
            } else {
                LOGGER.warn("Filter type in dropdown [{}] not visible", filterType.getName());
            }
        }
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
        for (int i = 0; i < Constants.SELECTOR_RETRIES; i++) {
            String currentFilters = CssSelectors.getLocator(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_SEARCH_CURRENT_STATUS_ITEMS).allInnerTexts().toString();

            LOGGER.debug("Locator contains {}", currentFilters);
            // Break the loop if the expected status is present
            if (currentFilters != null && currentFilters.contains(topicStatus.getName())) {
                LOGGER.debug("Topic status [{}] selected", topicStatus.getName());
                break;
            }

            // Open status dropdown
            tcc.page().focus(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
            PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
            PwUtils.sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);

            // Wait for dropdown item and click
            Locator dropdownItem = CssSelectors.getLocator(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS).nth(topicStatus.getPosition());

            if (dropdownItem.isVisible()) {
                PwUtils.clickWithRetry(dropdownItem);
                PwUtils.sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
            } else {
                LOGGER.warn("Dropdown item topic status [{}] not visible", topicStatus.getName());
            }
        }
    }
}
