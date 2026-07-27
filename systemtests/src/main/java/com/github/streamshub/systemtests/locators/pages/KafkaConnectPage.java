/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.pages;

import com.github.streamshub.systemtests.locators.TextConstants;
import com.github.streamshub.systemtests.locators.components.ResourceTable;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class KafkaConnectPage {
    private KafkaConnectPage() {}

    public static Locator connectorsTab(Page page) {
        return page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(TextConstants.CONNECTORS));
    }

    public static Locator connectClustersTab(Page page) {
        return page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(TextConstants.CLUSTERS));
    }

    public static Locator pageTitle(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1).setName(TextConstants.KAFKA_CONNECT));
    }

    /** Both the Connectors and Connect Clusters tabs render the same SearchInput shape with only the placeholder differing, and share the PatternFly default aria-label ("Search input"). */
    public static Locator nameFilterInput(Page page) {
        return page.getByLabel(TextConstants.SEARCH_INPUT);
    }

    public static ResourceTable table(Page page, boolean connectors) {
        return ResourceTable.byAccessibleName(page, connectors ? TextConstants.CONNECTORS_TABLE : TextConstants.CONNECT_CLUSTERS_TABLE);
    }
}
