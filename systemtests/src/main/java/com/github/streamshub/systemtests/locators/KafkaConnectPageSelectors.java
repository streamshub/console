package com.github.streamshub.systemtests.locators;

public class KafkaConnectPageSelectors {

    private KafkaConnectPageSelectors() {}

    public static final String KCPS_TAB_BUTTON = new CssBuilder(CssSelectors.PAGES_HEADER)
        .withChild()
        .withElementSection().withComponentPage().withSubComponentMainSubnav().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentTabs().withChild()
        .withElementUl().withComponentTabs().withSubComponentList().withChild()
        .withElementLi().withComponentTabs().withSubComponentItem()
        .build();

    public static final String KCPS_TAB_CONNECTORS_BUTTON = new CssBuilder(CssSelectors.PAGES_HEADER)
        .nth(1).withChild()
        .withElementButton().withComponentTabs().withSubComponentLink()
        .build();

    public static final String KCPS_TAB_CONNECT_CLUSTERS_BUTTON = new CssBuilder(CssSelectors.PAGES_HEADER)
        .nth(2).withChild()
        .withElementButton().withComponentTabs().withSubComponentLink()
        .build();

    public static final String KCPS_TABLE = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementTable().withComponentTable()
        .build();

    public static final String KCPS_TABLE_ITEMS = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";

    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(KCPS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }

    public static final String KCPS_PAGE_TITLE_NAME = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title";
    public static final String KCPS_NAME_FILTER_INPUT = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(1) > div.pf-v6-c-text-input-group > div.pf-v6-c-text-input-group__main:nth-of-type(1) > span.pf-v6-c-text-input-group__text > input.pf-v6-c-text-input-group__text-input";

}
