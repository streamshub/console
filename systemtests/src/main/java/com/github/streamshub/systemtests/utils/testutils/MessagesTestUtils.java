package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.enums.MessagesParameterType;
import com.github.streamshub.systemtests.enums.MessagesRetrieveType;
import com.github.streamshub.systemtests.enums.MessagesRetrieveLimit;
import com.github.streamshub.systemtests.enums.MessagesWhereFilter;
import com.github.streamshub.systemtests.locators.MessagesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class MessagesTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(MessagesTestUtils.class);
    private static final String ALL_PARTITIONS_LABEL = "All partitions";

    private MessagesTestUtils() {}

    /**
     * Opens the Messages page's popover filter form and waits for it to be ready for input.
     *
     * @param tcc test case configuration containing the Playwright page
     */
    public static void openFilterForm(TestCaseConfig tcc) {
        LOGGER.info("Opening Messages popover filter form");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT);
    }

    /**
     * Fills the "Has the words" input and verifies the value was applied.
     *
     * @param tcc  test case configuration containing the Playwright page
     * @param text the search terms to fill in
     */
    public static void fillHasWords(TestCaseConfig tcc, String text) {
        LOGGER.debug("Filling 'Has the words' input with [{}]", text);
        fillAndVerify(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, text);
    }

    /**
     * Selects the "Where" filter dimension (Anywhere/Key/Headers/Value) and verifies the dropdown reflects it.
     *
     * @param tcc   test case configuration containing the Playwright page
     * @param where the filter dimension to select
     */
    public static void selectWhere(TestCaseConfig tcc, MessagesWhereFilter where) {
        LOGGER.info("Selecting 'Where' filter [{}]", where.getLabel());
        selectMenuItem(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON, where.getLabel());
    }

    /**
     * Selects the "Messages" parameter type (From offset / From timestamp / From Unix timestamp / Latest messages)
     * and, where applicable, fills the secondary input that appears for that type.
     *
     * @param tcc   test case configuration containing the Playwright page
     * @param type  the Messages parameter type to select
     * @param value the value to fill into the type's secondary input; ignored for {@link MessagesParameterType#LATEST}.
     *              For {@link MessagesParameterType#FROM_TIMESTAMP} must be in {@code yyyy-MM-ddTHH:mm} format
     *              (the native {@code datetime-local} input's value format)
     */
    public static void selectMessagesParameter(TestCaseConfig tcc, MessagesParameterType type, String value) {
        LOGGER.info("Selecting Messages parameter [{}] with value [{}]", type.getLabel(), value);
        selectMenuItem(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_BUTTON, type.getLabel());

        switch (type) {
            case FROM_OFFSET, FROM_UNIX_TIMESTAMP ->
                fillAndVerify(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, value);
            case FROM_TIMESTAMP ->
                fillAndVerify(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_TIMESTAMP_INPUT, value);
            case LATEST -> LOGGER.debug("Messages parameter [Latest messages] has no secondary input to fill");
        }
    }

    /**
     * Selects the "Retrieve" type (Number of messages / Continuously) and, when {@code NUMBER_OF_MESSAGES}
     * is selected, also selects the given limit from the resulting sub-dropdown.
     *
     * @param tcc   test case configuration containing the Playwright page
     * @param type  the retrieve type to select
     * @param limit the number of messages to retrieve; only used (and required) when {@code type} is
     *              {@link MessagesRetrieveType#NUMBER_OF_MESSAGES}, otherwise ignored
     */
    public static void selectRetrieveType(TestCaseConfig tcc, MessagesRetrieveType type, MessagesRetrieveLimit limit) {
        LOGGER.info("Selecting Retrieve type [{}]{}", type.getLabel(), limit != null ? " with limit [" + limit.getLabel() + "]" : "");
        selectMenuItem(tcc, MessagesPageSelectors.MPS_TPF_RETRIEVE_DROPDOWN_BUTTON, type.getLabel());

        if (type == MessagesRetrieveType.NUMBER_OF_MESSAGES && limit != null) {
            selectMenuItem(tcc, MessagesPageSelectors.MPS_TPF_RETRIEVE_LIMIT_DROPDOWN_BUTTON, limit.getLabel());
        }
    }

    /**
     * Selects a partition filter.
     *
     * @param tcc            test case configuration containing the Playwright page
     * @param partitionIndex the partition number to select, or {@code null} to select "All partitions"
     */
    public static void selectPartition(TestCaseConfig tcc, Integer partitionIndex) {
        String label = partitionIndex == null ? ALL_PARTITIONS_LABEL : String.valueOf(partitionIndex);
        LOGGER.info("Selecting 'In partition' filter [{}]", label);
        selectMenuItem(tcc, MessagesPageSelectors.MPS_TPF_PARTITION_DROPDOWN_BUTTON, label);
    }

    /**
     * Submits the popover filter form.
     *
     * @param tcc test case configuration containing the Playwright page
     */
    public static void search(TestCaseConfig tcc) {
        LOGGER.debug("Submitting Messages filter form");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
    }

    /**
     * Resets the popover filter form back to its default state.
     *
     * @param tcc test case configuration containing the Playwright page
     */
    public static void resetFilters(TestCaseConfig tcc) {
        LOGGER.debug("Resetting Messages filter form");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_RESET_BUTTON);
    }

    /**
     * Opens a dropdown toggle, clicks the item matching the given label exactly (avoiding substring collisions
     * such as "5"/"50" or "1"/"10"), and verifies the toggle now displays that label.
     *
     * @param tcc            test case configuration containing the Playwright page
     * @param toggleSelector selector for the dropdown's toggle button
     * @param exactLabel     the exact, case-sensitive label of the item to select
     *
     * @throws AssertionError if the toggle does not display the expected label after selection
     */
    private static void selectMenuItem(TestCaseConfig tcc, String toggleSelector, String exactLabel) {
        PwUtils.waitForLocatorAndClick(tcc, toggleSelector);

        // Playwright serializes this Pattern's source to a JavaScript RegExp on the browser side, so it must be
        // escaped in a JS-regex-safe way - Java's Pattern.quote() wraps in \Q...\E, which JS regex doesn't
        // understand, silently producing a pattern that can never match anything.
        Pattern exactMatch = Pattern.compile("^" + escapeForJsRegex(exactLabel) + "$");
        Locator item = tcc.page().locator(MessagesPageSelectors.MPS_TPF_OPEN_MENU_ITEM_BUTTONS)
            .filter(new Locator.FilterOptions().setHasText(exactMatch));
        PwUtils.waitForLocatorAndClick(item);

        String toggleText = PwUtils.getTrimmedText(tcc.page().locator(toggleSelector).innerText());
        if (!toggleText.contains(exactLabel)) {
            LOGGER.error("Dropdown [{}] shows [{}], expected [{}]", toggleSelector, toggleText, exactLabel);
            throw new AssertionError("Dropdown [" + toggleSelector + "] expected to show [" + exactLabel + "] but showed [" + toggleText + "]");
        }
    }

    /**
     * Escapes regex metacharacters using backslash-escaping valid in both Java and JavaScript regex syntax
     * (unlike {@link Pattern#quote(String)}, which uses Java-only {@code \Q...\E} quoting that JavaScript's
     * RegExp does not understand - Playwright sends the pattern's source to the browser as a JS RegExp).
     *
     * @param text the literal text to escape
     * @return the escaped text, safe to embed in a regex matched by Playwright's browser-side engine
     */
    private static String escapeForJsRegex(String text) {
        return text.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }

    /**
     * Fills an input and verifies the resulting value matches exactly.
     *
     * @param tcc      test case configuration containing the Playwright page
     * @param selector selector for the input to fill
     * @param value    the value to fill and verify
     *
     * @throws AssertionError if the input's value doesn't match after filling
     */
    private static void fillAndVerify(TestCaseConfig tcc, String selector, String value) {
        PwUtils.waitForLocatorAndFill(tcc, selector, value);

        String actual = tcc.page().locator(selector).inputValue();
        if (!value.equals(actual)) {
            LOGGER.error("Input [{}] has value [{}], expected [{}]", selector, actual, value);
            throw new AssertionError("Input [" + selector + "] expected value [" + value + "] but was [" + actual + "]");
        }
    }
}
