package com.github.streamshub.systemtests.locators;

public class MessagesPageSelectors {
    private MessagesPageSelectors() {}

    public static final String MPS_EMPTY_BODY_CONTENT = new CssBuilder(CssSelectors.PAGES_MAIN_CONTENT)
        .withChild()
        .withElementDiv().withComponentEmptyState()
        .build();

    public static final String MPS_BODY_CONTENT = new CssBuilder(CssSelectors.PAGES_MAIN_CONTENT)
        .withChild()
        .withElementSection().withComponentPage().withSubComponentMainSection().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentDrawer().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentMain().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentContent().withChild()
        .withElementDiv().withComponentScrollOuterWrapper()
        .build();

    public static final String MPS_SEARCH_TOOLBAR = new CssBuilder(MPS_BODY_CONTENT)
        .withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection()
        .build();

    public static final String MPS_SEARCH_TOOLBAR_INPUT_GROUP = new CssBuilder(MPS_SEARCH_TOOLBAR)
        .withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementDiv().withComponentInputGroup()
        .build();

    public static final String MPS_SEARCH_TOOLBAR_QUERY_INPUT = new CssBuilder(MPS_SEARCH_TOOLBAR_INPUT_GROUP)
        .withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentTextInputGroup().withChild()
        .withElementDiv().withComponentTextInputGroup().withSubComponentMain().withChild()
        .withElementSpan().withComponentTextInputGroup().withSubComponentText().withChild()
        .withElementInput().withComponentTextInputGroup().withSubComponentTextInput()
        .build();

    public static final String MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON = new CssBuilder(MPS_SEARCH_TOOLBAR_INPUT_GROUP)
        .withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(2).withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String MPS_TOOLBAR_POPOVER_FORM = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().nth(3).withChild()
        .withElementDiv().withComponentPanel().withChild()
        .withElementDiv().withComponentPanel().withSubComponentMain().withChild()
        .withElementDiv().withComponentPanel().withSubComponentMainBody().withChild()
        .withElementForm().withComponentForm()
        .build();

    public static final String MPS_TPF_FILTER_SECTION = new CssBuilder(MPS_TOOLBAR_POPOVER_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(1)
        .build();

    public static final String MPS_TPF_HAS_WORDS_INPUT = new CssBuilder(MPS_TPF_FILTER_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String MPS_TPF_WHERE_DROPDOWN_BUTTON = new CssBuilder(MPS_TPF_FILTER_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();


    public static final String MPS_TPF_PARAMETERS_SECTION = new CssBuilder(MPS_TOOLBAR_POPOVER_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(2)
        .build();

    public static final String MPS_TPF_PARAMETERS_MESSAGES = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(1).withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS = MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS;

    public static final String MPS_TPF_PARAMETERS_RETRIEVE_TYPE_BUTTON = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(1).withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String MPS_TPF_PARAMETERS_MESSAGES_UNIX_TIMESTAMP_INPUT = MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT;

    public static final String MPS_TPF_PARAMETERS_MESSAGES_TIMESTAMP_INPUT = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withChild()
        .withElementDiv().withSubComponentInput().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(1).withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String MPS_TPF_PARAMETERS_RETRIEVE_COUNT_BUTTON = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementButton().withComponentMenuToggle()
        .build();


    public static final String MPS_TPF_PARAMETERS_RETRIEVE_COUNT_ITEMS = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String MPS_TPF_PARAMETERS_IN_PARTITION_BUTTON = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(3).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String MPS_TPF_PARAMETERS_IN_PARTITION_ITEMS = new CssBuilder()
        .withChild()
        .withElementBody().withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String MPS_TPF_SEARCH_BUTTON = new CssBuilder(MPS_TOOLBAR_POPOVER_FORM)
        .withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withComponentForm().withSubComponentActions().withChild()
        .withElementButton().withComponentButton().nth(1)
        .build();

    public static final String MPS_TPF_RESET_BUTTON = new CssBuilder(MPS_TOOLBAR_POPOVER_FORM)
        .withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withComponentForm().withSubComponentActions().withChild()
        .withElementButton().withComponentButton().nth(2)
        .build();

    public static final String MPS_TABLE = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentDrawer().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentMain().withChild()
        .withElementDiv().withComponentDrawer().withSubComponentContent().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementDiv().withChild()
        .withElementTable().withComponentTable()
        .build();

    public static final String MPS_EMPTY_FILTER_SEARCH_CONTENT = new CssBuilder(MPS_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody().withChild()
        .withElementTr().withComponentTable().withSubComponentTr().withChild()
        .withElementTd().withComponentTable().withSubComponentTd().withChild()
        .withElementDiv().withComponentEmptyState().withChild()
        .withElementDiv().withComponentEmptyState().withSubComponentContent()
        .build();

    public static final String MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON = new CssBuilder(MPS_SEARCH_TOOLBAR_INPUT_GROUP)
        .withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(3).withDesc()
        .withElementButton().withComponentButton()
        .build();

    public static final String MPS_SEARCH_RESULTS_TABLE = new CssBuilder(MPS_BODY_CONTENT)
        .withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementDiv().withChild()
        .withElementTable().withComponentTable()
        .build();


    public static final String MPS_SEARCH_RESULTS_TABLE_ITEMS = new CssBuilder(MPS_SEARCH_RESULTS_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(MPS_SEARCH_RESULTS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }

    public static final String MPS_TPF_PARAMETERS_DATE_INPUT = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentDatePicker().withChild()
        .withElementDiv().withComponentDatePicker().withSubComponentInput().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(1).withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String MPS_TPF_PARAMETERS_DATE_FROM_TIMESTAMP = new CssBuilder(MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemMain().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemText()
        .build();

    public static final String MPS_TPF_PARAMETERS_DATE_FROM_UNIT_TIMESTAMP = new CssBuilder(MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemMain().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemText()
        .build();

    public static final String MPS_TPF_PARAMETERS_TIME_INPUT = new CssBuilder(MPS_TPF_PARAMETERS_SECTION)
        .withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withComponentDatePicker().withChild()
        .withElementDiv().withComponentDatePicker().withSubComponentInput().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup().withSubComponentItem().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();
}
