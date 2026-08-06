/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.components;

import com.github.streamshub.systemtests.locators.IdConstants;
import com.github.streamshub.systemtests.locators.Locators;
import com.github.streamshub.systemtests.locators.OuiaIdConstants;
import com.github.streamshub.systemtests.locators.TextConstants;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** App-chrome locators shared by every page (masthead toolbar, cluster switcher, user menu). */
public final class Masthead {
    private Masthead() {}

    private static Locator toolbar(Page page) {
        return page.locator(IdConstants.MASTHEAD_TOOLBAR);
    }

    /** The "Kafka Clusters" toggle button (ClusterSwitcher.tsx) that opens the cluster-switch menu. */
    public static Locator clusterSwitcherButton(Page page) {
        return Locators.buttonNamed(toolbar(page), TextConstants.KAFKA_CLUSTERS);
    }

    /** The badge showing the total available Kafka cluster count, nested inside the switcher button. */
    public static Locator totalAvailableKafkaCount(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.TOTAL_AVAILABLE_KAFKA_COUNT);
    }

    /** Cluster entries in the open cluster-switcher menu (portaled to the page root). */
    public static Locator clusterListItems(Page page) {
        return Locators.menuItems(page);
    }

    /**
     * The logged-in user's dropdown toggle. Identified by the avatar's {@code alt}
     * attribute (UserDropdown.tsx sets {@code alt={username}}), which is real test
     * data (the username under assertion), not UI chrome text.
     */
    public static Locator userDropdownButton(Page page, String username) {
        return toolbar(page).locator("button").filter(new Locator.FilterOptions().setHas(page.getByAltText(username)));
    }

    public static Locator logoutMenuItem(Page page) {
        return page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(TextConstants.LOGOUT));
    }
}
