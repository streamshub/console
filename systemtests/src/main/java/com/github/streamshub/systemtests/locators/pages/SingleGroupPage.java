/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.pages;

import com.github.streamshub.systemtests.locators.IdConstants;
import com.github.streamshub.systemtests.locators.Locators;
import com.github.streamshub.systemtests.locators.OuiaIdConstants;
import com.github.streamshub.systemtests.locators.TextConstants;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class SingleGroupPage {
    private SingleGroupPage() {}

    public static Locator heading(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
    }

    public static Locator breadcrumbGroupName(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.BREADCRUMB_GROUP_NAME);
    }

    public static Locator resetOffsetButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.RESET_OFFSETS);
    }

    public static Locator topicDropdownButton(Page page) {
        return page.locator(IdConstants.TOPIC_SELECT);
    }

    public static Locator topicDropdownSearchInput(Page page) {
        return page.getByPlaceholder(TextConstants.SEARCH);
    }

    public static Locator topicDropdownResult(Page page, String topicName) {
        return page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(topicName));
    }

    public static Locator offsetTypeDropdownButton(Page page) {
        return page.locator(IdConstants.OFFSET_SELECT);
    }

    public static Locator earliestOffsetOption(Page page) {
        return page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(TextConstants.EARLIEST_OFFSET));
    }

    public static Locator latestOffsetOption(Page page) {
        return page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(TextConstants.LATEST_OFFSET));
    }

    public static Locator specificDateTimeIsoOption(Page page) {
        return page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(TextConstants.SPECIFIC_DATETIME_ISO));
    }

    public static Locator specificDateTimeUnixOption(Page page) {
        return page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(TextConstants.SPECIFIC_DATETIME_UNIX));
    }

    public static Locator deleteCommittedOffsetsOption(Page page) {
        return page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(TextConstants.DELETE_COMMITTED_OFFSETS));
    }

    public static Locator datetimeInput(Page page) {
        return page.locator(IdConstants.DATETIME_INPUT);
    }

    public static Locator resetButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.RESET_OFFSETS, true);
    }

    public static Locator dryRunButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.DRY_RUN);
    }

    public static Locator dryRunCommandToggle(Page page) {
        return page.getByText(TextConstants.CLI_COMMAND);
    }

    public static Locator dryRunCommandText(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.DRY_RUN_COMMAND_TEXT);
    }

    /** ResetOffsetModal and DryRunResults are two stacked, simultaneously-open dialogs; .last() targets the topmost one. */
    public static Locator dryRunCancelButton(Page page) {
        return Locators.buttonNamed(page.getByRole(AriaRole.DIALOG).last(), TextConstants.CANCEL);
    }
}
