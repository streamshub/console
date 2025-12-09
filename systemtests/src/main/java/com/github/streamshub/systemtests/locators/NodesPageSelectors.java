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

    // Rebalance

    public static final String NPS_REBALANCE_TABLE_ITEMS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(3).withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementTable().withComponentTable().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody().withChild()
        .withElementTr().withComponentTable().withSubComponentTr()
        .build();


    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_BUTTON = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().nth(1).withChild()
        .withElementButton().withComponentButton().withChild()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_STATUS = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().nth(3).withChild()
        .withElementDiv().withChild()
        .withElementSpan()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_STATUS = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().nth(3).withChild()
        .withElementDiv().withChild()
        .withElementSpan()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_UPDATED_DATE = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().nth(4).withChild()
        .withElementTime()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_NAME = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().nth(2).withChild()
        .withElementA()
        .build();


    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_ITEMS = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().withChild()
        .withElementDiv().withComponentTable().withSubComponentExpandableRowContent().withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_MODE = new CssBuilder(NPS_REBALANCE_PROPOSAL_DROPDOWN_ITEMS)
        .withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_APPROVAL = new CssBuilder(NPS_REBALANCE_PROPOSAL_DROPDOWN_ITEMS)
        .withChild()
        .withElementDiv().nth(1).withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();
}
