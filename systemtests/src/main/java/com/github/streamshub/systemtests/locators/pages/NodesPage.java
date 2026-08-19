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

public final class NodesPage {
    private NodesPage() {}

    public static ResourceTable table(Page page) {
        return ResourceTable.byOuiaId(page, OuiaIdConstants.NODES_LISTING);
    }

    public static Locator totalCountBadge(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.NODES_TOTAL_BADGE);
    }

    public static Locator workingNodesBadge(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.NODES_HEALTHY_BADGE);
    }

    public static Locator warningNodesBadge(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.NODES_UNHEALTHY_BADGE);
    }

    public static Locator totalNodesCount(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.OVERVIEW_TOTAL_NODES);
    }

    public static Locator controllerRoleCount(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.OVERVIEW_CONTROLLER_ROLE);
    }

    public static Locator brokerRoleCount(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.OVERVIEW_BROKER_ROLE);
    }

    public static Locator leadControllerCount(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.OVERVIEW_LEAD_CONTROLLER);
    }

    public static Locator nodePoolFilterButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.NODE_POOL);
    }

    public static Locator roleFilterButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.ROLES);
    }

    public static Locator statusFilterButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.STATUS);
    }

    public static Locator filterMenuItems(Page page) {
        return Locators.menuItems(page);
    }

    public static Locator clearAllFiltersButton(Page page) {
        return Locators.buttonNamed(page, TextConstants.CLEAR_ALL_FILTERS);
    }

    public static Locator notAuthorizedContent(Page page) {
        return page.getByText(TextConstants.FORBIDDEN_STATUS_CODE);
    }

    public static ResourceTable rebalanceTable(Page page) {
        return ResourceTable.byAccessibleName(page, TextConstants.REBALANCES_TABLE);
    }

    public static Locator rebalanceExpandToggle(Locator row) {
        return Locators.buttonNamed(row, TextConstants.DETAILS);
    }

    public static Locator rebalanceActionsMenuButton(Locator row) {
        return Locators.buttonNamed(row, TextConstants.ACTIONS);
    }

    public static Locator rebalanceMenuItem(Page page, String name) {
        return page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(name));
    }

    public static Locator rebalanceAutoApprovalValue(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.REBALANCE_AUTO_APPROVAL_VALUE);
    }

    public static Locator rebalanceModeValue(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.REBALANCE_MODE_VALUE);
    }

    public static Locator rebalanceDataToMoveMb(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.REBALANCE_DATA_TO_MOVE_MB);
    }

    public static Locator rebalanceMonitoredPartitionsPercentage(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.REBALANCE_MONITORED_PARTITIONS_PERCENTAGE);
    }

    public static Locator rebalanceNumReplicaMovements(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.REBALANCE_NUM_REPLICA_MOVEMENTS);
    }

    public static Locator rebalanceBalancednessBefore(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.REBALANCE_BALANCEDNESS_BEFORE);
    }

    public static Locator rebalanceBalancednessAfter(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.REBALANCE_BALANCEDNESS_AFTER);
    }
}
