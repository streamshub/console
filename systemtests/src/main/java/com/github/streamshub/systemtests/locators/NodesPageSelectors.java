package com.github.streamshub.systemtests.locators;

public class NodesPageSelectors {
    private NodesPageSelectors() {}
    
    // ----------------------------
    // Nodes page
    // ----------------------------
    
    public static final String NPS_HEADER_TITLE_BADGE_TOTAL_COUNT = new CssBuilder(CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(2).withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT = new CssBuilder(CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(3).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT = new CssBuilder(CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(4).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String NPS_OVERVIEW_ITEMS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem()
        .build();

    public static final String NPS_OVERVIEW_NODE_ITEMS = new CssBuilder(NPS_OVERVIEW_ITEMS)
        .nth(1).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup()
        .build();

    public static final String NPS_OVERVIEW_PARTITIONS = new CssBuilder(NPS_OVERVIEW_ITEMS)
        .nth(2)
        .build();

    public static final String NPS_CONTENT = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2)
        .build();

    public static final String NPS_TABLE_HEADER = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withElementTable().withComponentTable().withChild()
        .withElementThead().withComponentTable().withSubComponentThead().withChild()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTh().withComponentTable().withSubComponentTh()
        .build();

    public static final String NPS_TABLE_BODY = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withElementTable().withComponentTable().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

}
