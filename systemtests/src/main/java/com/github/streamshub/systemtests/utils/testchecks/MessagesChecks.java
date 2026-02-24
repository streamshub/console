package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.MessagesPageSelectors;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;

public class MessagesChecks {

    /**
     * Applies a Unix timestamp filter using the Messages page popover
     * and verifies the expected search results.
     *
     * <p>This method opens the filter popover in the Messages view,
     * selects the Unix timestamp filtering mode, enters the provided
     * timestamp value, and executes the search.</p>
     *
     * <p>After applying the filter, it validates:</p>
     * <ul>
     *   <li>The number of returned messages matches the expected count.</li>
     *   <li>If {@code expectedContent} is provided, the first result row
     *       contains the expected text.</li>
     * </ul>
     *
     * @param tcc             Test case configuration containing Playwright context
     * @param unixTimestamp   Unix epoch timestamp used for filtering messages
     * @param expectedCount   Expected number of messages returned after filtering
     * @param expectedContent Optional expected message content to verify in the first row;
     *                        may be {@code null} if content validation is not required
     */
    public static void checkPopoverUnixFilter(TestCaseConfig tcc, String unixTimestamp, int expectedCount, String expectedContent) {
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS).nth(3).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_UNIX_TIMESTAMP_INPUT, unixTimestamp);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, expectedCount, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        if (expectedContent != null) {
            PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), expectedContent, true);
        }
    }

    /**
     * Applies an ISO date/time filter using the Messages page popover
     * and verifies the expected search results.
     *
     * <p>This method opens the filter popover in the Messages view,
     * selects the ISO date/time filtering mode, fills in the provided
     * date and time values, and executes the search.</p>
     *
     * <p>After applying the filter, it validates:</p>
     * <ul>
     *   <li>The number of returned messages matches the expected count.</li>
     *   <li>If {@code expectedContent} is provided, the first result row
     *       contains the expected text.</li>
     * </ul>
     *
     * @param tcc             Test case configuration containing Playwright context
     * @param dateForm        Date value in {@code yyyy-MM-dd} format used for filtering
     * @param timeForm        Time value in UI-compatible format (e.g. {@code hh:mm a})
     * @param expectedCount   Expected number of messages returned after filtering
     * @param expectedContent Optional expected message content to verify in the first row;
     *                        may be {@code null} if content validation is not required
     */
    public static void checkPopoverIsoFilter(TestCaseConfig tcc, String dateForm, String timeForm, int expectedCount, String expectedContent) {
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS).nth(2).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_DATE_INPUT, dateForm);
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_TIME_INPUT, timeForm);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, expectedCount, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        if (expectedContent != null) {
            PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), expectedContent, true);
        }
    }

    /**
     * Applies a timestamp-based filter using the Messages page query bar
     * and verifies the expected search results.
     *
     * <p>This method fills the query input with the provided filter prefix
     * (e.g. ISO or Unix filter identifier) combined with the timestamp value,
     * executes the search, and validates the results.</p>
     *
     * <p>After applying the filter, it verifies:</p>
     * <ul>
     *   <li>The query input contains the expected timestamp value.</li>
     *   <li>The number of returned messages matches the expected count.</li>
     *   <li>If {@code expectedContent} is provided, the first result row
     *       contains the expected text.</li>
     * </ul>
     *
     * @param tcc             Test case configuration containing Playwright context
     * @param filterPrefix    Prefix identifying the filter type (e.g. ISO or Unix)
     * @param timestampValue  Timestamp value used for filtering
     * @param expectedCount   Expected number of messages returned after filtering
     * @param expectedContent Optional expected message content to verify in the first row;
     *                        may be {@code null} if content validation is not required
     */
    public static void checkQueryBarFilter(TestCaseConfig tcc, String filterPrefix, String timestampValue, int expectedCount, String expectedContent) {
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, filterPrefix + timestampValue);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, timestampValue, Constants.VALUE_ATTRIBUTE, true, true);
        PwUtils.waitForLocatorCount(tcc, expectedCount, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        if (expectedContent != null) {
            PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), expectedContent, true);
        }
    }
}
