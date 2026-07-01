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
        .withElementDiv().withId("root")
        .build();

    // Keycloak
    public static final String LOGIN_KEYCLOAK_PAGE_TITLE = new CssBuilder()
        .withElementHeader().withId("kc-header")
        .build();

    public static final String LOGIN_KEYCLOAK_FORM = new CssBuilder()
        .withElementForm().withId("kc-form-login")
        .build();

    public static final String LOGIN_KEYCLOAK_USERNAME_INPUT = new CssBuilder(LOGIN_KEYCLOAK_FORM)
        .withDesc()
        .withElementInput().withId("username")
        .build();

    public static final String LOGIN_KEYCLOAK_PASSWORD_INPUT = new CssBuilder(LOGIN_KEYCLOAK_FORM)
        .withDesc()
        .withElementInput().withId("password")
        .build();

    public static final String LOGIN_KEYCLOAK_SIGN_IN_BUTTON = new CssBuilder(LOGIN_KEYCLOAK_FORM)
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
        .withElementMain().withComponentPage().withSubComponentMain()
        .build();

    public static final String PAGES_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .withElementDiv().withComponentDrawer()
        .build();

    public static final String PAGES_HEADER = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainGroup()
        .build();

    public static final String PAGES_CONTENT_HEADER_TITLE_CONTENT = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > h1.pf-v6-c-title";

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

    public static final String PAGES_AD_TABLE_ROW_ITEMS = new CssBuilder()
        .withElementTd().withComponentTable().withSubComponentTd()
        .build();

    public static final String PAGES_AD_TABLE_ROW_ITEMS_LINK = new CssBuilder()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTd().withComponentTable().withSubComponentTd().withChild()
        .withElementA()
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


    // Kafka credentials login page

    public static final String PAGES_KAFKA_CREDENTIALS_NAME_INPUT = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body:nth-of-type(2) > form.pf-v6-c-form > div.pf-v6-c-form__group:nth-of-type(1) > div.pf-v6-c-form__group-control:nth-of-type(2) > span.pf-v6-c-form-control > input#username";

    public static final String PAGES_KAFKA_CREDENTIALS_PASSWORD_INPUT = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body:nth-of-type(2) > form.pf-v6-c-form > div.pf-v6-c-form__group:nth-of-type(2) > div.pf-v6-c-form__group-control:nth-of-type(2) > span.pf-v6-c-form-control > input#password";

    public static final String PAGES_KAFKA_CREDENTIALS_LOGIN_BUTTON = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > footer.pf-v6-c-modal-box__footer > button.pf-v6-c-button:nth-of-type(1) > span.pf-v6-c-button__text";

    public static final String PAGES_NAV_KAFKA_CLUSTERS_LIST_ITEMS = "body > div.pf-v6-c-menu:nth-of-type(2) > div.pf-v6-c-menu__content:nth-of-type(2) > ul.pf-v6-c-menu__list > section.pf-v6-c-menu__group > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item";
}
