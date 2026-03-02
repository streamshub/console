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

    public static final String KCPS_TABLE_ITEMS = new CssBuilder(KCPS_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(KCPS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }

    public static final String KCPS_NAME_FILTER_GROUP = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup()
        .build();

    public static final String KCPS_NAME_FILTER_INPUT = new CssBuilder(KCPS_NAME_FILTER_GROUP)
        .withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String KCPS_NAME_FILTER_SEARCH_BUTTON = new CssBuilder(KCPS_NAME_FILTER_GROUP)
        .withChild()
        .withElementButton().withComponentButton()
        .build();
}
