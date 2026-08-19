package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.enums.MessagesParameterType;
import com.github.streamshub.systemtests.enums.MessagesRetrieveType;
import com.github.streamshub.systemtests.enums.MessagesRetrieveLimit;
import com.github.streamshub.systemtests.enums.MessagesWhereFilter;
import com.github.streamshub.systemtests.locators.pages.MessagesPage;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.apache.logging.log4j.Logger;

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
        PwUtils.waitForLocatorAndClick(MessagesPage.advancedSearchToggle(tcc.page()));
        PwUtils.waitForLocatorVisible(MessagesPage.hasWordsInput(tcc.page()));
    }

    /**
     * Fills the "Has the words" input and verifies the value was applied.
     *
     * @param tcc  test case configuration containing the Playwright page
     * @param text the search terms to fill in
     */
    public static void fillHasWords(TestCaseConfig tcc, String text) {
        LOGGER.debug("Filling 'Has the words' input with [{}]", text);
        fillAndVerify(MessagesPage.hasWordsInput(tcc.page()), text);
    }

    /**
     * Selects the "Where" filter dimension (Anywhere/Key/Headers/Value) and verifies the dropdown reflects it.
     *
     * @param tcc   test case configuration containing the Playwright page
     * @param where the filter dimension to select
     */
    public static void selectWhere(TestCaseConfig tcc, MessagesWhereFilter where) {
        LOGGER.info("Selecting 'Where' filter [{}]", where.getLabel());
        selectMenuItem(tcc.page(), MessagesPage.whereDropdownButton(tcc.page()), where.getLabel());
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
        selectMenuItem(tcc.page(), MessagesPage.messagesFromDropdownButton(tcc.page()), type.getLabel());

        switch (type) {
            case FROM_OFFSET, FROM_UNIX_TIMESTAMP ->
                fillAndVerify(MessagesPage.offsetInput(tcc.page()), value);
            case FROM_TIMESTAMP ->
                fillAndVerify(MessagesPage.timestampInput(tcc.page()), value);
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
        selectMenuItem(tcc.page(), MessagesPage.retrieveTypeDropdownButton(tcc.page()), type.getLabel());

        if (type == MessagesRetrieveType.NUMBER_OF_MESSAGES && limit != null) {
            selectMenuItem(tcc.page(), MessagesPage.retrieveLimitDropdownButton(tcc.page()), limit.getLabel());
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
        selectMenuItem(tcc.page(), MessagesPage.partitionDropdownButton(tcc.page()), label);
    }

    /**
     * Submits the popover filter form.
     *
     * @param tcc test case configuration containing the Playwright page
     */
    public static void search(TestCaseConfig tcc) {
        LOGGER.debug("Submitting Messages filter form");
        PwUtils.waitForLocatorAndClick(MessagesPage.formSearchButton(tcc.page()));
    }

    /**
     * Resets the popover filter form back to its default state.
     *
     * @param tcc test case configuration containing the Playwright page
     */
    public static void resetFilters(TestCaseConfig tcc) {
        LOGGER.debug("Resetting Messages filter form");
        PwUtils.waitForLocatorAndClick(MessagesPage.formResetButton(tcc.page()));
    }

    /**
     * Opens a dropdown toggle, clicks the item matching the given label exactly (avoiding substring collisions
     * such as "5"/"50" or "1"/"10"), and verifies the toggle now displays that label.
     *
     * @param page       the Playwright page used to resolve the shared open-menu-item locator
     * @param toggle     the dropdown's toggle button
     * @param exactLabel the exact, case-sensitive label of the item to select
     *
     * @throws AssertionError if the toggle does not display the expected label after selection
     */
    private static void selectMenuItem(Page page, Locator toggle, String exactLabel) {
        PwUtils.waitForLocatorAndClick(toggle);
        PwUtils.waitForLocatorAndClick(MessagesPage.openMenuItem(page, exactLabel));

        String toggleText = PwUtils.getTrimmedText(toggle.innerText());
        if (!toggleText.contains(exactLabel)) {
            LOGGER.error("Dropdown shows [{}], expected [{}]", toggleText, exactLabel);
            throw new AssertionError("Dropdown expected to show [" + exactLabel + "] but showed [" + toggleText + "]");
        }
    }

    /**
     * Fills an input and verifies the resulting value matches exactly.
     *
     * @param input the input to fill
     * @param value the value to fill and verify
     *
     * @throws AssertionError if the input's value doesn't match after filling
     */
    private static void fillAndVerify(Locator input, String value) {
        PwUtils.waitForLocatorAndFill(input, value);

        String actual = input.inputValue();
        if (!value.equals(actual)) {
            LOGGER.error("Input has value [{}], expected [{}]", actual, value);
            throw new AssertionError("Input expected value [" + value + "] but was [" + actual + "]");
        }
    }
}
