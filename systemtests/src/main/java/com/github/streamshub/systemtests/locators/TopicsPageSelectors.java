package com.github.streamshub.systemtests.locators;

public class TopicsPageSelectors {
    private TopicsPageSelectors() {}
    // ----------------------------
    // Topics page
    // ----------------------------
    public static final String TPS_HEADER_TOTAL_TOPICS_BADGE = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(2) > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text";

    public static final String TPS_HEADER_BADGE_STATUS_SUCCESS = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(3) > div > span.pf-v6-c-label > span.pf-v6-c-label__content";

    public static final String TPS_HEADER_BADGE_STATUS_WARNING = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(4) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";

    public static final String TPS_HEADER_BADGE_STATUS_ERROR = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(5) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";

    public static final String TPS_TABLE_HEADER_SORT_BY_NAME = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(2) > table.pf-v6-c-table > thead.pf-v6-c-table__thead > tr.pf-v6-c-table__tr > th.pf-v6-c-table__th:nth-of-type(1)";
    public static final String TPS_TABLE_HEADER_SORT_BY_NAME_BUTTON = TPS_TABLE_HEADER_SORT_BY_NAME + " > button";

    public static final String TPS_TABLE_HEADER_SORT_BY_STORAGE = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(2) > table.pf-v6-c-table > thead.pf-v6-c-table__thead > tr.pf-v6-c-table__tr > th.pf-v6-c-table__th:nth-of-type(5)";
    public static final String TPS_TABLE_HEADER_SORT_BY_STORAGE_BUTTON = TPS_TABLE_HEADER_SORT_BY_STORAGE + " > button";

    public static final String TPS_NO_RESULTS_FOUND = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td > div.pf-v6-c-empty-state > div.pf-v6-c-empty-state__content > div.pf-v6-c-empty-state__header:nth-of-type(1) > div.pf-v6-c-empty-state__title:nth-of-type(2) > h4.pf-v6-c-empty-state__title-text";

    public static final String TPS_TABLE_ROWS = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";

    public static String getTopicsTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(TPS_TABLE_ROWS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTopicsTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTopicsTableRowItems(nthRow)).nth(nthColumn).build();
    }


    public static final String TPS_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(2) > div.pf-v6-c-toolbar__group:nth-of-type(2) > div.pf-v6-c-toolbar__item > button.pf-v6-c-button";

    public static final String TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item > div.pf-v6-c-toolbar__group:nth-of-type(1) > div.pf-v6-c-toolbar__group:nth-of-type(2) > div:nth-of-type(1) > button.pf-v6-c-menu-toggle";
    public static final String TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN_ITEMS = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item > div.pf-v6-c-toolbar__group:nth-of-type(1) > div.pf-v6-c-toolbar__group:nth-of-type(2) > div:nth-of-type(1) > div.pf-v6-c-menu > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li";

    public static final String TPS_TOP_TOOLBAR_FILTER_SEARCH = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(1) > div.pf-v6-c-toolbar__group:nth-of-type(1) > div.pf-v6-c-toolbar__group:nth-of-type(2) > div.pf-v6-c-toolbar__item:nth-of-type(2) > div.pf-v6-c-input-group > div.pf-v6-c-input-group__item:nth-of-type(1) > div.pf-v6-c-text-input-group > div.pf-v6-c-text-input-group__main > span.pf-v6-c-text-input-group__text > input.pf-v6-c-text-input-group__text-input";

    public static final String TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(1) > div.pf-v6-c-toolbar__group:nth-of-type(1) > div.pf-v6-c-toolbar__group:nth-of-type(2) > div.pf-v6-c-toolbar__item:nth-of-type(2) > div.pf-v6-c-input-group > div.pf-v6-c-input-group__item:nth-of-type(2) > button.pf-v6-c-button";
    public static final String TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item > div.pf-v6-c-toolbar__group:nth-of-type(1) > div.pf-v6-c-toolbar__group:nth-of-type(2) > div.pf-v6-c-toolbar__item:nth-of-type(2) > button.pf-v6-c-menu-toggle";

    public static final String TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS = "body > div.pf-v6-c-menu:nth-of-type(2) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li";


    public static final String TPS_TOP_PAGINATION_DROPDOWN_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(2) > div > div.pf-v6-c-pagination > div.pf-v6-c-pagination__page-menu:nth-of-type(2) > button.pf-v6-c-menu-toggle";

    public static final String TPS_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT = TPS_TOP_PAGINATION_DROPDOWN_BUTTON + " > span.pf-v6-c-menu-toggle__text";

    public static final String TPS_PAGINATION_DROPDOWN_ITEMS = "body > div.pf-v6-c-menu:nth-of-type(2) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li";

    public static final String TPS_TOP_PAGINATION_NAV_PREV_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(2) > div > div.pf-v6-c-pagination > nav.pf-v6-c-pagination__nav > div.pf-v6-c-pagination__nav-control:nth-of-type(1) > button";

    public static final String TPS_TOP_PAGINATION_NAV_NEXT_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(1) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(2) > div > div.pf-v6-c-pagination > nav.pf-v6-c-pagination__nav > div.pf-v6-c-pagination__nav-control:nth-of-type(2) > button";

    public static final String TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(3) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item > div > div.pf-v6-c-pagination > div.pf-v6-c-pagination__page-menu:nth-of-type(2) > button.pf-v6-c-menu-toggle";

    public static final String TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT = TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON + "> span.pf-v6-c-menu-toggle__text";

    public static final String TPS_BOTTOM_PAGINATION_NAV_PREV_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(3) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item > div > div.pf-v6-c-pagination > nav.pf-v6-c-pagination__nav > div.pf-v6-c-pagination__nav-control:nth-of-type(1) button";

    public static final String TPS_BOTTOM_PAGINATION_NAV_NEXT_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(3) > div.pf-v6-c-toolbar > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item > div > div.pf-v6-c-pagination > nav.pf-v6-c-pagination__nav > div.pf-v6-c-pagination__nav-control:nth-of-type(2) button";

    public static final String TPS_GROUPS_TABLE_FIRST_GROUP = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(1) > a";

    public static final String PAGES_NOT_AUTHORIZED_CONTENT = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-stack > div.pf-v6-l-stack__item:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td > div.pf-v6-c-empty-state > div.pf-v6-c-empty-state__content > div.pf-v6-c-empty-state__header:nth-of-type(1) > div.pf-v6-c-empty-state__title:nth-of-type(2) > h4.pf-v6-c-empty-state__title-text";

}
