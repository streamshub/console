/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.utils.playwright.locators;

// To ignore checkstyle that would make this file twice as long because of the breaks after curly brackets
@SuppressWarnings("LeftCurly")
public class CssBuilder {
    private final StringBuilder cssClass;

    // Page css selectors
    private static final String PATTERNFLY = ".pf-v5";
    private static final String SUFFIX_CONTENT = "-c";
    private static final String SUFFIX_LAYOUT = "-l";

    public CssBuilder() {
        this.cssClass = new StringBuilder();
    }

    public CssBuilder(String existingSelector) {
        this.cssClass = new StringBuilder();
        this.cssClass.append(existingSelector).append(" ");
    }

    public void checkBase(String suffix) {
        String base = PATTERNFLY + suffix;
        if (!this.cssClass.toString().endsWith(base)) {
            this.cssClass.append(base);
        }
    }

    // -------------------
    // Combinators
    // -------------------
    public CssBuilder withChild() {
        this.cssClass.append(" > ");
        return this;
    }

    public CssBuilder withDesc() {
        this.cssClass.append(" ");
        return this;
    }

    public CssBuilder withId(String id) {
        // Append nothing to make the element with class
        this.cssClass.append("#").append(id);
        return this;
    }

    public CssBuilder nth(int nth) {
        // Append nothing to make the element with class
        this.cssClass.append(":nth-child(" + nth + ")");
        return this;
    }

    static String joinLocators(String... locators) {
        return String.join(" ", locators);
    }

    // This shortens the code by a lot
    private CssBuilder addElement(String element) {
        this.cssClass.append(element);
        return this;
    }

    private CssBuilder addComponent(String component) {
        checkBase(SUFFIX_CONTENT);
        this.cssClass.append("-").append(component);
        return this;
    }

    private CssBuilder addSubComponent(String subComponent) {
        this.cssClass.append("__").append(subComponent);
        return this;
    }

    private CssBuilder addLayout(String layout) {
        checkBase(SUFFIX_LAYOUT);
        this.cssClass.append("-").append(layout);
        return this;
    }

    // -------------------
    // Elements
    // -------------------
    public CssBuilder e_body() { return addElement("body"); }
    public CssBuilder e_div() { return addElement("div"); }
    public CssBuilder e_footer() { return addElement("footer"); }
    public CssBuilder e_label() { return addElement("label"); }
    public CssBuilder e_thead() { return addElement("thead"); }
    public CssBuilder e_tbody() { return addElement("tbody"); }
    public CssBuilder e_tr() { return addElement("tr"); }
    public CssBuilder e_td() { return addElement("td"); }
    public CssBuilder e_th() { return addElement("th"); }
    public CssBuilder e_h1() { return addElement("h1"); }
    public CssBuilder e_h2() { return addElement("h2"); }
    public CssBuilder e_h3() { return addElement("h3"); }
    public CssBuilder e_section() { return addElement("section"); }
    public CssBuilder e_main() { return addElement("main"); }
    public CssBuilder e_nav() { return addElement("nav"); }
    public CssBuilder e_a() { return addElement("a"); }
    public CssBuilder e_ol() { return addElement("ol"); }
    public CssBuilder e_img() { return addElement("img"); }
    public CssBuilder e_header() { return addElement("header"); }
    public CssBuilder e_small() { return addElement("small"); }
    public CssBuilder e_span() { return addElement("span"); }
    public CssBuilder e_input() { return addElement("input"); }
    public CssBuilder e_p() { return addElement("p"); }
    public CssBuilder e_b() { return addElement("b"); }
    public CssBuilder e_ul() { return addElement("ul"); }
    public CssBuilder e_li() { return addElement("li"); }
    public CssBuilder e_table() { return addElement("table"); }
    public CssBuilder e_button() { return addElement("button"); }

    // -------------------
    // Components
    // -------------------
    public CssBuilder c_page() { return addComponent("page"); }
    public CssBuilder c_banner() { return addComponent("banner"); }
    public CssBuilder c_login() { return addComponent("login"); }
    public CssBuilder c_title() { return addComponent("title"); }
    public CssBuilder c_labelGroup() { return addComponent("label-group"); }
    public CssBuilder c_breadcrumb() { return addComponent("breadcrumb"); }
    public CssBuilder c_nav() { return addComponent("nav"); }
    public CssBuilder c_masthead() { return addComponent("masthead"); }
    public CssBuilder c_mastheadBrand() { return addComponent("masthead_brand"); }
    public CssBuilder c_brand() { return addComponent("brand"); }
    public CssBuilder c_toolbar() { return addComponent("toolbar"); }
    public CssBuilder c_pagination() { return addComponent("pagination"); }
    public CssBuilder c_switch() { return addComponent("switch"); }
    public CssBuilder c_inputGroup() { return addComponent("input-group"); }
    public CssBuilder c_textInputGroup() { return addComponent("text-input-group"); }
    public CssBuilder c_formControl() { return addComponent("form-control"); }
    public CssBuilder c_button() { return addComponent("button"); }
    public CssBuilder c_card() { return addComponent("card"); }
    public CssBuilder c_backdrop() { return addComponent("backdrop"); }
    public CssBuilder c_modalBox() { return addComponent("modal-box"); }
    public CssBuilder c_chart() { return addComponent("chart"); }
    public CssBuilder c_table() { return addComponent("table"); }
    public CssBuilder c_label() { return addComponent("label"); }
    public CssBuilder c_list() { return addComponent("list"); }
    public CssBuilder c_clipboardCopy() { return addComponent("clipboard-copy"); }
    public CssBuilder c_content() { return addComponent("content"); }
    public CssBuilder c_expandableSection() { return addComponent("expandable-section"); }
    public CssBuilder c_dataList() { return addComponent("data-list"); }
    public CssBuilder c_emptyState() { return addComponent("empty-state"); }
    public CssBuilder c_scrollOuterWrapper() { return addComponent("scroll-outer-wrapper"); }
    public CssBuilder c_scrollInnerWrapper() { return addComponent("scroll-inner-wrapper"); }
    public CssBuilder c_drawer() { return addComponent("drawer"); }
    public CssBuilder c_badge() { return addComponent("badge"); }
    public CssBuilder c_chipGroup() { return addComponent("chip-group"); }
    public CssBuilder c_menu() { return addComponent("menu"); }
    public CssBuilder c_check() { return addComponent("check"); }
    public CssBuilder c_menuToggle() { return addComponent("menu-toggle"); }

    // ----------------
    // Sub-components
    // ----------------
    public CssBuilder s_text() { return addSubComponent("text"); }
    public CssBuilder s_textInput() { return addSubComponent("text-input"); }
    public CssBuilder s_content() { return addSubComponent("content"); }
    public CssBuilder s_body() { return addSubComponent("body"); }
    public CssBuilder s_footer() { return addSubComponent("footer"); }
    public CssBuilder s_container() { return addSubComponent("container"); }
    public CssBuilder s_head() { return addSubComponent("head"); }
    public CssBuilder s_actions() { return addSubComponent("actions"); }
    public CssBuilder s_mainGroup() { return addSubComponent("main-group"); }
    public CssBuilder s_mainBreadcrumb() { return addSubComponent("main-breadcrumb"); }
    public CssBuilder s_contentSection() { return addSubComponent("content-section"); }
    public CssBuilder s_mainSection() { return addSubComponent("main-section"); }
    public CssBuilder s_item() { return addSubComponent("item"); }
    public CssBuilder s_open() { return addSubComponent("open"); }
    public CssBuilder s_itemMain() { return addSubComponent("item-main"); }
    public CssBuilder s_itemCheck() { return addSubComponent("item-check"); }
    public CssBuilder s_input() { return addSubComponent("input"); }
    public CssBuilder s_controls() { return addSubComponent("controls"); }
    public CssBuilder s_link() { return addSubComponent("link"); }
    public CssBuilder s_subnav() { return addSubComponent("subnav"); }
    public CssBuilder s_sidebarBody() { return addSubComponent("sidebar-body"); }
    public CssBuilder s_tbody() { return addSubComponent("tbody"); }
    public CssBuilder s_thead() { return addSubComponent("thead"); }
    public CssBuilder s_group() { return addSubComponent("group"); }
    public CssBuilder s_nav() { return addSubComponent("nav"); }
    public CssBuilder s_navControl() { return addSubComponent("nav-control"); }
    public CssBuilder s_close() { return addSubComponent("close"); }
    public CssBuilder s_panelMain() { return addSubComponent("panel-main"); }
    public CssBuilder s_panel() { return addSubComponent("panel"); }
    public CssBuilder s_toggle() { return addSubComponent("toggle"); }
    public CssBuilder s_toggleText() { return addSubComponent("toggle-text"); }
    public CssBuilder s_toggleIcon() { return addSubComponent("toggle-icon"); }
    public CssBuilder s_main() { return addSubComponent("main"); }
    public CssBuilder s_mainBody() { return addSubComponent("main-body"); }
    public CssBuilder s_brand() { return addSubComponent("brand"); }
    public CssBuilder s_list() { return addSubComponent("list"); }
    public CssBuilder s_listItem() { return addSubComponent("list-item"); }
    public CssBuilder s_header() { return addSubComponent("header"); }
    public CssBuilder s_title() { return addSubComponent("title"); }
    public CssBuilder s_titleText() { return addSubComponent("title-text"); }
    public CssBuilder s_headerMain() { return addSubComponent("header-main"); }
    public CssBuilder s_expandableContent() { return addSubComponent("expandable-content"); }
    public CssBuilder s_tr() { return addSubComponent("tr"); }
    public CssBuilder s_td() { return addSubComponent("td"); }
    public CssBuilder s_th() { return addSubComponent("th"); }

    // ------------
    // Layouts
    // ------------
    public CssBuilder l_split() { return addLayout("split"); }
    public CssBuilder l_grid() { return addLayout("grid"); }
    public CssBuilder l_bullseye() { return addLayout("bullseye"); }
    public CssBuilder l_stack() { return addLayout("stack"); }
    public CssBuilder l_flex() { return addLayout("flex"); }

    public String build() {
        // Regex pattern to match ')' not followed by a space
        String whitespaceAfterArgument = "\\)(?!\\s)";
        // Regex pattern to replace 2 and more spaces for 1
        String consecutiveSpaces = "\\s{2,}";
        // Regex pattern to remove spaces before :
        String doubleDots = "\\s+:";

        return this.cssClass.toString()
            .replaceAll(consecutiveSpaces, " ")
            .replaceAll(doubleDots, ":")
            .replaceAll(whitespaceAfterArgument, ") ");
    }
}
