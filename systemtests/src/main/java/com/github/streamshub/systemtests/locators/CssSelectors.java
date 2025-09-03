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
}
