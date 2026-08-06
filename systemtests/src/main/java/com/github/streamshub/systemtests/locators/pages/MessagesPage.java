/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.pages;

import com.github.streamshub.systemtests.locators.Locators;
import com.github.streamshub.systemtests.locators.OuiaIdConstants;
import com.github.streamshub.systemtests.locators.TextConstants;
import com.github.streamshub.systemtests.locators.components.ResourceTable;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class MessagesPage {
    private MessagesPage() {}

    public static ResourceTable table(Page page) {
        return ResourceTable.byAccessibleName(page, TextConstants.MESSAGES_TABLE);
    }

    /** The topic detail page's h1 title, showing the current topic name. */
    public static Locator pageTitle(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
    }

    /**
     * The message table's visible columns are user-configurable (ColumnsModal), so unlike other
     * tables a stable data-label can't be assigned to a fixed column index - this stays positional,
     * matching the table's own dynamic-column behavior rather than introducing a false stable identity.
     * nthRow/nthColumn are 1-based, matching the rest of this codebase's row/column addressing.
     */
    public static Locator cellAt(Page page, int nthRow, int nthColumn) {
        return table(page).rows().nth(nthRow - 1).locator("td").nth(nthColumn - 1);
    }

    public static Locator emptyBodyContent(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(TextConstants.NO_MESSAGES_DATA));
    }

    public static Locator searchInput(Page page) {
        return page.getByLabel(TextConstants.SEARCH_INPUT);
    }

    public static Locator advancedSearchToggle(Page page) {
        return page.getByLabel(TextConstants.OPEN_ADVANCED_SEARCH);
    }

    public static Locator searchSubmitButton(Page page) {
        return page.getByLabel(TextConstants.SEARCH, new Page.GetByLabelOptions().setExact(true));
    }

    public static Locator hasWordsInput(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.HAS_WORDS_INPUT);
    }

    public static Locator whereDropdownButton(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.WHERE_DROPDOWN_BUTTON);
    }

    public static Locator messagesFromDropdownButton(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.MESSAGES_FROM_DROPDOWN_BUTTON);
    }

    /** Exact-match menu item lookup - PatternFly portals every open Select menu to the same DOM position, and numeric option labels (e.g. "5" vs "50") collide under substring matching, so this must stay an exact match. */
    public static Locator openMenuItem(Page page, String exactLabel) {
        return page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(exactLabel).setExact(true));
    }

    public static Locator offsetInput(Page page) {
        return page.getByLabel(TextConstants.SPECIFY_OFFSET);
    }

    public static Locator timestampInput(Page page) {
        return page.getByLabel(TextConstants.SPECIFY_TIMESTAMP);
    }

    public static Locator unixTimestampInput(Page page) {
        return page.getByLabel(TextConstants.SPECIFY_UNIX_TIMESTAMP);
    }

    public static Locator retrieveTypeDropdownButton(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.RETRIEVE_TYPE_DROPDOWN_BUTTON);
    }

    public static Locator retrieveLimitDropdownButton(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.RETRIEVE_LIMIT_DROPDOWN_BUTTON);
    }

    public static Locator partitionDropdownButton(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.PARTITION_DROPDOWN_BUTTON);
    }

    public static Locator formSearchButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.SEARCH, true);
    }

    public static Locator formResetButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.RESET, true);
    }

    public static Locator sidebarValueFormat(Page page) {
        return page.locator(".pf-v6-c-description-list__group").filter(new Locator.FilterOptions().setHas(page.getByText(TextConstants.VALUE_FORMAT, new Page.GetByTextOptions().setExact(true)))).locator(".pf-v6-c-description-list__description");
    }

    public static Locator sidebarSchemaName(Page page) {
        return page.locator("[role='tabpanel']").getByRole(AriaRole.PARAGRAPH);
    }

    public static Locator sidebarSchemaCode(Page page) {
        return page.locator("[role='tabpanel'] .pf-v6-c-code-block__code");
    }
}
