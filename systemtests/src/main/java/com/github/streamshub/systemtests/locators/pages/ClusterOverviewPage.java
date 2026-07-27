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

import java.util.regex.Pattern;

/** Locators for the Kafka cluster Overview page. */
public final class ClusterOverviewPage {
    private ClusterOverviewPage() {}

    public static Locator pauseReconciliationButton(Page page) {
        return Locators.buttonNamed(page, Pattern.compile(TextConstants.PAUSE_OR_RESUME_RECONCILIATION_PATTERN));
    }

    public static Locator reconciliationPausedBanner(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.RECONCILIATION_PAUSED_BANNER);
    }

    public static Locator reconciliationResumeButton(Page page) {
        return Locators.buttonNamed(reconciliationPausedBanner(page), TextConstants.RESUME);
    }

    public static Locator clusterName(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(2));
    }

    public static Locator brokerCount(Page page) {
        return page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\d+/\\d+$")));
    }

    public static Locator kafkaVersion(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.CLUSTER_KAFKA_VERSION);
    }

    public static Locator warningsToggle(Page page) {
        return page.locator(IdConstants.CLUSTER_WARNINGS_TOGGLE);
    }

    public static Locator warningsList(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.CLUSTER_WARNINGS_LIST);
    }

    /**
     * PatternFly's {@code DataListItem} consumes its {@code aria-labelledby} prop internally (wiring
     * cells together) rather than rendering it as an HTML attribute - confirmed against the live DOM,
     * where each item's {@code <li>} has no {@code aria-labelledby} at all. Scope under the already
     * OUIA-anchored warnings list instead and find items by their native {@code <li>} role.
     */
    public static Locator warningMessages(Page page) {
        return warningsList(page).getByRole(AriaRole.LISTITEM);
    }

    public static Locator recentTopicsListItems(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.RECENT_TOPICS_LIST).getByRole(AriaRole.LISTITEM);
    }

    public static Locator totalTopics(Page page) {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(Pattern.compile("topics$")));
    }

    public static Locator totalPartitions(Page page) {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(Pattern.compile("partitions$")));
    }

    public static Locator fullyReplicated(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.FULLY_REPLICATED_STATUS_LINK);
    }

    public static Locator underReplicated(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.UNDER_REPLICATED_STATUS_LINK);
    }

    public static Locator unavailable(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.OFFLINE_STATUS_LINK);
    }
}
