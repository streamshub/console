package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.playwright.locators.CssSelectors;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

public class TopicsTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicsTestUtils.class);
    private TopicsTestUtils() {}

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
                tcc.page().click(selectorSortButton);
                PwUtils.sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
                continue;
            }

            // Case 3: Matches desired value
            break;
        }
    }

    public static void selectFilter(TestCaseConfig tcc, FilterType filterType) {
        LOGGER.debug("Selecting topic filter type [{}]", filterType.getName());
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN);

        for (int i = 0; i < Constants.SELECTOR_RETRIES; i++) {
            String currentText = CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN).innerText();

            LOGGER.debug("Filter type contains [{}]", currentText);

            // Break if the filter type is already correctly set
            if (currentText != null && currentText.contains(filterType.getName())) {
                LOGGER.debug("Filter [{}] selected", filterType.getName());
                break;
            }

            // Attempt to open the dropdown
            tcc.page().focus(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN);
            tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN);

            // Select the item if it's visible
            Locator dropdownItem = CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN_ITEMS).nth(filterType.getPosition());

            if (dropdownItem.isVisible()) {
                dropdownItem.click();
            } else {
                LOGGER.warn("Filter type in dropdown [{}] not visible", filterType.getName());
            }
        }
    }

    public static void selectTopicStatus(TestCaseConfig tcc, TopicStatus topicStatus) {
        LOGGER.debug("Selecting topic status [{}]", topicStatus.getName());
        PwUtils.waitForLocatorVisible(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
        for (int i = 0; i < Constants.SELECTOR_RETRIES; i++) {
            String currentFilters = CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CURRENT_STATUS_ITEMS).allInnerTexts().toString();

            LOGGER.debug("Locator contains {}", currentFilters);
            // Break the loop if the expected status is present
            if (currentFilters != null && currentFilters.contains(topicStatus.getName())) {
                LOGGER.debug("Topic status [{}] selected", topicStatus.getName());
                break;
            }

            // Open status dropdown
            tcc.page().focus(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
            tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
            PwUtils.sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);

            // Wait for dropdown item and click
            Locator dropdownItem = CssSelectors.getLocator(tcc, CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS).nth(topicStatus.getPosition());

            if (dropdownItem.isVisible()) {
                dropdownItem.click();
                PwUtils.sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
            } else {
                LOGGER.warn("Dropdown item topic status [{}] not visible", topicStatus.getName());
            }
        }
    }
}
