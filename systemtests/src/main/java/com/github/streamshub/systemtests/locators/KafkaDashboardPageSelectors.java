package com.github.streamshub.systemtests.locators;

public class KafkaDashboardPageSelectors {
    private KafkaDashboardPageSelectors() {}

    public static final String KDPS_KAFKA_CLUSTER_LIST_ITEMS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withDesc()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutStack().withChild()
        .withElementDiv().withLayoutStack().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementTable().withComponentTable().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static final String KDPS_CURRENTLY_LOGGED_USER_BUTTON = new CssBuilder(CssSelectors.PAGE_DIV)
        .withChild()
        .withElementDiv().withComponentPage().withChild()
        .withElementHeader().withComponentMasthead().withChild()
        .withElementDiv().withComponentMasthead().withSubComponentContent().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();
}
