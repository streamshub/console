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

    public static final String KEYCLOAK_PAGE = new CssBuilder()
        .withElementBody().withId("keycloak-bg").withChild()
        .withElementDiv().withChild()
        .withElementDiv()
        .build();

    public static final String KEYCLOAK_PAGE_REALM_NAME = new CssBuilder(KEYCLOAK_PAGE)
        .withElementHeader().withId("kc-header").withChild()
        .withElementDiv().withId("kc-header-wrapper")
        .build();
    public static final String KEYCLOAK_PAGE_LOGIN_FORM = new CssBuilder(KEYCLOAK_PAGE)
        .withElementMain().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withId("kc-form").withChild()
        .withElementDiv().withId("kc-form-wrapper").withChild()
        .withElementForm().withId("kc-form-login")
        .build();

    public static final String KEYCLOAK_PAGE_LOGIN_FORM_USERNAME = new CssBuilder(KEYCLOAK_PAGE_LOGIN_FORM)
        .withChild()
        .withElementDiv().nth(1).withChild()
        .withElementSpan().withChild()
        .withElementInput().withId("username")
        .build();

    public static final String KEYCLOAK_PAGE_LOGIN_FORM_PASSWORD = new CssBuilder(KEYCLOAK_PAGE_LOGIN_FORM)
        .withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().nth(1).withChild()
        .withElementSpan().withChild()
        .withElementInput().withId("password")
        .build();
    public static final String KEYCLOAK_PAGE_LOGIN_BUTTON = new CssBuilder(KEYCLOAK_PAGE_LOGIN_FORM)
        .withChild()
        .withElementDiv().withChild()
        .withElementDiv().withChild()
        .withElementButton().withId("kc-login")
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

}
