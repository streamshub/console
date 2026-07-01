package com.github.streamshub.systemtests.locators;

public class NodesPageSelectors {

    private NodesPageSelectors() {}
    
    // ----------------------------
    // Nodes page
    // ----------------------------
    
    public static final String NPS_HEADER_TITLE_BADGE_TOTAL_COUNT = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text";
    public static final String NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(3) > div > span.pf-v6-c-label > span.pf-v6-c-label__content";
    public static final String NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(4) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";

    public static final String NPS_OVERVIEW_ITEMS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem()
        .build();

    public static final String NPS_OVERVIEW_NODE_ITEMS = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(1) > div.pf-v6-c-card > div.pf-v6-c-card__body > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group";

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

    public static final String NPS_TABLE_BODY = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";

    public static String getNodeTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(NPS_TABLE_BODY).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getNodeTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getNodeTableRowItems(nthRow)).nth(nthColumn).build();
    }

    // Rebalance
    public static final String NPS_REBALANCE_TABLE_ITEMS = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";


    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(1) > button#expand-toggle0.pf-v6-c-button > span.pf-v6-c-button__icon > div.pf-v6-c-table__toggle-icon";

    public static final String NPS_REBALANCE_PROPOSAL_STATUS = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(3) > div > span";

    public static final String NPS_REBALANCE_PROPOSAL_UPDATED_DATE = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().nth(4).withChild()
        .withElementTime()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_NAME = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(2) > button.pf-v6-c-button > span.pf-v6-c-button__text";

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_ITEMS = new CssBuilder(NPS_REBALANCE_TABLE_ITEMS)
        .withChild()
        .withElementTd().withComponentTable().withSubComponentTd().withChild()
        .withElementDiv().withComponentTable().withSubComponentExpandableRowContent().withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_MODE = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr:nth-of-type(2) > td.pf-v6-c-table__td > div.pf-v6-c-table__expandable-row-content > dl.pf-v6-c-description-list > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-description-list__group > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_AUTO_APPROVAL_ENABLED = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr:nth-of-type(2) > td.pf-v6-c-table__td > div.pf-v6-c-table__expandable-row-content > dl.pf-v6-c-description-list > div.pf-v6-l-flex > div:nth-of-type(1) > div.pf-v6-c-description-list__group > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_PROPOSAL_ACTION_DROPDOWN_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr:nth-of-type(1) > td.pf-v6-c-table__td:nth-of-type(5) > button.pf-v6-c-menu-toggle > span.pf-v6-c-menu-toggle__icon";


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

    public static final String NPS_PROPOSAL_ACTION_APPROVE_BUTTON = "body > div.pf-v6-c-menu:nth-of-type(2) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(1) > button.pf-v6-c-menu__item > span.pf-v6-c-menu__item-main > span.pf-v6-c-menu__item-text";

    public static final String NPS_PROPOSAL_ACTION_REFRESH_BUTTON = new CssBuilder(NPS_PROPOSAL_ACTION_DROPDOWN_ITEMS)
        .nth(2)
        .build();

    public static final String NPS_PROPOSAL_ACTION_STOP_BUTTON = new CssBuilder(NPS_PROPOSAL_ACTION_DROPDOWN_ITEMS)
        .nth(3)
        .build();

    public static final String NPS_PROPOSAL_MODAL_BODY = "body.pf-v6-c-backdrop__open > div#pf-modal-part-3.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div#pf-modal-part-2.pf-v6-c-modal-box > div:nth-of-type(2)";

    public static final String NPS_PROPOSAL_MODAL_DATA_TO_MOVE = new CssBuilder(NPS_PROPOSAL_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(1).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_MONITORED_PARTITIONS_PERCENTAGE = new CssBuilder(NPS_PROPOSAL_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(6).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_NUMBER_REPLICA_MOVEMENTS = new CssBuilder(NPS_PROPOSAL_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(9).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_AFTER = new CssBuilder(NPS_PROPOSAL_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(10).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_BEFORE = new CssBuilder(NPS_PROPOSAL_MODAL_BODY)
        .withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup().nth(11).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String NPS_PROPOSAL_MODAL_CLOSE_BUTTON = "body.pf-v6-c-backdrop__open > div#pf-modal-part-3.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div#pf-modal-part-2.pf-v6-c-modal-box > div:nth-of-type(3) > button.pf-v6-c-button > span.pf-v6-c-button__text";

    public static final String NPS_PROPOSAL_MODAL_CONFIRM_BUTTON = "body.pf-v6-c-backdrop__open > div#pf-modal-part-2.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div#pf-modal-part-1.pf-v6-c-modal-box > footer.pf-v6-c-modal-box__footer > button.pf-v6-c-button:nth-of-type(1) > span.pf-v6-c-button__text";
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

    public static final String NPS_FILTER_TYPE_ROLE_DROPDOWN_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div#pf-random-id-_r_1d_.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__group:nth-of-type(1) > div.pf-v6-c-toolbar__item:nth-of-type(2) > button.pf-v6-c-menu-toggle";

    public static final String NPS_FILTER_BY_NODEPOOL_ITEMS = "body > div.pf-v6-c-menu:nth-of-type(2) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item";

    public static final String NPS_FILTER_BY_ROLE_ITEMS = new CssBuilder(NPS_FILTER_TYPE_DROPDOWN_ITEMS)
        .withChild()
        .withElementLabel().withComponentMenu().withSubComponentItem().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemMain().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemText().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String NPS_FILTER_CLEAR_ALL_FILTERS_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div#pf-random-id-_r_1d_.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(2) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__group > div.pf-v6-c-toolbar__item:nth-of-type(2) > button.pf-v6-c-button > span.pf-v6-c-button__text";
    public static final String PAGES_NOT_AUTHORIZED_CONTENT = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-c-empty-state > div.pf-v6-c-empty-state__content > div.pf-v6-c-empty-state__body:nth-of-type(2)";

}
