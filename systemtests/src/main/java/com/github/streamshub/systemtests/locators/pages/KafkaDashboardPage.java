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

public final class KafkaDashboardPage {
    private KafkaDashboardPage() {}

    public static ResourceTable table(Page page) {
        return ResourceTable.byOuiaId(page, OuiaIdConstants.KAFKA_CLUSTERS_TABLE);
    }

    /** The "View" button in the row for the named cluster, found by matching the cluster name itself rather than a row index. */
    public static Locator viewButton(Page page, String clusterName) {
        return Locators.buttonNamed(table(page).row(clusterName), TextConstants.VIEW);
    }
}
