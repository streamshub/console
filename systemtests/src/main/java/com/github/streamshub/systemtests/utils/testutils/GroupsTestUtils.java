package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.locators.GroupsPageSelectors;
import com.github.streamshub.systemtests.locators.SingleGroupPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class GroupsTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(GroupsTestUtils.class);

    private GroupsTestUtils() {}

    /**
     * Selects the offset reset type in the group reset UI.
     * <p>
     * This method handles the dropdown selection of various offset types such as:
     * {@code EARLIEST}, {@code LATEST}, {@code DATE_TIME}, and {@code CUSTOM_OFFSET}.
     * For {@code DATE_TIME}, it determines whether a specific partition radio button is selected
     * and chooses the appropriate option accordingly. For {@code CUSTOM_OFFSET}, it also fills in the input field with the provided value.
     *
     * @param tcc        the test case configuration containing the page context
     * @param offsetType the type of offset to select (e.g., EARLIEST, LATEST, DATE_TIME, CUSTOM_OFFSET)
     * @param value      the value to fill in, used only for {@code CUSTOM_OFFSET} (and reused elsewhere for consistency)
     */
    public static void selectResetOffsetType(TestCaseConfig tcc, ResetOffsetType offsetType, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.info("Selecting reset offset type {}", offsetType);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_DROPDOWN_BUTTON);

        switch (offsetType) {
            case EARLIEST:
                PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_EARLIEST_OFFSET);
                break;
            case LATEST:
                PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_LATEST_OFFSET);
                break;
            case DATE_TIME_ISO:
                PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_SPECIFIC_DATETIME_OFFSET_ISO);
                fillResetOffsetDatetime(tcc, dateTimeType, value);
                break;
            case DATE_TIME_UNIX:
                PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_SPECIFIC_DATETIME_OFFSET_UNIX);
                fillResetOffsetDatetime(tcc, dateTimeType, value);
                break;
            case DELETE_COMMITED_OFFSETS:
                PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_DELETE_COMMITED_OFFSETS);
                break;
        }
    }

    /**
     * Selects the datetime input format (Unix epoch or ISO 8601) for resetting consumer group offsets,
     * and fills in the corresponding datetime value.
     * <p>
     * This method is used when the reset offset type is {@code DATE_TIME}. It first selects
     * the appropriate radio button based on the specified {@code dateTimeType}, then fills
     * the input field with the provided value.
     *
     * @param tcc          the test case configuration containing the page and context
     * @param dateTimeType the format of the datetime input ({@code UNIX_EPOCH} or {@code ISO_8601})
     * @param value        the datetime value to enter into the input field
     */
    public static void fillResetOffsetDatetime(TestCaseConfig tcc, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.debug("Selecting reset offset datetime type {} and filling value {}", dateTimeType, value);
        switch (dateTimeType) {
            case UNIX_EPOCH:
                PwUtils.waitForLocatorAndFill(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_INPUT_UNIX, value);
                break;
            case ISO_8601:
                PwUtils.waitForLocatorAndFill(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_INPUT_ISO, value);
                break;
        }
    }

    /**
     * Performs a dry-run reset of consumer group offsets through the UI.
     * <p>
     * This method:
     * <ul>
     *     <li>Selects offset reset parameters (type, datetime, value) for the operation.</li>
     *     <li>Executes a dry-run via the UI and validates that the generated command contains the correct offset argument.</li>
     *     <li>Returns to the offset reset page and re-selects the parameters (since the UI resets them).</li>
     * </ul>
     *
     * @param tcc           the test case configuration, including Playwright page and Kafka context
     * @param offsetType    the type of offset reset (e.g., earliest, latest, specific offset)
     * @param dateTimeType  the datetime type used if applicable (e.g., absolute or relative)
     * @param value         the value associated with the offset reset (e.g., a specific offset or timestamp)
     */
    public static void execDryRun(TestCaseConfig tcc, ResetOffsetType offsetType, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.info("DryRun reset offset - OffsetType {} DateTime {} reset value {}", offsetType, dateTimeType, value);
        selectResetOffsetType(tcc, offsetType, dateTimeType, value);

        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_DRY_RUN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_DRY_RUN_COMMAND_DROPDOWN);
        PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_DRY_RUN_COMMAND, offsetType.getCommand(), false, false);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_CANCEL_DRY_RUN_OFFSET_BUTTON);
    }

    /**
     * Performs a reset of consumer group offsets through the UI.
     * <p>
     * This method:
     * <ul>
     *     <li>Selects offset reset parameters (type, datetime, value) for the operation.</li>
     *     <li>Executes the actual offset reset operation through the UI.</li>
     * </ul>
     *
     * @param tcc           the test case configuration, including Playwright page and Kafka context
     * @param offsetType    the type of offset reset (e.g., earliest, latest, specific offset)
     * @param dateTimeType  the datetime type used if applicable (e.g., absolute or relative)
     * @param value         the value associated with the offset reset (e.g., a specific offset or timestamp)
     */
    public static void execResetOffset(TestCaseConfig tcc, ResetOffsetType offsetType, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.info("Reset offset - OffsetType {} DateTime {} reset value {}", offsetType, dateTimeType, value);
        selectResetOffsetType(tcc, offsetType, dateTimeType, value);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_OFFSET_RESET_BUTTON);
    }

    /**
     * Verifies that a consumer group with the given name is present in the Groups table.
     *
     * @param tcc               test case configuration containing the Playwright page
     * @param consumerGroupName name of the consumer group expected to be visible
     *
     * @throws AssertionError if the consumer group is not found in the table
     */
    public static void waitForGroupInTable(TestCaseConfig tcc, String consumerGroupName) {
        LOGGER.info("Verifying consumer group {} is present in Groups table", consumerGroupName);
        List<String> visibleGroups = tcc.page().locator(GroupsPageSelectors.GPS_TABLE_ITEMS)
            .all()
            .stream()
            .map(locator -> PwUtils.getTrimmedText(locator.allInnerTexts().toString()))
            .toList();

        if (visibleGroups.stream().noneMatch(group -> group.contains(consumerGroupName))) {
            LOGGER.error("Consumer group {} not found in Groups table, visible groups: {}", consumerGroupName, visibleGroups);
            throw new AssertionError("Consumer group not found in UI, expected: " + consumerGroupName +
                " - Visible groups: " + visibleGroups);
        }
    }

    /**
     * Clicks the consumer group with the given name in the Groups table.
     *
     * @param tcc               test case configuration containing the Playwright page
     * @param consumerGroupName name of the consumer group to open
     */
    public static void clickGroupInTable(TestCaseConfig tcc, String consumerGroupName) {
        LOGGER.info("Opening consumer group {} from Groups table", consumerGroupName);
        PwUtils.waitForLocatorAndClick(tcc.page().locator(GroupsPageSelectors.GPS_TABLE_ITEMS)
            .filter(new Locator.FilterOptions().setHasText(consumerGroupName))
            .locator("a")
            .first());
    }
}
