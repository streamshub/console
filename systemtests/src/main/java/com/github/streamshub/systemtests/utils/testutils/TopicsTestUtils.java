package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
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
        LOGGER.info("Sorting topics table using button [{}] until aria-sort={}", selectorSortButton, expectedAttr);
        Utils.retryAction("Ensure topics table is sorted correctly", () -> {
            Locator locator = tcc.page().locator(selectorWithAttribute);

            if (locator == null || locator.isHidden()) {
                PwUtils.screenshot(tcc, tcc.kafkaName(), "topicStatusFilterInvisible");
                throw new IllegalStateException("Locator was not visible");
            }

            String currentAttr = locator.getAttribute("aria-sort");
            if (!expectedAttr.equals(currentAttr)) {
                PwUtils.waitForLocatorAndClick(tcc, selectorSortButton);
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
     * This method repeatedly attempts (up to {@link Constants#SELECTOR_RETRIES} times) to select the filter type
     * specified by {@code filterType}. It first checks if the desired filter is already selected by inspecting
     * the current dropdown text. If not selected, it opens the dropdown, waits for the desired filter option to be visible,
     * and clicks it.
     *
     * @param tcc         the test case configuration containing the page and context
     * @param filterType  the {@link FilterType} to select from the filter dropdown
     */
    public static void selectFilter(TestCaseConfig tcc, FilterType filterType) {
        LOGGER.info("Selecting topic filter type [{}]", filterType.getName());
        PwUtils.waitForLocatorVisible(tcc, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_NAME);
        String filterTypeSelector = new CssBuilder(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN).build();
        // if status is selected and both are present
        if (tcc.page().locator(filterTypeSelector).count() > 1) {
            LOGGER.debug("Topic filter type status is already present = two identical css buttons, select the first button");
            filterTypeSelector = new CssBuilder(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN).nth(1).build();
        }

        String currentText = tcc.page().locator(filterTypeSelector).innerText();
        LOGGER.debug("Filter type contains [{}]", currentText);

        // Already correct
        if (currentText != null && currentText.contains(filterType.getName())) {
            LOGGER.debug("Filter [{}] already selected", filterType.getName());
            return;
        }

        PwUtils.waitForLocatorAndClick(tcc, filterTypeSelector);
        String dropdownItem = new CssBuilder(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN_ITEMS)
            .nth(filterType.getPosition())
            .build();
        PwUtils.waitForLocatorAndClick(tcc, dropdownItem);
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
        LOGGER.info("Selecting topic status filter [{}]", topicStatus.getName());
        // Try selecting it
        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN);
        String dropdownItemInput = new CssBuilder(TopicsPageSelectors.TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
            .nth(topicStatus.getPosition()).build();
        PwUtils.waitForLocatorAndClick(tcc, dropdownItemInput);

    }

}
