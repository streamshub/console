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

    public static String getNodeTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(NPS_TABLE_BODY).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getNodeTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getNodeTableRowItems(nthRow)).nth(nthColumn).build();
    }

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
        .withElementButton().withComponentButton()
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

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_AUTO_APPROVAL_ENABLED = new CssBuilder(NPS_REBALANCE_PROPOSAL_DROPDOWN_ITEMS)
        .withChild()
        .withElementDiv().nth(1).withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_ACTION_DROPDOWN_BUTTON = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().nth(5).withChild()
        .withElementButton().withComponentMenuToggle()
        .build();


    public static final String NPS_PROPOSAL_ACTION_DROPDOWN_ITEMS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(3).withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String NPS_PROPOSAL_ACTION_APPROVE_BUTTON = new CssBuilder(NPS_PROPOSAL_ACTION_DROPDOWN_ITEMS)
        .nth(1)
        .build();

    public static final String NPS_PROPOSAL_ACTION_REFRESH_BUTTON = new CssBuilder(NPS_PROPOSAL_ACTION_DROPDOWN_ITEMS)
        .nth(2)
        .build();

    public static final String NPS_PROPOSAL_ACTION_STOP_BUTTON = new CssBuilder(NPS_PROPOSAL_ACTION_DROPDOWN_ITEMS)
        .nth(3)
        .build();


    public static final String NPS_PROPOSAL_MODAL_DATA_TO_MOVE = new CssBuilder(CssSelectors.PAGES_POPUP_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(1).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_MONITORED_PARTITIONS_PERCENTAGE = new CssBuilder(CssSelectors.PAGES_POPUP_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(6).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_NUMBER_REPLICA_MOVEMENTS = new CssBuilder(CssSelectors.PAGES_POPUP_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(9).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_AFTER = new CssBuilder(CssSelectors.PAGES_POPUP_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(10).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_BEFORE = new CssBuilder(CssSelectors.PAGES_POPUP_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(11).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_CLOSE_BUTTON = new CssBuilder(CssSelectors.PAGES_POPUP_MODAL)
        .withChild()
        .withElementFooter().withComponentModalBox().withSubComponentFooter().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String NPS_PROPOSAL_MODAL_CONFIRM_BUTTON = CssSelectors.PAGES_MODAL_CONFIRM_BUTTON;

    public static final String NPS_FILTER_DROPDOWN_BUTTONS = new CssBuilder(NPS_CONTENT)
        .withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String NPS_FILTER_TYPE_DROPDOWN_BUTTON = new CssBuilder(NPS_FILTER_DROPDOWN_BUTTONS).nth(1).build();
    public static final String NPS_FILTER_BY_DROPDOWN_BUTTON = new CssBuilder(NPS_FILTER_DROPDOWN_BUTTONS).nth(2).build();

    public static final String NPS_FILTER_TYPE_DROPDOWN_ITEMS = new CssBuilder(NPS_CONTENT)
        .withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String NPS_FILTER_TYPE_NODEPOOL_BUTTON = new CssBuilder(NPS_FILTER_TYPE_DROPDOWN_ITEMS)
        .nth(1).withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String NPS_FILTER_TYPE_ROLE_BUTTON = new CssBuilder(NPS_FILTER_TYPE_DROPDOWN_ITEMS)
        .nth(2).withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String NPS_FILTER_TYPE_STATUS_BUTTON = new CssBuilder(NPS_FILTER_TYPE_DROPDOWN_ITEMS)
        .nth(3).withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String NPS_FILTER_BY_NODEPOOL_ITEMS = new CssBuilder(NPS_CONTENT)
        .withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String NPS_FILTER_BY_ROLE_ITEMS = new CssBuilder(NPS_FILTER_TYPE_DROPDOWN_ITEMS)
        .withChild()
        .withElementLabel().withComponentMenu().withSubComponentItem().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemMain().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemText().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String NPS_FILTER_CLEAR_ALL_FILTERS_BUTTON = new CssBuilder(NPS_CONTENT)
        .withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(2).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().nth(2).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementButton().withComponentButton()
        .build();
}
