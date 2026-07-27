/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.pages;

import com.github.streamshub.systemtests.locators.OuiaIdConstants;
import com.github.streamshub.systemtests.locators.TextConstants;
import com.github.streamshub.systemtests.locators.components.ResourceTable;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class GroupsPage {
    private GroupsPage() {}

    public static Locator heading(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1).setName(TextConstants.GROUPS));
    }

    /** No id/aria-label on GroupsPage.tsx's SearchInput; only the placeholder text is stable. */
    public static Locator searchInput(Page page) {
        return page.getByPlaceholder(TextConstants.FILTER_BY_NAME);
    }

    public static ResourceTable table(Page page) {
        return ResourceTable.byOuiaId(page, OuiaIdConstants.GROUPS_LISTING);
    }

    public static Locator noGroupsAvailable(Page page) {
        return page.getByText(TextConstants.NO_GROUPS_AVAILABLE);
    }
}
