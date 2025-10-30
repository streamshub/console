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
        .withElementThead().withComponentTable().withSubComponentThead().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();
}
