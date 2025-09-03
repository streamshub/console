package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.locators.SingleConsumerGroupPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.microsoft.playwright.Locator;
import org.apache.logging.log4j.Logger;

public class ConsumerTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerTestUtils.class);

    private ConsumerTestUtils() {}

    /**
     * Selects the offset reset type in the consumer group reset UI.
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
    public static void selectResetOffsetType(TestCaseConfig tcc, ResetOffsetType offsetType, String value) {
        LOGGER.debug("Select reset offset type");
        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_DROPDOWN_BUTTON);

        switch (offsetType) {
            case EARLIEST:
                PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_EARLIEST_OFFSET);
                break;
            case LATEST:
                PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_LATEST_OFFSET);
                break;
            case DATE_TIME:
                Locator specificPartitionRadio = tcc.page().locator(SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_SELECTED_PARTITION_RADIO);
                if (specificPartitionRadio.isVisible() && specificPartitionRadio.getAttribute(Constants.CHECKED_ATTRIBUTE) != null) {
                    PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_SPECIFIC_PARTITION_SPECIFIC_DATETIME_OFFSET);
                } else {
                    PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_SPECIFIC_DATETIME_OFFSET);
                }
                break;
            case CUSTOM_OFFSET:
                PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET);
                PwUtils.waitForLocatorAndFill(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET_INPUT, value);
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
    public static void selectResetOffsetDatetimeTypeAndFill(TestCaseConfig tcc, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.debug("Select reset offset datetime type and fill value");
        switch (dateTimeType) {
            case UNIX_EPOCH:
                PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_EPOCH_FORMAT_RADIO);
                break;
            case ISO_8601:
                PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_ISO_FORMAT_RADIO);
                break;
        }
        PwUtils.waitForLocatorAndFill(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_INPUT, value);
    }

    /**
     * Selects and fills the reset offset parameters in the consumer group offset reset UI.
     * <p>
     * This method handles the UI interactions required to set the reset offset type
     * and associated parameters based on the provided input values. It delegates to helper
     * methods based on the type of offset:
     * <ul>
     *   <li>If {@code offsetType} is {@code DATE_TIME}, it selects the datetime type and fills the value field.</li>
     *   <li>If {@code offsetType} is {@code CUSTOM_OFFSET}, it fills the custom offset input field.</li>
     *   <li>For all types, it selects the correct offset type radio button or dropdown.</li>
     * </ul>
     *
     * @param tcc          the test case configuration containing the page context and cluster setup
     * @param offsetType   the type of offset reset to apply (e.g., {@code EARLIEST}, {@code LATEST}, {@code DATE_TIME}, {@code CUSTOM_OFFSET})
     * @param dateTimeType the type of datetime input format to use (only applicable when {@code offsetType} is {@code DATE_TIME})
     * @param value        the value to input (timestamp string or numeric offset, depending on {@code offsetType})
     */
    public static void selectResetOffsetParameters(TestCaseConfig tcc, ResetOffsetType offsetType, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.debug("Select reset offset parameters");
        selectResetOffsetType(tcc, offsetType, value);

        if (offsetType.equals(ResetOffsetType.DATE_TIME)) {
            selectResetOffsetDatetimeTypeAndFill(tcc, dateTimeType, value);
        }

        if (offsetType.equals(ResetOffsetType.CUSTOM_OFFSET)) {
            PwUtils.waitForLocatorAndFill(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET_INPUT, value);
        }
    }

    /**
     * Performs a dry-run and then executes a reset of consumer group offsets through the UI.
     * <p>
     * This method:
     * <ul>
     *     <li>Selects offset reset parameters (type, datetime, value) for the operation.</li>
     *     <li>Executes a dry-run via the UI and validates that the generated command contains the correct offset argument.</li>
     *     <li>Returns to the offset reset page and re-selects the parameters (since the UI resets them).</li>
     *     <li>Executes the actual offset reset operation through the UI.</li>
     * </ul>
     *
     * @param tcc           the test case configuration, including Playwright page and Kafka context
     * @param offsetType    the type of offset reset (e.g., earliest, latest, specific offset)
     * @param dateTimeType  the datetime type used if applicable (e.g., absolute or relative)
     * @param value         the value associated with the offset reset (e.g., a specific offset or timestamp)
     */
    public static void execDryRunAndReset(TestCaseConfig tcc, ResetOffsetType offsetType, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.info("DryRun reset offset - OffsetType {} DateTime {} reset value {}", offsetType, dateTimeType, value);
        selectResetOffsetParameters(tcc, offsetType, dateTimeType, value);

        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_DRY_RUN_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, SingleConsumerGroupPageSelectors.SCGPS_DRY_RUN_COMMAND, "--to-" + offsetType.getCommand(), Constants.VALUE_ATTRIBUTE, true);
        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_BACK_TO_EDIT_OFFSET_BUTTON);

        // Reselect offset since UI resets previously selected
        LOGGER.info("Reset offset - OffsetType {} DateTime {} reset value {}", offsetType, dateTimeType, value);
        selectResetOffsetParameters(tcc, offsetType, dateTimeType, value);
        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_OFFSET_RESET_BUTTON);
    }
}
