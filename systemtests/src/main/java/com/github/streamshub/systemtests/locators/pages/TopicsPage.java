/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.pages;

import com.github.streamshub.systemtests.locators.IdConstants;
import com.github.streamshub.systemtests.locators.Locators;
import com.github.streamshub.systemtests.locators.OuiaIdConstants;
import com.github.streamshub.systemtests.locators.TextConstants;
import com.github.streamshub.systemtests.locators.components.ResourceTable;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class TopicsPage {
    private TopicsPage() {}

    public static ResourceTable table(Page page) {
        return ResourceTable.byOuiaId(page, OuiaIdConstants.TOPICS_TABLE);
    }

    public static Locator totalTopicsBadge(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.TOPICS_TOTAL_BADGE);
    }

    public static Locator fullyReplicatedBadge(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.TOPICS_FULLY_REPLICATED_BADGE);
    }

    public static Locator underReplicatedBadge(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.TOPICS_UNDER_REPLICATED_BADGE);
    }

    public static Locator offlineBadge(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.TOPICS_OFFLINE_BADGE);
    }

    public static Locator searchInput(Page page) {
        // TopicsFilterToolbar.tsx sets id={`${selectedFilterType}-filter-input`}; "name" is the default filter type.
        return page.locator("input[id$='" + IdConstants.FILTER_INPUT_ID_SUFFIX + "']");
    }

    public static Locator searchButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.SEARCH);
    }

    public static Locator clearAllFiltersButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.CLEAR_ALL_FILTERS);
    }

    public static Locator filterTypeDropdownButton(Page page) {
        return page.locator(IdConstants.FILTER_TYPE_SELECT);
    }

    public static Locator filterTypeDropdownItems(Page page) {
        return Locators.menuItems(page);
    }

    public static Locator statusFilterDropdownButton(Page page) {
        return page.locator(IdConstants.STATUS_FILTER_SELECT);
    }

    public static Locator statusFilterDropdownItems(Page page) {
        return Locators.menuItems(page);
    }

    public static Locator noResultsFound(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(TextConstants.NO_RESULTS_FOUND));
    }

    public static Locator notAuthorizedContent(Page page) {
        return page.getByText(TextConstants.NOT_AUTHORIZED);
    }

    public static Locator sortByNameHeader(Page page) {
        return page.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName(TextConstants.NAME));
    }

    public static Locator sortByStorageHeader(Page page) {
        return page.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName(TextConstants.STORAGE));
    }

    public static Locator paginationDropdownButton(Page page, boolean top) {
        return page.locator(top ? IdConstants.PAGINATION_TOP_TOGGLE : IdConstants.PAGINATION_BOTTOM_TOGGLE);
    }

    public static Locator paginationDropdownItems(Page page) {
        return Locators.menuItems(page);
    }

    public static Locator paginationPrevButton(Page page, boolean top) {
        return Locators.buttonNamed(page, TextConstants.GO_TO_PREVIOUS_PAGE).nth(top ? 0 : 1);
    }

    public static Locator paginationNextButton(Page page, boolean top) {
        return Locators.buttonNamed(page, TextConstants.GO_TO_NEXT_PAGE).nth(top ? 0 : 1);
    }

    /** Link to the first group in the "Groups reading from this topic" table on the Topic Groups tab. */
    public static Locator groupsTabFirstGroupLink(Page page) {
        return ResourceTable.byOuiaId(page, OuiaIdConstants.TOPIC_GROUPS_LISTING).rows().first().locator("a");
    }
}
