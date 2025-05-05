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
        .e_body().withDesc()
        .e_div().c_login().withChild()
        .e_div().c_login().s_container().withChild()
        .e_main().c_login().s_main().withChild()
        .e_div().c_login().s_mainBody().withChild()
        .e_button().c_button()
        .build();


    // ----------------------------
    // Page contents
    // ----------------------------
    String PAGES_MAIN_CONTENT = new CssBuilder()
        .e_div().c_page().withChild()
        .e_main().c_page().s_main().withChild()
        .e_div().c_drawer().withChild()
        .e_div().c_drawer().s_main().withChild()
        .e_div().c_drawer().s_content().withChild()
        .e_div().c_drawer().s_body()
        .build();

    String PAGES_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .e_section().c_page().s_mainSection()
        .build();


    // -------------
    // Cluster Overview Page
    // -------------

    // Reconciliation
    String C_OVERVIEW_RECONCILIATION_MODAL = new CssBuilder()
        .e_body().c_backdrop().s_open().withChild()
        .e_div().c_backdrop().withChild()
        .e_div().l_bullseye().withChild()
        .e_div().c_modalBox()
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_HEADER = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .e_header().c_modalBox().s_header()
        .build();


    String C_OVERVIEW_RECONCILIATION_MODAL_CLOSE_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .e_div().c_modalBox().s_close().withChild()
        .e_button().c_button()
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_BODY = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .e_div().c_modalBox().s_body()
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_CONFIRM_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .e_footer().c_modalBox().s_footer().withChild()
        .e_button().c_button().nth(1)
        .build();

    String C_OVERVIEW_RECONCILIATION_MODAL_CANCEL_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_MODAL)
        .withChild()
        .e_footer().c_modalBox().s_footer().withChild()
        .e_button().c_button().nth(2)
        .build();

    String C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION = new CssBuilder()
        .e_body().withChild()
        .e_div().withChild()
        .e_div().c_page().withChild()
        .e_main().c_page().s_main().withChild()
        .e_div().c_banner().withChild()
        .e_div().l_bullseye().withChild()
        .e_div().l_flex()
        .build();

    String C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION_RESUME_BUTTON = new CssBuilder(C_OVERVIEW_RECONCILIATION_PAUSED_NOTIFICATION)
        .withChild()
        .e_div().withChild()
        .e_button().c_button()
        .build();

    String C_OVERVIEW_CLUSTER_CARDS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .e_div().l_grid().withChild()
        .e_div().l_grid().s_item().nth(1).withChild()
        .e_div().l_flex().withChild()
        .e_div()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO = new CssBuilder(C_OVERVIEW_CLUSTER_CARDS)
        .nth(1)
        .e_div().c_card().withChild()
        .e_div().c_card().s_body().withChild()
        .e_div().l_flex()
        .build();

    String C_OVERVIEW_KAFKA_PAUSE_RECONCILIATION_BUTTON = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO)
        .nth(1).withChild()
        .e_div().nth(2).withChild()
        .e_button().c_button()
        .build();

}
