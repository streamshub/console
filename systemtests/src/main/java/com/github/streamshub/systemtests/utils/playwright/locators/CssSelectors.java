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
    // Top navbar
    // ----------------------------
    String TOP_NAVBAR = new CssBuilder()
        .e_div().c_page().withChild()
        .e_header().c_masthead()
        .build();

    String NAVBAR_HAMBURGER_MENU = new CssBuilder(TOP_NAVBAR)
        .e_span().c_masthead().s_toggle().withChild()
        .e_button().withId("nav-toggle")
        .build();

    String TOP_NAVBAR_LOGO_IMAGE = new CssBuilder(TOP_NAVBAR)
        .e_div().c_masthead().s_main().withChild()
        .e_a().c_mastheadBrand().s_main().withChild()
        .e_img().c_brand()
        .build();

    String TOP_NAVBAR_RIGHT_CONTENT = new CssBuilder(TOP_NAVBAR)
        .e_div().c_masthead().s_content().withDesc()
        .e_div().c_toolbar().withChild()
        .e_div().c_toolbar().s_content()
        .build();

    String TOP_NAVBAR_RIGHT_CONTENT_GROUP = new CssBuilder(TOP_NAVBAR_RIGHT_CONTENT)
        .e_div().c_toolbar().s_contentSection().withChild()
        .e_div().c_toolbar().s_group()
        .e_div().c_toolbar().s_group()
        .build();

    String USER_BUTTON_GROUP = new CssBuilder(TOP_NAVBAR_RIGHT_CONTENT)
        .e_div().c_toolbar().s_contentSection().withChild()
        .e_div().c_toolbar().s_item().withDesc()
        .build();

    String USER_BUTTON = new CssBuilder(USER_BUTTON_GROUP)
        .c_menuToggle()
        .build();

    String LOGOUT_BUTTON = new CssBuilder(USER_BUTTON_GROUP)
        .e_div().c_menu().withChild()
        .e_div().c_menu().s_content().withChild()
        .e_ul().c_menu().s_list().withChild()
        .e_li().c_menu().s_listItem().withChild()
        .e_button().c_menu().s_item()
        .build();

    // ----------------------------
    // Left sidebar with Kafkas
    // ----------------------------
    String LEFT_SIDEBAR = new CssBuilder()
        .e_div().c_page().withChild()
        .e_div().withId("page-sidebar").withDesc()
        .e_div().c_page().s_sidebarBody()
        .build();

    String LEFT_SIDEBAR_NAV = new CssBuilder(LEFT_SIDEBAR)
        .e_nav().c_nav().withChild()
        .e_ul().c_nav().s_list()
        .build();

    String SIDEBAR_HOME_BUTTON = new CssBuilder(LEFT_SIDEBAR_NAV)
        .e_li().c_nav().s_item().nth(1)
        .build();

    String SIDEBAR_KAFKA_CLUSTERS = new CssBuilder(LEFT_SIDEBAR_NAV)
        .e_li().c_nav().s_item().nth(2)
        .build();

    String SIDEBAR_KAFKA_CLUSTERS_BUTTON = new CssBuilder(SIDEBAR_KAFKA_CLUSTERS)
        .withChild()
        .e_button().c_nav().s_link()
        .build();

    String SIDEBAR_KAFKA_CLUSTERS_ITEMS = new CssBuilder(SIDEBAR_KAFKA_CLUSTERS)
        .withChild()
        .e_section().c_nav().s_subnav().withChild()
        .e_ul().c_nav().s_list()
        .build();

    static String getSidebarKafkaLocator(int nth) {
        return new CssBuilder(SIDEBAR_KAFKA_CLUSTERS_ITEMS)
            .e_li().c_nav().s_item().nth(nth)
            .build();
    }

    // ----------------------------
    // Right sidebar with Kafka boostraps
    // ----------------------------
    String RIGHT_SIDEBAR = new CssBuilder()
        .e_div().c_page().withChild()
        .e_main().c_page().s_main().withChild()
        .e_div().c_drawer().withChild()
        .e_div().c_drawer().s_main().withChild()
        .e_div().c_drawer().s_panel()
        .build();

    String RIGHT_SIDEBAR_CONTENT = new CssBuilder(RIGHT_SIDEBAR)
        .e_div().c_drawer().s_panelMain()
        .build();

    String RIGHT_SIDEBAR_HEADER = new CssBuilder(RIGHT_SIDEBAR_CONTENT)
        .e_div().c_drawer().s_body().withChild()
        .e_div().c_drawer().s_head()
        .build();

    String RIGHT_SIDEBAR_HEADER_TITLE = new CssBuilder(RIGHT_SIDEBAR_HEADER)
        .e_h3().c_title()
        .build();

    String RIGHT_SIDEBAR_HEADER_EXIT_BUTTON = new CssBuilder(RIGHT_SIDEBAR_HEADER)
        .e_div().c_drawer().s_actions().withChild()
        .e_div().c_drawer().s_close().withChild()
        .e_button().c_button()
        .build();

    String RIGHT_SIDEBAR_CONTENT_MAIN = new CssBuilder(RIGHT_SIDEBAR_CONTENT)
        .e_div().l_stack().withChild()
        .e_div().l_stack().s_item().withChild()
        .e_div().c_content()
        .build();

    String RIGHT_SIDEBAR_EXTERNAL_BOOTSTRAPS = new CssBuilder(RIGHT_SIDEBAR_CONTENT)
        .e_div().c_expandableSection().nth(2)
        .build();

    String RIGHT_SIDEBAR_EXTERNAL_BOOTSTRAPS_DROPDOWN_BUTTON = new CssBuilder(RIGHT_SIDEBAR_EXTERNAL_BOOTSTRAPS)
        .e_button().c_expandableSection().s_toggle()
        .build();

    String RIGHT_SIDEBAR_EXTERNAL_BOOTSTRAPS_LIST = new CssBuilder(RIGHT_SIDEBAR_EXTERNAL_BOOTSTRAPS)
        .e_div().c_expandableSection().s_content().withChild()
        .e_ul().c_list().withChild()
        .e_li()
        .build();

    String RIGHT_SIDEBAR_EXTERNAL_BOOTSTRAPS_COUNT_SPAN = new CssBuilder(RIGHT_SIDEBAR_EXTERNAL_BOOTSTRAPS_DROPDOWN_BUTTON)
        .e_span().c_expandableSection().s_toggleText().withChild()
        .e_div().withChild()
        .e_span().c_badge()
        .build();

    String RIGHT_SIDEBAR_INTERNAL_BOOTSTRAPS = new CssBuilder(RIGHT_SIDEBAR_CONTENT)
        .e_div().c_expandableSection().nth(3)
        .build();

    String RIGHT_SIDEBAR_INTERNAL_BOOTSTRAPS_DROPDOWN_BUTTON = new CssBuilder(RIGHT_SIDEBAR_INTERNAL_BOOTSTRAPS)
        .e_button().c_expandableSection().s_toggle()
        .build();

    String RIGHT_SIDEBAR_INTERNAL_BOOTSTRAPS_LIST = new CssBuilder(RIGHT_SIDEBAR_INTERNAL_BOOTSTRAPS)
        .e_div().c_expandableSection().s_content().withChild()
        .e_ul().c_list().withChild()
        .e_li()
        .build();

    String RIGHT_SIDEBAR_INTERNAL_BOOTSTRAPS_COUNT_SPAN = new CssBuilder(RIGHT_SIDEBAR_INTERNAL_BOOTSTRAPS_DROPDOWN_BUTTON)
        .e_span().c_expandableSection().s_toggleText().withChild()
        .e_div().withChild()
        .e_span().c_badge()
        .build();

    String RIGHT_SIDEBAR_LEARNING_RESOURCES = new CssBuilder(RIGHT_SIDEBAR_CONTENT)
        .e_div().l_stack().withChild()
        .e_div().l_stack().s_item().withChild()
        .e_div().l_stack().withChild()
        .e_div().l_stack().s_item()
        .build();

    String ADD_BOOTSTRAP_ADDRESS = new CssBuilder()
        .e_div().c_clipboardCopy().withChild()
        .e_div().c_clipboardCopy().s_group().withChild()
        .e_span().c_formControl().withChild()
        .e_input()
        .build();

    String ADD_BOOTSTRAP_AUTH = new CssBuilder()
        .e_small()
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

    String PAGES_CONTENT_HEADER = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .e_div().c_page().s_mainGroup()
        .build();

    String PAGES_HEADER_RELOAD_BUTTON = new CssBuilder(PAGES_CONTENT_HEADER)
        .withChild()
        .e_section().c_page().s_mainSection().withChild()
        .e_div().l_flex().withChild()
        .e_div().l_flex().nth(1).withChild()
        .e_div().nth(2).withChild()
        .e_div().withChild()
        .e_button().c_button()
        .build();

    String PAGES_CONTENT = new CssBuilder(PAGES_MAIN_CONTENT)
        .withChild()
        .e_section().c_page().s_mainSection()
        .build();

    String PAGES_HEADER_BREADCRUMBS = new CssBuilder(PAGES_CONTENT_HEADER)
        .e_section().c_page().s_mainBreadcrumb().withChild()
        .e_nav().c_breadcrumb().withChild()
        .e_ol().c_breadcrumb().s_list()
        .build();

    String PAGES_CONTENT_HEADER_CONTENT_TITLE = new CssBuilder(PAGES_CONTENT_HEADER)
        .e_section().c_page().s_mainSection().withChild()
        .e_div().l_flex().withChild()
        .e_div().l_flex().withChild()
        .e_div().withChild()
        .e_h1().c_title()
        .build();
    String PAGES_PAGE_FILTER = new CssBuilder(PAGES_CONTENT)
        .e_div().c_toolbar()
        .build();
    String PAGES_CLEAR_ALL_FILTERS = new CssBuilder(PAGES_PAGE_FILTER)
        .e_div().c_toolbar().s_content().withChild()
        .e_div().c_toolbar().s_item()
        .build();

    String OVERVIEW_PAGE_CARDS = new CssBuilder(PAGES_MAIN_CONTENT)
        .e_section().c_page().s_mainSection().withChild()
        .e_div().l_stack()
        .build();

    String OVERVIEW_PAGE_SINGLE_CARD = new CssBuilder(OVERVIEW_PAGE_CARDS)
        .e_div().l_stack().s_item()
        .build();

    String CARD_HEADER = new CssBuilder()
        .e_div().c_card().s_header()
        .build();

    // -------------
    // Cluster Overview Page
    // -------------
    String C_OVERVIEW_HEADER_CONTENT_SUBTITLE = new CssBuilder(PAGES_CONTENT_HEADER)
        .e_section().c_page().s_mainSection().withChild()
        .e_div().l_flex().withChild()
        .e_div().l_flex().nth(2).withChild()
        .e_div().nth(1)
        .build();

    String C_OVERVIEW_HEADER_CONNECTION_BUTTON = new CssBuilder(PAGES_CONTENT_HEADER)
        .e_section().c_page().s_mainSection().withChild()
        .e_div().l_flex().withChild()
        .e_div().l_flex().nth(2).withChild()
        .e_div().l_flex().withChild()
        .e_div().l_flex().withChild()
        .e_div().withChild()
        .e_button().c_button()
        .build();

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

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_ITEMS = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO)
        .e_div().l_flex().nth(3).withChild()
        .e_div().l_grid().withChild()
        .e_div().l_grid().s_item()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(1)
        .e_a()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT_TITLE = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(1)
        .e_small()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_CONSUMER_COUNT = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(2)
        .e_a()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_CONSUMER_COUNT_TITLE = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(2)
        .e_small()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_KAFKA_VERSION = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(3).withChild()
        .e_div().nth(1)
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_KAFKA_VERSION_TITLE = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(3)
        .e_small()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_NAME = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO)
        .e_div().l_flex().nth(1).withChild()
        .e_div().withChild()
        .e_h2().c_title()
        .build();

    // Warnings bottom
    String C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNINGS = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO)
        .e_div().withChild()
        .e_div().c_expandableSection()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNINGS_DROPDOWN_BUTTON = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO)
        .e_div().l_flex().withChild()
        .e_div().withChild()
        .e_div().c_card().withChild()
        .e_div().c_card().s_body().withChild()
        .e_div().l_flex().withChild()
        .e_div().withChild()
        .e_div().c_expandableSection().withChild()
        .e_button().c_expandableSection().s_toggle()
        .build();
    // Button with labels
    String C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_TILES = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNINGS)
        .e_button().c_expandableSection().s_toggle().withChild()
        .e_span().c_expandableSection().s_toggleText().withChild()
        .e_h3().c_title()
        .build();

    // Labels with warning and error
    String C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_LABEL_ITEMS = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_TILES)
        .e_div().c_labelGroup().withChild()
        .e_div().c_labelGroup().s_main().withChild()
        .e_ul().c_labelGroup().s_list().withChild()
        .e_li().c_labelGroup().s_listItem()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_ERROR_LABEL = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_LABEL_ITEMS)
        .nth(1).withChild()
        .e_span().c_label()
        .build();

    String C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_LABEL = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_LABEL_ITEMS)
        .nth(2).withChild()
        .e_span().c_label()
        .build();


    // Message part visible
    String C_OVERVIEW_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS = new CssBuilder(C_OVERVIEW_CLUSTER_CARD_KAFKA_INFO)
        .e_div().c_expandableSection().withChild()
        .e_div().c_expandableSection().s_content().withChild()
        .e_ul().c_dataList().withChild()
        .e_li().c_dataList().s_item()
        .build();

    // Cluster metrics
    String C_OVERVIEW_CLUSTER_METRICS_CARD = new CssBuilder(C_OVERVIEW_CLUSTER_CARDS)
        .nth(2)
        .e_div().c_card()
        .build();

    String C_OVERVIEW_CLUSTER_METRICS_CARD_HEADER = new CssBuilder(C_OVERVIEW_CLUSTER_METRICS_CARD)
        .withChild()
        .e_div().c_card().s_header().withChild()
        .e_div().c_card().s_headerMain().withChild()
        .e_div().c_card().s_title().withChild()
        .e_div().c_card().s_titleText().withChild()
        .e_h2().c_title()
        .build();

    String C_OVERVIEW_CLUSTER_METRICS_CARD_METRICS_ITEMS = new CssBuilder(C_OVERVIEW_CLUSTER_METRICS_CARD)
        .withChild()
        .e_div().c_card().s_body().withChild()
        .e_div().l_flex()
        .build();

    String C_OVERVIEW_CLUSTER_METRICS_CARD_METRICS_CHART_ITEMS = new CssBuilder(C_OVERVIEW_CLUSTER_METRICS_CARD_METRICS_ITEMS)
        .withChild()
        .e_div().withChild()
        .e_div().c_chart()
        .build();

    String C_OVERVIEW_CLUSTER_METRICS_CARD_METRICS_HEADER_ITEMS = new CssBuilder(C_OVERVIEW_CLUSTER_METRICS_CARD_METRICS_ITEMS)
        .withChild()
        .e_b()
        .build();

    // -------
    // Topics card
    String C_OVERVIEW_TOPIC_CARDS_ITEMS = new CssBuilder(PAGES_CONTENT)
        .withChild()
        .e_div().l_grid().withChild()
        .e_div().l_grid().s_item().nth(2).withChild()
        .e_div().l_flex().withChild()
        .e_div()
        .build();

    String C_OVERVIEW_TOPICS_CARD = new CssBuilder(C_OVERVIEW_TOPIC_CARDS_ITEMS)
        .nth(2)
        .e_div().c_card()
        .build();

    String C_OVERVIEW_TOPICS_CARD_HEADER = new CssBuilder(C_OVERVIEW_TOPICS_CARD)
        .withChild()
        .e_div().c_card().s_header()
        .build();

    String C_OVERVIEW_TOPICS_CARD_HEADER_TITLE = new CssBuilder(C_OVERVIEW_TOPICS_CARD_HEADER)
        .withChild()
        .e_div().c_card().s_headerMain().withChild()
        .e_div().c_card().s_title().withChild()
        .e_div().c_card().s_titleText()
        .build();

    String C_OVERVIEW_TOPICS_CARD_HEADER_LINK = new CssBuilder(C_OVERVIEW_TOPICS_CARD_HEADER)
        .withChild()
        .e_div().c_card().s_actions().withChild()
        .e_a()
        .build();

    String C_OVERVIEW_RECENTLY_VIEWED_TOPICS_CARD = new CssBuilder(C_OVERVIEW_TOPIC_CARDS_ITEMS)
        .nth(1)
        .build();

    String C_OVERVIEW_RECENTLY_VIEWED_TOPICS_CARD_HEADER = CssBuilder.joinLocators(C_OVERVIEW_RECENTLY_VIEWED_TOPICS_CARD, CARD_HEADER);

    String C_OVERVIEW_RECENTLY_VIEWED_TOPICS_CARD_BODY = new CssBuilder(C_OVERVIEW_RECENTLY_VIEWED_TOPICS_CARD)
        .e_div().c_card().s_expandableContent().withChild()
        .e_div().c_card().s_body()
        .build();

    String C_OVERVIEW_RECENTLY_VIEWED_TOPICS_ITEMS = new CssBuilder(C_OVERVIEW_RECENTLY_VIEWED_TOPICS_CARD_BODY)
        .withChild()
        .e_table().c_table().withChild()
        .e_tbody().c_table().s_tbody()
        .build();

    String C_AD_TOPIC_ROW = new CssBuilder()
        .e_tr().c_table().s_tr().withChild()
        .e_td().c_table().s_td().nth(1).withChild()
        .e_a()
        .build();

    String C_AD_CLUSTER_ROW = new CssBuilder()
        .e_tr().c_table().s_tr().withChild()
        .e_td().c_table().s_td().nth(2).withChild()
        .e_a()
        .build();

    // Topics Body
    String C_OVERVIEW_TOPICS_CARD_BODY = new CssBuilder(C_OVERVIEW_TOPICS_CARD)
        .withChild()
        .e_div().c_card().s_body().withChild()
        .e_div().l_flex().withChild()
        .e_div().l_flex()
        .build();

    String C_OVERVIEW_TOPICS_CARD_TOTAL_TOPICS = new CssBuilder(C_OVERVIEW_TOPICS_CARD_BODY)
        .nth(1)
        .e_div().withChild()
        .e_div().l_flex().withChild()
        .e_div().nth(1).withChild()
        .e_div().c_content().withChild()
        .e_small().withChild()
        .e_a()
        .build();

    String C_OVERVIEW_TOPICS_CARD_TOTAL_PARTITIONS = new CssBuilder(C_OVERVIEW_TOPICS_CARD_BODY)
        .nth(1)
        .e_div().withChild()
        .e_div().l_flex().withChild()
        .e_div().nth(3)
        .e_div().c_content().withChild()
        .e_small()
        .build();

    String C_OVERVIEW_TOPICS_CARD_FULLY_REPLICATED = new CssBuilder(C_OVERVIEW_TOPICS_CARD_BODY)
        .nth(3).withChild()
        .e_div().nth(1)
        .build();

    String C_OVERVIEW_TOPICS_CARD_UNDER_REPLICATED = new CssBuilder(C_OVERVIEW_TOPICS_CARD_BODY)
        .nth(3).withChild()
        .e_div().nth(2)
        .build();

    String C_OVERVIEW_TOPICS_CARD_UNAVAILABLE = new CssBuilder(C_OVERVIEW_TOPICS_CARD_BODY)
        .nth(3).withChild()
        .e_div().nth(3)
        .build();

    // -------
    // Topic metrics card
    String C_OVERVIEW_TOPICS_METRICS_CARD = new CssBuilder(C_OVERVIEW_TOPIC_CARDS_ITEMS)
        .nth(3)
        .e_div().c_card()
        .build();

    String C_OVERVIEW_TOPICS_METRICS_CARD_TITLE = new CssBuilder(C_OVERVIEW_TOPICS_METRICS_CARD)
        .e_div().c_card().s_header().withChild()
        .e_div().c_card().s_headerMain().withChild()
        .e_div().c_card().s_title().withChild()
        .e_div().c_card().s_titleText().withChild()
        .e_h2()
        .build();

    String C_OVERVIEW_TOPICS_METRICS_CARD_BODY = new CssBuilder(C_OVERVIEW_TOPICS_METRICS_CARD)
        .e_div().c_card().s_body().withChild()
        .e_div().l_flex()
        .build();

    String C_OVERVIEW_TOPICS_METRICS_CARD_BODY_TITLE = new CssBuilder(C_OVERVIEW_TOPICS_METRICS_CARD_BODY)
        .withChild()
        .e_b()
        .build();

    String C_OVERVIEW_TOPICS_METRICS_CARD_BODY_CHART = new CssBuilder(C_OVERVIEW_TOPICS_METRICS_CARD_BODY)
        .withChild()
        .e_div().withChild()
        .e_div().c_chart()
        .build();

}
