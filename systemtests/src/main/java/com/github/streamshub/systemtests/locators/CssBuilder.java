/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators;

// To ignore checkstyle that would make this file twice as long because of the breaks after curly brackets
@SuppressWarnings("LeftCurly")
public class CssBuilder {
    private final StringBuilder cssClass;

    // Page css selectors
    private static final String PATTERNFLY = ".pf-v6";
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
        this.cssClass.append(":nth-of-type(" + nth + ")");
        return this;
    }

    static String joinLocators(String... locators) {
        return String.join(" ", locators);
    }

    // This shortens the code by a lot
    private CssBuilder withElement(String element) {
        this.cssClass.append(element);
        return this;
    }

    private CssBuilder withComponent(String component) {
        checkBase(SUFFIX_CONTENT);
        this.cssClass.append("-").append(component);
        return this;
    }

    private CssBuilder withSubComponent(String subComponent) {
        this.cssClass.append("__").append(subComponent);
        return this;
    }

    private CssBuilder withLayout(String layout) {
        checkBase(SUFFIX_LAYOUT);
        this.cssClass.append("-").append(layout);
        return this;
    }

    // -------------------
    // Constants
    // -------------------
    public static final String HREF = "href";
    // -------------------
    // Elements
    // -------------------
    public CssBuilder withElementBody() { return withElement("body"); }
    public CssBuilder withElementDiv() { return withElement("div"); }
    public CssBuilder withElementDl() { return withElement("dl"); }
    public CssBuilder withElementDt() { return withElement("dt"); }
    public CssBuilder withElementDd() { return withElement("dd"); }
    public CssBuilder withElementFooter() { return withElement("footer"); }
    public CssBuilder withElementLabel() { return withElement("label"); }
    public CssBuilder withElementThead() { return withElement("thead"); }
    public CssBuilder withElementTbody() { return withElement("tbody"); }
    public CssBuilder withElementTr() { return withElement("tr"); }
    public CssBuilder withElementTd() { return withElement("td"); }
    public CssBuilder withElementTh() { return withElement("th"); }
    public CssBuilder withElementH1() { return withElement("h1"); }
    public CssBuilder withElementH2() { return withElement("h2"); }
    public CssBuilder withElementH3() { return withElement("h3"); }
    public CssBuilder withElementH4() { return withElement("h4"); }
    public CssBuilder withElementSection() { return withElement("section"); }
    public CssBuilder withElementMain() { return withElement("main"); }
    public CssBuilder withElementNav() { return withElement("nav"); }
    public CssBuilder withElementA() { return withElement("a"); }
    public CssBuilder withElementOl() { return withElement("ol"); }
    public CssBuilder withElementImg() { return withElement("img"); }
    public CssBuilder withElementHeader() { return withElement("header"); }
    public CssBuilder withElementSmall() { return withElement("small"); }
    public CssBuilder withElementSpan() { return withElement("span"); }
    public CssBuilder withElementInput() { return withElement("input"); }
    public CssBuilder withElementP() { return withElement("p"); }
    public CssBuilder withElementB() { return withElement("b"); }
    public CssBuilder withElementUl() { return withElement("ul"); }
    public CssBuilder withElementLi() { return withElement("li"); }
    public CssBuilder withElementTable() { return withElement("table"); }
    public CssBuilder withElementButton() { return withElement("button"); }
    public CssBuilder withElementForm() { return withElement("form"); }
    public CssBuilder withElementTime() { return withElement("time"); }
    public CssBuilder withElementSvg() { return withElement("svg"); }
    public CssBuilder withElementG() { return withElement("g"); }
    public CssBuilder withElementText() { return withElement("text"); }
    public CssBuilder withElementPre() { return withElement("pre"); }
    public CssBuilder withElementCode() { return withElement("code"); }

    // -------------------
    // Components
    // -------------------
    public CssBuilder withComponentPage() { return withComponent("page"); }
    public CssBuilder withComponentBanner() { return withComponent("banner"); }
    public CssBuilder withComponentLogin() { return withComponent("login"); }
    public CssBuilder withComponentTitle() { return withComponent("title"); }
    public CssBuilder withComponentLabelGroup() { return withComponent("label-group"); }
    public CssBuilder withComponentBreadcrumb() { return withComponent("breadcrumb"); }
    public CssBuilder withComponentNav() { return withComponent("nav"); }
    public CssBuilder withComponentMasthead() { return withComponent("masthead"); }
    public CssBuilder withComponentMastheadBrand() { return withComponent("masthead_brand"); }
    public CssBuilder withComponentBrand() { return withComponent("brand"); }
    public CssBuilder withComponentToolbar() { return withComponent("toolbar"); }
    public CssBuilder withComponentPagination() { return withComponent("pagination"); }
    public CssBuilder withComponentSwitch() { return withComponent("switch"); }
    public CssBuilder withComponentInputGroup() { return withComponent("input-group"); }
    public CssBuilder withComponentTextInputGroup() { return withComponent("text-input-group"); }
    public CssBuilder withComponentFormControl() { return withComponent("form-control"); }
    public CssBuilder withComponentButton() { return withComponent("button"); }
    public CssBuilder withComponentCard() { return withComponent("card"); }
    public CssBuilder withComponentBackdrop() { return withComponent("backdrop"); }
    public CssBuilder withComponentDescriptionList() { return withComponent("description-list"); }
    public CssBuilder withComponentModalBox() { return withComponent("modal-box"); }
    public CssBuilder withComponentChart() { return withComponent("chart"); }
    public CssBuilder withComponentTable() { return withComponent("table"); }
    public CssBuilder withComponentLabel() { return withComponent("label"); }
    public CssBuilder withComponentList() { return withComponent("list"); }
    public CssBuilder withComponentClipboardCopy() { return withComponent("clipboard-copy"); }
    public CssBuilder withComponentContent() { return withComponent("content"); }
    public CssBuilder withComponentExpandableSection() { return withComponent("expandable-section"); }
    public CssBuilder withComponentDataList() { return withComponent("data-list"); }
    public CssBuilder withComponentEmptyState() { return withComponent("empty-state"); }
    public CssBuilder withComponentScrollOuterWrapper() { return withComponent("scroll-outer-wrapper"); }
    public CssBuilder withComponentScrollInnerWrapper() { return withComponent("scroll-inner-wrapper"); }
    public CssBuilder withComponentDrawer() { return withComponent("drawer"); }
    public CssBuilder withComponentBadge() { return withComponent("badge"); }
    public CssBuilder withComponentChipGroup() { return withComponent("chip-group"); }
    public CssBuilder withComponentMenu() { return withComponent("menu"); }
    public CssBuilder withComponentCheck() { return withComponent("check"); }
    public CssBuilder withComponentMenuToggle() { return withComponent("menu-toggle"); }
    public CssBuilder withComponentPanel() { return withComponent("panel"); }
    public CssBuilder withComponentForm() { return withComponent("form"); }
    public CssBuilder withComponentRadio() { return withComponent("radio"); }
    public CssBuilder withComponentDatePicker() { return withComponent("date-picker"); }
    public CssBuilder withComponentTabs() { return withComponent("tabs"); }
    public CssBuilder withComponentCodeBlock() { return withComponent("code-block"); }

    // ----------------
    // Sub-components
    // ----------------
    public CssBuilder withSubComponentText() { return withSubComponent("text"); }
    public CssBuilder withSubComponentTextInput() { return withSubComponent("text-input"); }
    public CssBuilder withSubComponentContent() { return withSubComponent("content"); }
    public CssBuilder withSubComponentBody() { return withSubComponent("body"); }
    public CssBuilder withSubComponentFooter() { return withSubComponent("footer"); }
    public CssBuilder withSubComponentContainer() { return withSubComponent("container"); }
    public CssBuilder withSubComponentHead() { return withSubComponent("head"); }
    public CssBuilder withSubComponentActions() { return withSubComponent("actions"); }
    public CssBuilder withSubComponentMainGroup() { return withSubComponent("main-group"); }
    public CssBuilder withSubComponentMainBreadcrumb() { return withSubComponent("main-breadcrumb"); }
    public CssBuilder withSubComponentContentSection() { return withSubComponent("content-section"); }
    public CssBuilder withSubComponentPageMenu() { return withSubComponent("page-menu"); }
    public CssBuilder withSubComponentMainSection() { return withSubComponent("main-section"); }
    public CssBuilder withSubComponentItem() { return withSubComponent("item"); }
    public CssBuilder withSubComponentItemRow() { return withSubComponent("item-row"); }
    public CssBuilder withSubComponentItemControl() { return withSubComponent("item-control"); }
    public CssBuilder withSubComponentOpen() { return withSubComponent("open"); }
    public CssBuilder withSubComponentItemMain() { return withSubComponent("item-main"); }
    public CssBuilder withSubComponentItemText() { return withSubComponent("item-text"); }
    public CssBuilder withSubComponentItemCheck() { return withSubComponent("item-check"); }
    public CssBuilder withSubComponentInput() { return withSubComponent("input"); }
    public CssBuilder withSubComponentControls() { return withSubComponent("controls"); }
    public CssBuilder withSubComponentLink() { return withSubComponent("link"); }
    public CssBuilder withSubComponentSubnav() { return withSubComponent("subnav"); }
    public CssBuilder withSubComponentSidebar() { return withSubComponent("sidebar"); }
    public CssBuilder withSubComponentSidebarBody() { return withSubComponent("sidebar-body"); }
    public CssBuilder withSubComponentTbody() { return withSubComponent("tbody"); }
    public CssBuilder withSubComponentThead() { return withSubComponent("thead"); }
    public CssBuilder withSubComponentGroup() { return withSubComponent("group"); }
    public CssBuilder withSubComponentNav() { return withSubComponent("nav"); }
    public CssBuilder withSubComponentNavControl() { return withSubComponent("nav-control"); }
    public CssBuilder withSubComponentClose() { return withSubComponent("close"); }
    public CssBuilder withSubComponentPanelMain() { return withSubComponent("panel-main"); }
    public CssBuilder withSubComponentPanel() { return withSubComponent("panel"); }
    public CssBuilder withSubComponentToggle() { return withSubComponent("toggle"); }
    public CssBuilder withSubComponentToggleText() { return withSubComponent("toggle-text"); }
    public CssBuilder withSubComponentToggleIcon() { return withSubComponent("toggle-icon"); }
    public CssBuilder withSubComponentMain() { return withSubComponent("main"); }
    public CssBuilder withSubComponentMainContainer() { return withSubComponent("main-container"); }
    public CssBuilder withSubComponentMainBody() { return withSubComponent("main-body"); }
    public CssBuilder withSubComponentBrand() { return withSubComponent("brand"); }
    public CssBuilder withSubComponentList() { return withSubComponent("list"); }
    public CssBuilder withSubComponentListItem() { return withSubComponent("list-item"); }
    public CssBuilder withSubComponentHeader() { return withSubComponent("header"); }
    public CssBuilder withSubComponentTitle() { return withSubComponent("title"); }
    public CssBuilder withSubComponentTitleText() { return withSubComponent("title-text"); }
    public CssBuilder withSubComponentHeaderMain() { return withSubComponent("header-main"); }
    public CssBuilder withSubComponentExpandableContent() { return withSubComponent("expandable-content"); }
    public CssBuilder withSubComponentTr() { return withSubComponent("tr"); }
    public CssBuilder withSubComponentTd() { return withSubComponent("td"); }
    public CssBuilder withSubComponentTh() { return withSubComponent("th"); }
    public CssBuilder withSubComponentButton() { return withSubComponent("button"); }
    public CssBuilder withSubComponentSection() { return withSubComponent("section"); }
    public CssBuilder withSubComponentSectionTitle() { return withSubComponent("section-title"); }
    public CssBuilder withSubComponentGroupControl() { return withSubComponent("group-control"); }
    public CssBuilder withSubComponentCheck() { return withSubComponent("check"); }
    public CssBuilder withSubComponentSort() { return withSubComponent("sort"); }
    public CssBuilder withSubComponentButtonContent() { return withSubComponent("button-content"); }
    public CssBuilder withSubComponentLabel() { return withSubComponent("label"); }
    public CssBuilder withSubComponentExpandableRowContent() { return withSubComponent("expandable-row-content"); }
    public CssBuilder withSubComponentDescription() { return withSubComponent("description"); }
    public CssBuilder withComponentTabContent() { return withComponent("tab-content"); }
    public CssBuilder withSubComponentMainSubnav() { return withSubComponent("main-subnav"); }
    public CssBuilder withSubComponentPre() { return withSubComponent("pre"); }
    public CssBuilder withSubComponentCode() { return withSubComponent("code"); }

    // ------------
    // Layouts
    // ------------
    public CssBuilder withLayoutSplit() { return withLayout("split"); }
    public CssBuilder withLayoutGrid() { return withLayout("grid"); }
    public CssBuilder withLayoutBullseye() { return withLayout("bullseye"); }
    public CssBuilder withLayoutStack() { return withLayout("stack"); }
    public CssBuilder withLayoutFlex() { return withLayout("flex"); }

    public String build() {
        return this.cssClass.toString()
            // Remove multiple whitespaces with only one
            .replaceAll("\\s+", " ")
            // Remove whitespace before ":" to avoid `div :nth-child(1)`
            .replace(" :", ":")
            // Add space after ")" if it's not there to avoid `div:nth-child(1)div`
            .replaceAll("\\)(?!\\s)", ") ");
    }
}
