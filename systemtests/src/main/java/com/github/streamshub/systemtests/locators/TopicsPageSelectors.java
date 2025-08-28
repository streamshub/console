package com.github.streamshub.systemtests.locators;

public class TopicsPageSelectors {
    private TopicsPageSelectors() {}
    // ----------------------------
    // Topics page
    // ----------------------------
    public static final String TPS_HEADER_TOTAL_TOPICS_BADGE = new CssBuilder(CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(2).withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TPS_HEADER_BADGE_STATUS_SUCCESS = new CssBuilder(CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(3).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TPS_HEADER_BADGE_STATUS_WARNING = new CssBuilder(CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(4).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TPS_HEADER_BADGE_STATUS_ERROR = new CssBuilder(CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT_ITEMS)
        .nth(5).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentLabel().withChild()
        .withElementSpan().withComponentLabel().withSubComponentContent().withChild()
        .withElementSpan().withComponentLabel().withSubComponentText()
        .build();

    public static final String TPS_TABLE = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withChild()
        .withElementTable().withComponentTable()
        .build();

    public static final String TPS_TABLE_HEADER_ITEMS = new CssBuilder(TPS_TABLE)
        .withChild()
        .withElementThead().withComponentTable().withSubComponentThead().withChild()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTh().withComponentTable().withSubComponentTh()
        .build();

    public static final String TPS_TABLE_HEADER_SORT_BY_NAME = new CssBuilder(TPS_TABLE_HEADER_ITEMS)
        .nth(1)
        .build();

    public static final String TPS_TABLE_HEADER_SORT_BY_NAME_BUTTON = new CssBuilder(TPS_TABLE_HEADER_SORT_BY_NAME)
        .withChild()
        .withElementButton().withComponentTable().withSubComponentButton()
        .build();

    public static final String TPS_TABLE_HEADER_SORT_BY_STORAGE = new CssBuilder(TPS_TABLE_HEADER_ITEMS)
        .nth(5)
        .build();

    public static final String TPS_TABLE_HEADER_SORT_BY_STORAGE_BUTTON = new CssBuilder(TPS_TABLE_HEADER_SORT_BY_STORAGE)
        .withChild()
        .withElementButton().withComponentTable().withSubComponentButton()
        .build();

    public static final String TPS_TABLE_ROWS = new CssBuilder(TPS_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static final String TPS_TABLE_MANAGED_BADGE = new CssBuilder(TPS_TABLE_ROWS)
        .withChild()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTd().withComponentTable().withSubComponentTr().withChild()
        .withElementDiv().withChild()
        .withElementSpan()
        .build();

    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(TPS_TABLE_ROWS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }

    public static final String TPS_TOP_TOOLBAR = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1)
        .build();

    public static final String TPS_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(2).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().nth(2).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String TPS_TOP_TOOLBAR_SEARCH_CURRENT_STATUS_ITEMS = new CssBuilder(CssSelectors.PAGES_CONTENT)
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

    public static final String TPS_TOP_TOOLBAR_FILTER = new CssBuilder(TPS_TOP_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withComponentInputGroup()
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN = new CssBuilder(TPS_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementButton().withComponentMenuToggle().nth(1)
        .build();
    public static final String TPS_TOP_TOOLBAR_FILTER_TYPE_DROPDOWN_ITEMS = new CssBuilder(TPS_TOP_TOOLBAR_FILTER)
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_SEARCH = new CssBuilder(TPS_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_SEARCH_BUTTON = new CssBuilder(TPS_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN = new CssBuilder(TPS_TOP_TOOLBAR_FILTER)
        .withChild()
        .withElementButton().withComponentMenuToggle().nth(2)
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS = new CssBuilder(TPS_TOP_TOOLBAR_FILTER)
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_BY_STATUS_FULLY_REPLICATED = new CssBuilder(TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
        .nth(1)
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_BY_STATUS_UNDER_REPLICATED = new CssBuilder(TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
        .nth(2)
        .build();

    public static final String TPS_TOP_TOOLBAR_FILTER_BY_STATUS_OFFLINE = new CssBuilder(TPS_TOP_TOOLBAR_FILTER_BY_STATUS_DROPDOWN_ITEMS)
        .nth(5)
        .build();

    public static final String TPS_TOP_PAGINATION_DROPDOWN_BUTTON = new CssBuilder(TPS_TOP_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().withChild()
        .withElementDiv().withComponentPagination().withChild()
        .withElementDiv().withComponentPagination().withSubComponentPageMenu().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String TPS_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT = new CssBuilder(TPS_TOP_PAGINATION_DROPDOWN_BUTTON)
        .withChild()
        .withElementSpan().withComponentMenuToggle().withSubComponentText()
        .build();

    public static final String TPS_PAGINATION_DROPDOWN_ITEMS = new CssBuilder()
        .withElementBody().withDesc()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String TPS_TOP_PAGINATION_NAV_BUTTONS = new CssBuilder(TPS_TOP_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentGroup().withChild()
        .withElementDiv().withComponentPagination().withChild()
        .withElementNav().withComponentPagination().withSubComponentNav()
        .build();

    public static final String TPS_TOP_PAGINATION_NAV_PREV_BUTTON = new CssBuilder(TPS_TOP_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(1)
        .build();

    public static final String TPS_TOP_PAGINATION_NAV_NEXT_BUTTON = new CssBuilder(TPS_TOP_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(2)
        .build();

    public static final String TPS_BOTTOM_TOOLBAR = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentPagination()
        .build();

    public static final String TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON = new CssBuilder(TPS_BOTTOM_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentPageMenu().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT = new CssBuilder(TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON)
        .withChild()
        .withElementSpan().withComponentMenuToggle().withSubComponentText()
        .build();

    public static final String TPS_BOTTOM_PAGINATION_NAV_BUTTONS = new CssBuilder(TPS_BOTTOM_TOOLBAR)
        .withChild()
        .withElementNav().withComponentPagination().withSubComponentNav()
        .build();

    public static final String TPS_BOTTOM_PAGINATION_NAV_PREV_BUTTON = new CssBuilder(TPS_BOTTOM_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(1)
        .build();

    public static final String TPS_BOTTOM_PAGINATION_NAV_NEXT_BUTTON = new CssBuilder(TPS_BOTTOM_PAGINATION_NAV_BUTTONS)
        .withChild()
        .withElementDiv().withComponentPagination().withSubComponentNavControl().nth(2)
        .build();
}
