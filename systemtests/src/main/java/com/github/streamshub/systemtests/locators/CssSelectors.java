/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators;

/**
 * Note: When creating new element from scratch using empty CssSelectors constructor,
 * please start with `body` element that is right after `html` element, this ensures code continuity.
 */
public class CssSelectors {

    private CssSelectors() {}

    public static final String PAGE_DIV = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().nth(1)
        .build();

    // ----------------------------
    // Login page
    // ----------------------------
    public static final String LOGIN_ANONYMOUSLY_BUTTON = new CssBuilder(PAGE_DIV)
        .withChild()
        .withElementDiv().withComponentLogin().withChild()
        .withElementDiv().withComponentLogin().withSubComponentContainer().withChild()
        .withElementMain().withComponentLogin().withSubComponentMain().withChild()
        .withElementDiv().withComponentLogin().withSubComponentMainBody().withChild()
        .withElementButton().withComponentButton()
        .build();

    // Keycloak
    public static final String LOGIN_KEYCLOAK_PAGE_TITLE = new CssBuilder(PAGE_DIV)
        .withDesc()
        .withElementHeader().withId("kc-header")
        .build();

    public static final String LOGIN_KEYCLOAK_USERNAME_INPUT = new CssBuilder(PAGE_DIV)
        .withDesc()
        .withElementInput().withId("username")
        .build();

    public static final String LOGIN_KEYCLOAK_PASSWORD_INPUT = new CssBuilder(PAGE_DIV)
        .withDesc()
        .withElementInput().withId("password")
        .build();

    public static final String LOGIN_KEYCLOAK_SIGN_IN_BUTTON = new CssBuilder(PAGE_DIV)
        .withDesc()
        .withElementButton().withId("kc-login")
        .build();

    // ----------------------------
    // Page contents
    // ----------------------------
    public static final String PAGES_MAIN_CONTENT = new CssBuilder(PAGE_DIV)
        .withChild()
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
        .withElementDiv().withLayoutFlex().nth(1)
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
        .nth(1)
        .build();

    public static final String PAGES_POPUP_MODAL = new CssBuilder()
        .withElementBody().withComponentBackdrop().withSubComponentOpen().withChild()
        .withElementDiv().withComponentBackdrop().withChild()
        .withElementDiv().withLayoutBullseye().withChild()
        .withElementDiv().withComponentModalBox()
        .build();

    public static final String PAGES_POPUP_MODAL_HEADER = new CssBuilder(PAGES_POPUP_MODAL)
        .withChild()
        .withElementHeader().withComponentModalBox().withSubComponentHeader()
        .build();

    public static final String PAGES_POPUP_MODAL_BODY = new CssBuilder(PAGES_POPUP_MODAL)
        .withChild()
        .withElementDiv().withComponentModalBox().withSubComponentBody()
        .build();

    public static final String PAGES_MODAL_CONFIRM_BUTTON = new CssBuilder(PAGES_POPUP_MODAL)
        .withChild()
        .withElementFooter().withComponentModalBox().withSubComponentFooter().withChild()
        .withElementButton().withComponentButton().nth(1)
        .build();

    public static final String PAGES_MODAL_CANCEL_BUTTON = new CssBuilder(PAGES_POPUP_MODAL)
        .withChild()
        .withElementFooter().withComponentModalBox().withSubComponentFooter().withChild()
        .withElementButton().withComponentButton().nth(2)
        .build();

    public static final String PAGES_MODAL_CLOSE_BUTTON = new CssBuilder(PAGES_POPUP_MODAL)
        .withChild()
        .withElementDiv().withComponentModalBox().withSubComponentClose().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String PAGES_AD_TABLE_ROW_ITEMS = new CssBuilder()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTd().withComponentTable().withSubComponentTd()
        .build();

    // Logged user
    public static final String PAGES_CURRENTLY_LOGGED_USER_BUTTON = new CssBuilder(CssSelectors.PAGE_DIV)
        .withChild()
        .withElementDiv().withComponentPage().withChild()
        .withElementHeader().withComponentMasthead().withChild()
        .withElementDiv().withComponentMasthead().withSubComponentContent().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().nth(3).withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    // Name of kafka cluster
    public static final String PAGES_LEFT_TOOLBAR_KAFKA_NAME = new CssBuilder(CssSelectors.PAGE_DIV)
        .withChild()
        .withElementDiv().withComponentPage().withChild()
        .withElementDiv().withComponentPage().withSubComponentSidebar().withChild()
        .withElementDiv().withComponentPage().withSubComponentSidebarBody().withChild()
        .withElementNav().withComponentNav().withChild()
        .withElementUl().withComponentNav().withSubComponentList().withChild()
        .withElementSection().withComponentNav().withSubComponentSection().withChild()
        .withElementH2().withComponentNav().withSubComponentSectionTitle()
        .build();

    // Select kafka
    public static final String PAGES_NAV_KAFKA_SELECT_BUTTON = new CssBuilder(CssSelectors.PAGE_DIV)
        .withChild()
        .withElementDiv().withComponentPage().withChild()
        .withElementHeader().withComponentMasthead().withChild()
        .withElementDiv().withComponentMasthead().withSubComponentContent().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().nth(1).withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    // Total available kafka clusters count
    public static final String PAGES_TOTAL_AVAILABLE_KAFKA_COUNT = new CssBuilder(PAGES_NAV_KAFKA_SELECT_BUTTON)
        .withChild()
        .withElementSpan().withComponentMenuToggle().withSubComponentText().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementSpan().withComponentBadge()
        .build();

    public static final String PAGES_LOGOUT_BUTTON = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem().withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String PAGES_NOT_AUTHORIZED_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withDesc()
        .withElementDiv().withComponentEmptyState().withChild()
        .withElementDiv().withComponentEmptyState().withSubComponentContent().withChild()
        .withElementDiv().withComponentEmptyState().withSubComponentHeader().withChild()
        .withElementDiv().withComponentEmptyState().withSubComponentTitle().withChild()
        .withElementH1().withComponentEmptyState().withSubComponentTitleText()
        .build();

    // Kafka credentials login page

    public static final String PAGES_KAFKA_CREDENTIALS_NAME_INPUT = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentLogin().withChild()
        .withElementDiv().withComponentLogin().withSubComponentContainer().withChild()
        .withElementMain().withComponentLogin().withSubComponentMain().withChild()
        .withElementDiv().withComponentLogin().withSubComponentMainBody().withChild()
        .withElementForm().withComponentForm().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String PAGES_KAFKA_CREDENTIALS_PASSWORD_INPUT = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentLogin().withChild()
        .withElementDiv().withComponentLogin().withSubComponentContainer().withChild()
        .withElementMain().withComponentLogin().withSubComponentMain().withChild()
        .withElementDiv().withComponentLogin().withSubComponentMainBody().withChild()
        .withElementForm().withComponentForm().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String PAGES_KAFKA_CREDENTIALS_LOGIN_BUTTON = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentLogin().withChild()
        .withElementDiv().withComponentLogin().withSubComponentContainer().withChild()
        .withElementMain().withComponentLogin().withSubComponentMain().withChild()
        .withElementDiv().withComponentLogin().withSubComponentMainBody().withChild()
        .withElementForm().withComponentForm().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().nth(3).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withComponentForm().withSubComponentActions().withChild()
        .withElementButton().withComponentButton()
        .build();
}
