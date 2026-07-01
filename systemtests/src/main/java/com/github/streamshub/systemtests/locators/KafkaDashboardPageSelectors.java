package com.github.streamshub.systemtests.locators;

public class KafkaDashboardPageSelectors {
    private KafkaDashboardPageSelectors() {}

    public static final String KDPS_KAFKA_CLUSTER_LIST_ITEMS = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";

    public static final String AD_KDPS_VIEW_BUTTON = "td.pf-v6-c-table__td:nth-of-type(6) > button.pf-v6-c-button";

    public static String getViewButton(int row) {
        return CssBuilder.joinLocators(new CssBuilder(KDPS_KAFKA_CLUSTER_LIST_ITEMS).nth(row).build(), AD_KDPS_VIEW_BUTTON);
    }

    public static final String KDPS_CURRENTLY_LOGGED_USER_BUTTON = "body > div#root > div.pf-v6-c-page > header.pf-v6-c-masthead > div.pf-v6-c-masthead__content > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item > button.pf-v6-c-menu-toggle > span.pf-v6-c-menu-toggle__text:nth-of-type(2)";
}
