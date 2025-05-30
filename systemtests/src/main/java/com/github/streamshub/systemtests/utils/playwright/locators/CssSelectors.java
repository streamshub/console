/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.utils.playwright.locators;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class CssSelectors {

    private CssSelectors() {}

    public static Locator getLocator(TestCaseConfig tcc, String selector) {
        return getLocator(tcc.page(), selector);
    }

    public static Locator getLocator(Page page, String selector) {
        return page.locator(selector);
    }

    // ----------------------------
    // Login page
    // ----------------------------
    public static final String LOGIN_ANONYMOUSLY_BUTTON = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentLogin().withChild()
        .withElementDiv().withComponentLogin().withSubComponentContainer().withChild()
        .withElementMain().withComponentLogin().withSubComponentMain().withChild()
        .withElementDiv().withComponentLogin().withSubComponentMainBody().withChild()
        .withElementButton().withComponentButton()
        .build();

    // ----------------------------
    // Page contents
    // ----------------------------
    public static final String PAGES_MAIN_CONTENT = new CssBuilder()
        .withElementDiv().withComponentPage().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainContainer().withChild()
        .withElementMain().withComponentPage().withSubComponentMain().withChild()
        .withElementDiv().withComponentDrawer().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentMain().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentContent().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentBody()
        .build();

    public static final String PAGES_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .withElementSection().withComponentPage().withSubComponentMainSection()
        .build();

    public static final String PAGES_HEADER = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainGroup()
        .build();

    public static final String PAGES_HEADER_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withElementSection().withComponentPage().withSubComponentMainSection().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withLayoutFlex().nth(1).withChild()
        .build();

    public static final String PAGES_HEADER_BREADCRUMB = new CssBuilder(PAGES_HEADER)
        .withChild()
        .withElementSection().withComponentPage().withSubComponentMainBreadcrumb().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementNav().withComponentBreadcrumb()
        .build();

    public static final String PAGES_HEADER_BREADCRUMB_ITEMS = new CssBuilder(PAGES_HEADER_BREADCRUMB)
        .withChild()
        .withElementOl().withComponentBreadcrumb().withSubComponentList().withChild()
        .withElementLi().withComponentBreadcrumb().withSubComponentItem()
        .build();

    public static final String PAGES_HEADER_RELOAD_BUTTON = new CssBuilder(PAGES_HEADER_CONTENT)
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withChild()
        .withElementButton().withComponentButton()
        .build();
    
    public static final String PAGES_CONTENT_HEADER_TITLE_CONTENT = new CssBuilder(PAGES_HEADER_CONTENT)
        .withElementDiv().withChild()
        .withElementH1().withComponentTitle()
        .build();

    public static final String PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT)
        .withChild()
        .withElementDiv().withLayoutSplit().withChild()
        .withElementDiv().withLayoutSplit().withSubComponentItem()
        .build();

    public static final String PAGES_CONTENT_HEADER_PAGE_NAME = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(1).withChild()
        .build();

    // -------------
    // Cluster Overview Page
    // -------------
    public static final String C_OVERVIEW_PAGE_RECONCILIATION_MODAL = new CssBuilder()
        .withElementBody().withComponentBackdrop().withSubComponentOpen().withChild()
        .withElementDiv().withComponentBackdrop().withChild()
        .withElementDiv().withLayoutBullseye().withChild()
        .withElementDiv().withComponentModalBox()
        .build();

    public static final String C_OVERVIEW_PAGE_RECONCILIATION_MODAL_HEADER = new CssBuilder(C_OVERVIEW_PAGE_RECONCILIATION_MODAL)
        .withChild()
        .withElementHeader().withComponentModalBox().withSubComponentHeader()
        .build();


    public static final String C_OVERVIEW_PAGE_RECONCILIATION_MODAL_CLOSE_BUTTON = new CssBuilder(C_OVERVIEW_PAGE_RECONCILIATION_MODAL)
        .withChild()
        .withElementDiv().withComponentModalBox().withSubComponentClose().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String C_OVERVIEW_PAGE_RECONCILIATION_MODAL_BODY = new CssBuilder(C_OVERVIEW_PAGE_RECONCILIATION_MODAL)
        .withChild()
        .withElementDiv().withComponentModalBox().withSubComponentBody()
        .build();

    public static final String C_OVERVIEW_PAGE_RECONCILIATION_MODAL_CONFIRM_BUTTON = new CssBuilder(C_OVERVIEW_PAGE_RECONCILIATION_MODAL)
        .withChild()
        .withElementFooter().withComponentModalBox().withSubComponentFooter().withChild()
        .withElementButton().withComponentButton().nth(1)
        .build();

    public static final String C_OVERVIEW_PAGE_RECONCILIATION_MODAL_CANCEL_BUTTON = new CssBuilder(C_OVERVIEW_PAGE_RECONCILIATION_MODAL)
        .withChild()
        .withElementFooter().withComponentModalBox().withSubComponentFooter().withChild()
        .withElementButton().withComponentButton().nth(2)
        .build();

    public static final String C_OVERVIEW_PAGE_RECONCILIATION_PAUSED_NOTIFICATION = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentPage().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainContainer().withChild()
        .withElementMain().withComponentPage().withSubComponentMain().withChild()
        .withElementDiv().withComponentBanner().withChild()
        .withElementDiv().withLayoutBullseye().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String C_OVERVIEW_PAGE_RECONCILIATION_PAUSED_NOTIFICATION_RESUME_BUTTON = new CssBuilder(C_OVERVIEW_PAGE_RECONCILIATION_PAUSED_NOTIFICATION)
        .withElementButton().withComponentButton()
        .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARDS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_INFO = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARDS)
        .withChild()
        .withElementDiv().nth(1).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody()
        .build();

    public static final String C_OVERVIEW_PAGE_KAFKA_PAUSE_RECONCILIATION_BUTTON = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_INFO)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(1).withChild()
        .withElementDiv().nth(2).withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_NAME = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_INFO)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(1).withChild()
        .withElementDiv().withChild()
        .withElementH2().withComponentTitle()
        .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_WARNINGS = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_INFO)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(2).withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentExpandableSection()
        .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_DATA_ITEMS = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_INFO)
            .withElementDiv().withLayoutFlex().nth(3).withChild()
            .withElementDiv().withLayoutGrid().withChild()
            .withElementDiv().withLayoutGrid().withSubComponentItem()
            .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_DATA_ITEMS)
            .nth(1)
            .withElementA()
            .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_DATA_CONSUMER_COUNT = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_DATA_ITEMS)
            .nth(2)
            .withElementA()
            .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_DATA_KAFKA_VERSION = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_DATA_ITEMS)
            .nth(3).withChild()
            .withElementDiv().nth(1)
            .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_WARNINGS_DROPDOWN_BUTTON = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_WARNINGS)
        .withElementDiv().withComponentExpandableSection().withSubComponentToggle().withChild()
        .withElementButton()
        .build();

    public static final String C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS = new CssBuilder(C_OVERVIEW_PAGE_CLUSTER_CARD_KAFKA_WARNINGS)
        .withElementDiv().withComponentExpandableSection().withSubComponentContent().withChild()
        .withElementUl().withComponentDataList().withChild()
        .withElementLi().withComponentDataList().withSubComponentItem()
        .build();

    public static final String C_OVERVIEW_PAGE_TOPIC_COLUMN_CARD_ITEMS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv()
        .build();

    public static final String C_OVERVIEW_PAGE_RECENT_TOPICS_CARD = new CssBuilder(C_OVERVIEW_PAGE_TOPIC_COLUMN_CARD_ITEMS)
        .nth(1)
        .withElementDiv().withComponentCard()
        .build();

    public static final String C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_HEADER = new CssBuilder(C_OVERVIEW_PAGE_RECENT_TOPICS_CARD)
        .nth(1)
        .withElementDiv().withComponentCard().withSubComponentHeader().withChild()
        .withElementDiv().withComponentCard().withSubComponentHeaderMain().withChild()
        .withElementDiv().withComponentContent()
        .build();

    public static final String C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_BODY = new CssBuilder(C_OVERVIEW_PAGE_RECENT_TOPICS_CARD)
        .withChild()
        .withElementDiv().withComponentCard().withSubComponentExpandableContent().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody()
        .build();

    public static final String C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS = new CssBuilder(C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_BODY)
        .withChild()
        .withElementTable().withComponentTable().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD = new CssBuilder(C_OVERVIEW_PAGE_TOPIC_COLUMN_CARD_ITEMS)
        .nth(2)
        .withElementDiv().withComponentCard()
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD_TOP_BODY_ITEMS = new CssBuilder(C_OVERVIEW_PAGE_TOPICS_CARD)
        .withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv()
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD_TOTAL_TOPICS = new CssBuilder(C_OVERVIEW_PAGE_TOPICS_CARD_TOP_BODY_ITEMS)
        .nth(1)
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD_TOTAL_PARTITIONS = new CssBuilder(C_OVERVIEW_PAGE_TOPICS_CARD_TOP_BODY_ITEMS)
        .nth(3)
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD_BODY_ITEMS = new CssBuilder(C_OVERVIEW_PAGE_TOPICS_CARD)
        .withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withLayoutFlex().nth(3).withChild()
        .withElementDiv()
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD_FULLY_REPLICATED = new CssBuilder(C_OVERVIEW_PAGE_TOPICS_CARD_BODY_ITEMS)
        .nth(1)
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD_UNDER_REPLICATED = new CssBuilder(C_OVERVIEW_PAGE_TOPICS_CARD_BODY_ITEMS)
        .nth(2)
        .build();

    public static final String C_OVERVIEW_PAGE_TOPICS_CARD_UNAVAILABLE = new CssBuilder(C_OVERVIEW_PAGE_TOPICS_CARD_BODY_ITEMS)
        .nth(3)
        .build();

    // ----------------------------
    // Nodes page
    // ----------------------------
    
    public static final String NODES_PAGE_HEADER_TITLE_BADGE_TOTAL_COUNT = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(2).withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String NODES_PAGE_HEADER_TITLE_BADGE_WORKING_NODES_COUNT = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(3).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String NODES_PAGE_HEADER_TITLE_BADGE_WARNING_NODES_COUNT = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(4).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String NODES_PAGE_OVERVIEW_ITEMS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem()
        .build();

    public static final String NODES_PAGE_OVERVIEW_NODE_ITEMS = new CssBuilder(NODES_PAGE_OVERVIEW_ITEMS)
        .nth(1).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup()
        .build();

    public static final String NODES_PAGE_OVERVIEW_PARTITIONS = new CssBuilder(NODES_PAGE_OVERVIEW_ITEMS)
        .nth(2)
        .build();

    public static final String NODES_PAGE_CONTENT = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2)
        .build();


    public static final String NODES_PAGE_TABLE_HEADER = new CssBuilder(PAGES_CONTENT)
        .withElementTable().withComponentTable().withChild()
        .withElementThead().withComponentTable().withSubComponentThead().withChild()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTh().withComponentTable().withSubComponentTh()
        .build();

    public static final String NODES_PAGE_TABLE_BODY = new CssBuilder(PAGES_CONTENT)
        .withElementTable().withComponentTable().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    // ----------------------------
    // Topics page
    // ----------------------------
    public static final String TOPICS_PAGE_HEADER_TOTAL_TOPICS_BADGE = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(2).withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TOPICS_PAGE_HEADER_BADGE_STATUS_SUCCESS = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(3).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TOPICS_PAGE_HEADER_BADGE_STATUS_WARNING = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(4).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TOPICS_PAGE_HEADER_BADGE_STATUS_ERROR = new CssBuilder(PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(5).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TOPICS_PAGE_TABLE = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withChild()
        .withElementTable().withComponentTable()
        .build();

    public static final String TOPICS_PAGE_TABLE_HEADER_ITEMS = new CssBuilder(TOPICS_PAGE_TABLE)
        .withChild()
        .withElementThead().withComponentTable().withSubComponentThead().withChild()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTh().withComponentTable().withSubComponentTh()
        .build();

    public static final String TOPICS_PAGE_TABLE_HEADER_SORT_BY_NAME = new CssBuilder(TOPICS_PAGE_TABLE_HEADER_ITEMS)
        .nth(1)
        .build();

    public static final String TOPICS_PAGE_TABLE_HEADER_SORT_BY_NAME_BUTTON = new CssBuilder(TOPICS_PAGE_TABLE_HEADER_SORT_BY_NAME)
        .withChild()
        .withElementButton().withComponentTable().withSubComponentButton()
        .build();

    public static final String TOPICS_PAGE_TABLE_HEADER_SORT_BY_STORAGE = new CssBuilder(TOPICS_PAGE_TABLE_HEADER_ITEMS)
        .nth(5)
        .build();

    public static final String TOPICS_PAGE_TABLE_HEADER_SORT_BY_STORAGE_BUTTON = new CssBuilder(TOPICS_PAGE_TABLE_HEADER_SORT_BY_STORAGE)
        .withChild()
        .withElementButton().withComponentTable().withSubComponentButton()
        .build();

    public static final String TOPICS_PAGE_TABLE_ROWS = new CssBuilder(TOPICS_PAGE_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static final String TOPICS_PAGE_TABLE_MANAGED_BADGE = new CssBuilder(TOPICS_PAGE_TABLE_ROWS)
        .withChild()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTd().withComponentTable().withSubComponentTr().withChild()
        .withElementDiv().withChild()
        .withElementSpan()
        .build();

    public static final String AD_TOPICS_PAGE_TOPIC_ROW_ITEMS = new CssBuilder()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTd().withComponentTable().withSubComponentTd()
        .build();

    public static String getTopicsPageTableRowItems(int nth) {
        // need to shift +2 due to thead being children too
        return CssBuilder.joinLocators(new CssBuilder(TOPICS_PAGE_TABLE_ROWS).nth(nth + 2).build(), AD_TOPICS_PAGE_TOPIC_ROW_ITEMS);
    }

    public static final String TOPICS_PAGE_TOP_TOOLBAR = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1)
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(2).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().nth(2).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CURRENT_STATUS_ITEMS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(2).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementDiv().withComponentLabelGroup().withChild()
        .withElementDiv().withComponentLabelGroup().withSubComponentMain().withChild()
        .withElementUl().withComponentLabelGroup().withSubComponentList().withChild()
        .withElementLi().withComponentLabelGroup().withSubComponentListItem()
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_FILTER = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withComponentInputGroup()
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementButton().withComponentMenuToggle().nth(1)
        .build();
    public static final String TOPICS_PAGE_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN_ITEMS = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER)
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_FILTER_SEARCH = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_FILTER_SEARCH_BUTTON = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementButton().withComponentMenuToggle().nth(2)
        .build();

    public static final String TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER)
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String  TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_FULLY_REPLICATED = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
        .nth(1)
        .build();

    public static final String  TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_UNDER_REPLICATED = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
        .nth(2)
        .build();

    public static final String  TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_OFFLINE = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
        .nth(5)
        .build();

    public static final String TOPICS_PAGE_TOP_PAGINATION_DROPDOWN_BUTTON = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().withChild()
        .withElementDiv().withComponentPagination().withChild()
        .withElementDiv().withComponentPagination().withSubComponentPageMenu().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String TOPICS_PAGE_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT = new CssBuilder(TOPICS_PAGE_TOP_PAGINATION_DROPDOWN_BUTTON)
        .withChild()
        .withElementSpan().withComponentMenuToggle().withSubComponentText()
        .build();

    public static final String TOPICS_PAGE_PAGINATION_DROPDOWN_ITEMS = new CssBuilder()
        .withElementBody().withDesc()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String TOPICS_PAGE_TOP_PAGINATION_NAV_BUTTONS = new CssBuilder(TOPICS_PAGE_TOP_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().withChild()
        .withElementDiv().withComponentPagination().withChild()
        .withElementNav().withComponentPagination().withSubComponentNav()
        .build();

    public static final String TOPICS_PAGE_TOP_PAGINATION_NAV_PREV_BUTTON = new CssBuilder(TOPICS_PAGE_TOP_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(1)
        .build();

    public static final String TOPICS_PAGE_TOP_PAGINATION_NAV_NEXT_BUTTON = new CssBuilder(TOPICS_PAGE_TOP_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(2)
        .build();

    public static final String TOPICS_PAGE_BOTTOM_TOOLBAR = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentPagination()
        .build();

    public static final String TOPICS_PAGE_BOTTOM_PAGINATION_DROPDOWN_BUTTON = new CssBuilder(TOPICS_PAGE_BOTTOM_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentPageMenu().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String TOPICS_PAGE_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT = new CssBuilder(TOPICS_PAGE_BOTTOM_PAGINATION_DROPDOWN_BUTTON)
        .withChild()
        .withElementSpan().withComponentMenuToggle().withSubComponentText()
        .build();

    public static final String TOPICS_PAGE_BOTTOM_PAGINATION_NAV_BUTTONS = new CssBuilder(TOPICS_PAGE_BOTTOM_TOOLBAR)
        .withChild()
        .withElementNav().withComponentPagination().withSubComponentNav()
        .build();

    public static final String TOPICS_PAGE_BOTTOM_PAGINATION_NAV_PREV_BUTTON = new CssBuilder(TOPICS_PAGE_BOTTOM_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(1)
        .build();

    public static final String TOPICS_PAGE_BOTTOM_PAGINATION_NAV_NEXT_BUTTON = new CssBuilder(TOPICS_PAGE_BOTTOM_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(2)
        .build();

    // ----------------------------
    // Messages page
    // ----------------------------
    public static final String MESSAGES_PAGE_EMPTY_BODY_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .withElementDiv().withComponentEmptyState()
        .build();
}
