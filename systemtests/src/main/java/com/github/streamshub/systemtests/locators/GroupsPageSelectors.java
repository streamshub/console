package com.github.streamshub.systemtests.locators;

public class GroupsPageSelectors {

    private GroupsPageSelectors() {}

    public static final String GPS_HEADER_TITLE = CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT;

    public static final String GPS_TABLE = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > table.pf-v6-c-table";

    public static final String GPS_TABLE_ITEMS = GPS_TABLE + " > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";

    public static final String GPS_GROUP_NAME_INPUT = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(1) > div.pf-v6-c-text-input-group > div.pf-v6-c-text-input-group__main:nth-of-type(1) > span.pf-v6-c-text-input-group__text > input.pf-v6-c-text-input-group__text-input";
    public static final String GPS_RESULT_FIRST_NAME = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(1) > a";
    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(GPS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }
    public static final String GPS_NO_GROUPS_AVAILABLE = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-c-empty-state:nth-of-type(2) > div.pf-v6-c-empty-state__content > h2.pf-v6-c-title";

}
