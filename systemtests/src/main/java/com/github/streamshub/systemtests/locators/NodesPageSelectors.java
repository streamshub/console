package com.github.streamshub.systemtests.locators;

public class NodesPageSelectors {

    private NodesPageSelectors() {}
    
    // ----------------------------
    // Nodes page
    // ----------------------------
    
    public static final String NPS_HEADER_TITLE_BADGE_TOTAL_COUNT = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text";
    public static final String NPS_HEADER_TITLE_BADGE_WORKING_NODES_COUNT = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(3) > div > span.pf-v6-c-label > span.pf-v6-c-label__content";
    public static final String NPS_HEADER_TITLE_BADGE_WARNING_NODES_COUNT = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(4) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";

    public static final String NPS_OVERVIEW_NODE_ITEMS = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(1) > div.pf-v6-c-card > div.pf-v6-c-card__body > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group";

    public static final String NPS_TABLE_BODY = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";

    public static String getNodeTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(NPS_TABLE_BODY).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getNodeTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getNodeTableRowItems(nthRow)).nth(nthColumn).build();
    }

    // Rebalance
    public static final String NPS_REBALANCE_TABLE_ITEMS = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";


    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(1) > button.pf-v6-c-button";

    public static final String NPS_REBALANCE_PROPOSAL_STATUS = "body > div#root:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(3) > div > span";

    public static final String NPS_REBALANCE_PROPOSAL_NAME = "body > div:nth-of-type(1) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(2) > button";

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_MODE = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr:nth-of-type(2) > td.pf-v6-c-table__td > div.pf-v6-c-table__expandable-row-content > dl.pf-v6-c-description-list > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-description-list__group > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_REBALANCE_PROPOSAL_DROPDOWN_AUTO_APPROVAL_ENABLED = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr:nth-of-type(2) > td.pf-v6-c-table__td > div.pf-v6-c-table__expandable-row-content > dl.pf-v6-c-description-list > div.pf-v6-l-flex > div:nth-of-type(1) > div.pf-v6-c-description-list__group > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_PROPOSAL_ACTION_DROPDOWN_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr > td.pf-v6-c-table__td:nth-of-type(5) > button.pf-v6-c-menu-toggle";

    public static final String NPS_PROPOSAL_ACTION_APPROVE_BUTTON = "body > div.pf-v6-c-menu:nth-of-type(2) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(1) > button.pf-v6-c-menu__item";

    public static final String NPS_PROPOSAL_MODAL_DATA_TO_MOVE = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div:nth-of-type(2) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(1) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_PROPOSAL_MODAL_DATA_MONITORED_PARTITIONS_PERCENTAGE = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div:nth-of-type(2) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(6) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";
    public static final String NPS_PROPOSAL_MODAL_DATA_NUMBER_REPLICA_MOVEMENTS = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div:nth-of-type(2) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(9) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_AFTER = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div:nth-of-type(2) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(10) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_PROPOSAL_MODAL_DATA_BALANCEDNESS_BEFORE = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div:nth-of-type(2) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(11) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String NPS_PROPOSAL_MODAL_CLOSE_BUTTON = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div:nth-of-type(3) > button.pf-v6-c-button > span.pf-v6-c-button__text";

    public static final String NPS_PROPOSAL_MODAL_CONFIRM_BUTTON = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > footer.pf-v6-c-modal-box__footer > button.pf-v6-c-button:nth-of-type(1)";

    public static final String NPS_FILTER_TYPE_ROLE_DROPDOWN_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__group:nth-of-type(1) > div.pf-v6-c-toolbar__item:nth-of-type(2) > button.pf-v6-c-menu-toggle";

    public static final String NPS_FILTER_BY_NODEPOOL_ITEMS = "body > div.pf-v6-c-menu:nth-of-type(2) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item";

    public static final String NPS_FILTER_CLEAR_ALL_FILTERS_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(3) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(2) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__group > div.pf-v6-c-toolbar__item:nth-of-type(2) > button.pf-v6-c-button > span.pf-v6-c-button__text";

    public static final String PAGES_NOT_AUTHORIZED_CONTENT = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-c-empty-state > div.pf-v6-c-empty-state__content > div.pf-v6-c-empty-state__body:nth-of-type(2)";
}
