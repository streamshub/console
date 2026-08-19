package com.github.streamshub.systemtests.utils.testchecks;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.MessagesParameterType;
import com.github.streamshub.systemtests.locators.pages.MessagesPage;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.testutils.MessagesTestUtils;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class MessagesChecks {
    private static final Logger LOGGER = LogWrapper.getLogger(MessagesChecks.class);

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
        LOGGER.info("Checking popover Unix timestamp filter [{}] returns {} message(s)", unixTimestamp, expectedCount);
        MessagesTestUtils.openFilterForm(tcc);
        MessagesTestUtils.selectMessagesParameter(tcc, MessagesParameterType.FROM_UNIX_TIMESTAMP, unixTimestamp);
        MessagesTestUtils.search(tcc);
        PwUtils.waitForLocatorCount(expectedCount, MessagesPage.table(tcc.page()).rows(), true);
        if (expectedContent != null) {
            LOGGER.debug("Verifying first search result row contains [{}]", expectedContent);
            PwUtils.waitForContainsText(MessagesPage.cellAt(tcc.page(), 1, 5), expectedContent, true);
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
     * @param isoDateTime     Date/time value in {@code yyyy-MM-dd'T'HH:mm} format (the native
     *                        {@code datetime-local} input's value format) used for filtering
     * @param expectedCount   Expected number of messages returned after filtering
     * @param expectedContent Optional expected message content to verify in the first row;
     *                        may be {@code null} if content validation is not required
     */
    public static void checkPopoverIsoFilter(TestCaseConfig tcc, String isoDateTime, int expectedCount, String expectedContent) {
        LOGGER.info("Checking popover ISO date/time filter [{}] returns {} message(s)", isoDateTime, expectedCount);
        MessagesTestUtils.openFilterForm(tcc);
        MessagesTestUtils.selectMessagesParameter(tcc, MessagesParameterType.FROM_TIMESTAMP, isoDateTime);
        MessagesTestUtils.search(tcc);
        PwUtils.waitForLocatorCount(expectedCount, MessagesPage.table(tcc.page()).rows(), true);
        if (expectedContent != null) {
            LOGGER.debug("Verifying first search result row contains [{}]", expectedContent);
            PwUtils.waitForContainsText(MessagesPage.cellAt(tcc.page(), 1, 5), expectedContent, true);
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
        LOGGER.info("Checking query bar filter [{}{}] returns {} message(s)", filterPrefix, timestampValue, expectedCount);
        PwUtils.waitForLocatorVisible(MessagesPage.searchInput(tcc.page()));
        PwUtils.waitForLocatorAndFill(MessagesPage.searchInput(tcc.page()), filterPrefix + timestampValue);
        PwUtils.waitForLocatorAndClick(MessagesPage.searchSubmitButton(tcc.page()));
        PwUtils.waitForAttributeContainsText(MessagesPage.searchInput(tcc.page()), timestampValue, Constants.VALUE_ATTRIBUTE, true, true);
        PwUtils.waitForLocatorCount(expectedCount, MessagesPage.table(tcc.page()).rows(), true);
        if (expectedContent != null) {
            LOGGER.debug("Verifying first search result row contains [{}]", expectedContent);
            PwUtils.waitForContainsText(MessagesPage.cellAt(tcc.page(), 1, 5), expectedContent, true);
        }
    }

    /**
     * Verifies the outcome of a popover filter-form submission: that the resulting query-bar value matches
     * what's expected, that the expected number of result rows are shown, and that each given locator
     * contains its expected text.
     *
     * @param tcc                 Test case configuration containing Playwright context
     * @param expectedQueryValue  The expected value of the search query-bar input after submitting the form
     *                            (e.g. {@code "messages=offset:95 retrieve=50 orderID where=key"})
     * @param expectedCount       Expected number of messages returned after filtering
     * @param rowChecks           Map of locator to expected text, each verified against the result table
     *                            (e.g. a specific row/column cell)
     */
    public static void checkFilterResults(TestCaseConfig tcc, String expectedQueryValue, int expectedCount, Map<Locator, String> rowChecks) {
        LOGGER.info("Checking filter results: query=[{}], expecting {} row(s)", expectedQueryValue, expectedCount);
        PwUtils.waitForAttributeContainsText(MessagesPage.searchInput(tcc.page()), expectedQueryValue, Constants.VALUE_ATTRIBUTE, true, true);
        PwUtils.waitForLocatorCount(expectedCount, MessagesPage.table(tcc.page()).rows(), true);
        rowChecks.forEach((locator, expectedText) -> {
            LOGGER.debug("Checking locator [{}] contains expected value [{}]", locator, expectedText);
            PwUtils.waitForContainsText(locator, expectedText, true);
        });
    }
}
