/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.utils.playwright.locators;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public interface CssSelectors {


    static Locator getLocator(TestCaseConfig tcc, String selector) {
        return getLocator(tcc.page(), selector);
    }

    static Locator getLocator(Page page, String selector) {
        return page.locator(selector);
    }

    // ----------------------------
    // Login page
    // ----------------------------
    String LOGIN_ANONYMOUSLY_BUTTON = new CssBuilder()
        .withElementBody().withDesc()
        .withElementDiv().withComponentLogin().withChild()
        .withElementDiv().withComponentLogin().withSubComponentContainer().withChild()
        .withElementMain().withComponentLogin().withSubComponentMain().withChild()
        .withElementDiv().withComponentLogin().withSubComponentMainBody().withChild()
        .withElementButton().withComponentButton()
        .build();

    // ----------------------------
    // Page contents
    // ----------------------------
    String PAGES_MAIN_CONTENT = new CssBuilder()
        .withElementDiv().withComponentPage().withChild()
        .withElementMain().withComponentPage().withSubComponentMain().withChild()
        .withElementDiv().withComponentDrawer().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentMain().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentContent().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentBody()
        .build();

    String PAGES_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .withElementSection().withComponentPage().withSubComponentMainSection()
        .build();


    // -------------
    // Cluster Overview Page
    // -------------

    // Reconciliation
    String C_OVERVIEW_RECONCILIATION_MODAL = new CssBuilder()
        .withElementBody().withComponentBackdrop().withSubComponentOpen().withChild()
        .withElementDiv().withComponentBackdrop().withChild()
        .withElementDiv().withLayoutBullseye().withChild()
        .withElementDiv().withComponentModalBox()
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_HEADER = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .withElementHeader().withComponentModalBox().withSubComponentHeader()
        .build();


    String C_OVERVIEW_RECONCILIATION_MODAL_CLOSE_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .withElementDiv().withComponentModalBox().withSubComponentClose().withChild()
        .withElementButton().withComponentButton()
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_BODY = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .withElementDiv().withComponentModalBox().withSubComponentBody()
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_CONFIRM_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .withElementFooter().withComponentModalBox().withSubComponentFooter().withChild()
        .withElementButton().withComponentButton().nth(1)
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_CANCEL_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .withElementFooter().withComponentModalBox().withSubComponentFooter().withChild()
        .withElementButton().withComponentButton().nth(2)
        .build();

    String C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentPage().withChild()
        .withElementMain().withComponentPage().withSubComponentMain().withChild()
        .withElementDiv().withComponentBanner().withChild()
        .withElementDiv().withLayoutBullseye().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    String C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION_RESUME_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION)
        .withChild()
        .withElementDiv().withChild()
        .withElementButton().withComponentButton()
        .build();

    String C_OVERVIEW_CLUSTER_CARDS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO = new CssBuilder(C_OVERVIEW_CLUSTER_CARDS)
        .nth(1)
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    String C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO)
        .nth(1).withChild()
        .withElementDiv().nth(2).withChild()
        .withElementButton().withComponentButton()
        .build();
}
