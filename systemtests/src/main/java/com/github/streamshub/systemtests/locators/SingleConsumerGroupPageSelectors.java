package com.github.streamshub.systemtests.locators;

public class SingleConsumerGroupPageSelectors {
    private SingleConsumerGroupPageSelectors() {}

    public static final String SCGPS_PAGE_HEADER = new CssBuilder(CssSelectors.PAGES_HEADER)
        .withChild()
        .withElementSection().withComponentPage().withSubComponentMainSection().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String SCGPS_PAGE_HEADER_NAME = new CssBuilder(SCGPS_PAGE_HEADER)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(1).withChild()
        .withElementDiv().nth(1).withChild()
        .withElementH1().withComponentTitle()
        .build();

    public static final String SCGPS_RELOAD_PAGE_BUTTON = new CssBuilder(SCGPS_PAGE_HEADER)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(1).withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String SCGPS_PAGE = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentPanel().withChild()
        .withElementDiv().withComponentPanel().withSubComponentMain().withChild()
        .withElementDiv().withComponentPanel().withSubComponentMainBody()
        .build();

    public static final String SCGPS_FORM = new CssBuilder(SCGPS_PAGE)
        .withChild()
        .withComponentForm()
        .build();

    public static final String SCGPS_CONSUMER_GROUPS_TABLE = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementTable().withComponentTable()
        .build();

    public static final String SCGPS_CONSUMER_GROUPS_TABLE_ITEMS = new CssBuilder(SCGPS_CONSUMER_GROUPS_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static String getConsumerGroupsTableRow(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(SCGPS_CONSUMER_GROUPS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getConsumerGroupsTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getConsumerGroupsTableRow(nthRow)).nth(nthColumn).build();
    }

    public static final String SCGPS_RESET_CONSUMER_OFFSET_BUTTON = new CssBuilder(SCGPS_PAGE_HEADER)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(2).withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String SCGPS_RESET_PAGE_CONSUMER_GROUP_NAME = new CssBuilder(SCGPS_PAGE_HEADER)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(2).withChild()
        .withElementDiv().withChild()
        .withElementSpan().withChild()
        .withElementB()
        .build();

    public static final String SCGPS_RESET_PAGE_APPLY_TARGET = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl()
        .build();

    public static final String SCGPS_RESET_PAGE_APPLY_ON_ALL_TOPICS_RADIO = new CssBuilder(SCGPS_RESET_PAGE_APPLY_TARGET)
        .withChild()
        .withElementDiv().withComponentRadio().nth(1).withChild()
        .withElementInput().withComponentRadio().withSubComponentInput()
        .build();

    public static final String SCGPS_RESET_PAGE_APPLY_ON_SELECTED_TOPIC_RADIO = new CssBuilder(SCGPS_RESET_PAGE_APPLY_TARGET)
        .withChild()
        .withElementDiv().withComponentRadio().nth(2).withChild()
        .withElementInput().withComponentRadio().withSubComponentInput()
        .build();

    public static final String SCGPS_RESET_PAGE_TARGET_SEARCH_TOPIC_INPUT = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(1).withChild()
        .withElementDiv().withComponentMenuToggle().withChild()
        .withElementDiv().withComponentTextInputGroup().withChild()
        .withElementDiv().withComponentTextInputGroup().withSubComponentMain().withChild()
        .withElementSpan().withComponentTextInputGroup().withSubComponentText().withChild()
        .withElementInput().withComponentTextInputGroup().withSubComponentTextInput()
        .build();

    public static final String SCGPS_RESET_PAGE_TARGET_SEARCH_TOPIC_RESULTS = new CssBuilder()
        .withElementBody().withChild()
        .withElementDiv().withComponentMenu().withChild()
        .withElementDiv().withComponentMenu().withSubComponentContent().withChild()
        .withElementUl().withComponentMenu().withSubComponentList().withChild()
        .withElementLi().withComponentMenu().withSubComponentListItem()
        .build();

    public static final String SCGPS_AD_TARGET_SEARCH_TOPIC_RESULTS_BUTTON = new CssBuilder()
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static String getResetPageSelectedTopicResultItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(SCGPS_CONSUMER_GROUPS_TABLE_ITEMS).nth(nth).build(), SCGPS_AD_TARGET_SEARCH_TOPIC_RESULTS_BUTTON);
    }

    public static final String SCGPS_RESET_PAGE_PARTITIONS_RADIOS = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(1).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().nth(4).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl()
        .build();

    public static final String SCGPS_RESET_PAGE_ALL_PARTITIONS_RADIO = new CssBuilder(SCGPS_RESET_PAGE_PARTITIONS_RADIOS)
        .withChild()
        .withElementDiv().withComponentRadio().nth(1).withChild()
        .withElementInput().withComponentRadio().withSubComponentInput()
        .build();

    public static final String SCGPS_RESET_PAGE_SELECTED_PARTITION_RADIO = new CssBuilder(SCGPS_RESET_PAGE_PARTITIONS_RADIOS)
        .withChild()
        .withElementDiv().withComponentRadio().nth(2).withChild()
        .withElementInput().withComponentRadio().withSubComponentInput()
        .build();


    public static final String SCGPS_RESET_PAGE_SELECTED_PARTITION_DROPDOWN_BUTTON = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(1).withChild()
        .withElementButton().withComponentMenuToggle()
        .build();

    public static final String SCGPS_RESET_PAGE_SELECTED_PARTITION_DROPDOWN_ITEMS = SCGPS_RESET_PAGE_TARGET_SEARCH_TOPIC_RESULTS;

    public static final String SCGPS_AD_RESET_PAGE_SELECTED_PARTITION_ITEM_BUTTTON = new CssBuilder(SCGPS_RESET_PAGE_TARGET_SEARCH_TOPIC_RESULTS)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemMain().withChild()
        .withElementSpan().withComponentMenu().withSubComponentItemText()
        .build();

    public static String getResetPageSelectedPartitionItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(SCGPS_RESET_PAGE_SELECTED_PARTITION_DROPDOWN_ITEMS).nth(nth).build(), SCGPS_AD_RESET_PAGE_SELECTED_PARTITION_ITEM_BUTTTON);
    }

    public static final String SCGPS_RESET_PAGE_OFFSET_DROPDOWN_BUTTON = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementButton().withComponentMenuToggle()
        .build();


    public static final String SCGPS_RESET_PAGE_OFFSET_DROPDOWN_ITEMS = SCGPS_RESET_PAGE_TARGET_SEARCH_TOPIC_RESULTS;

    public static final String SCGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET = new CssBuilder(SCGPS_RESET_PAGE_OFFSET_DROPDOWN_ITEMS)
        .nth(1)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_LATEST_OFFSET = new CssBuilder(SCGPS_RESET_PAGE_OFFSET_DROPDOWN_ITEMS)
        .nth(2)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_EARLIEST_OFFSET = new CssBuilder(SCGPS_RESET_PAGE_OFFSET_DROPDOWN_ITEMS)
        .nth(3)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_SPECIFIC_DATETIME_OFFSET = new CssBuilder(SCGPS_RESET_PAGE_OFFSET_DROPDOWN_ITEMS)
        .nth(1)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_SPECIFIC_PARTITION_SPECIFIC_DATETIME_OFFSET = new CssBuilder(SCGPS_RESET_PAGE_OFFSET_DROPDOWN_ITEMS)
        .nth(4)
        .withChild()
        .withElementButton().withComponentMenu().withSubComponentItem()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET_INPUT = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withComponentRadio()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_ISO_FORMAT_RADIO = new CssBuilder(SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME)
        .nth(1).withChild()
        .withElementInput().withComponentRadio().withSubComponentInput()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_EPOCH_FORMAT_RADIO = new CssBuilder(SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME)
        .nth(2).withChild()
        .withElementInput().withComponentRadio().withSubComponentInput()
        .build();


    public static final String SCGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_INPUT = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementSection().withComponentForm().withSubComponentSection().nth(2).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().nth(4).withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();


    public static final String SCGPS_RESET_PAGE_RESET_BUTTONS = new CssBuilder(SCGPS_FORM)
        .withChild()
        .withElementDiv().withComponentForm().withSubComponentGroup().withChild()
        .withElementDiv().withComponentForm().withSubComponentGroupControl().withChild()
        .withElementDiv().withComponentForm().withSubComponentActions()
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_RESET_BUTTON = new CssBuilder(SCGPS_RESET_PAGE_RESET_BUTTONS)
        .withChild()
        .withElementButton().withComponentButton().nth(1)
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_DRY_RUN_BUTTON = new CssBuilder(SCGPS_RESET_PAGE_RESET_BUTTONS)
        .withChild()
        .withElementDiv().withComponentMenuToggle().withChild()
        .withElementButton().withComponentMenuToggle().withSubComponentButton().nth(1)
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_DRY_RUN_MORE_BUTTON = new CssBuilder(SCGPS_RESET_PAGE_RESET_BUTTONS)
        .withChild()
        .withElementDiv().withComponentMenuToggle().withChild()
        .withElementButton().withComponentMenuToggle().withSubComponentButton().nth(2)
        .build();

    public static final String SCGPS_RESET_PAGE_OFFSET_CANCEL_BUTTON = new CssBuilder(SCGPS_RESET_PAGE_RESET_BUTTONS)
        .withChild()
        .withElementButton().withComponentButton().nth(2).withChild()
        .withElementSpan().withComponentButton().withSubComponentText()
        .build();

    public static final String SCGPS_DRY_RUN_COMMAND = new CssBuilder(SCGPS_PAGE)
        .withChild()
        .withElementDiv().withLayoutStack().withChild()
        .withElementDiv().withLayoutStack().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withComponentClipboardCopy().withChild()
        .withElementDiv().withComponentClipboardCopy().withSubComponentGroup().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String SCGPS_BACK_TO_EDIT_OFFSET_BUTTON = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentPanel().withChild()
        .withElementDiv().withComponentPanel().withSubComponentMain().withChild()
        .withElementDiv().withComponentPanel().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutStack().withChild()
        .withElementDiv().withLayoutStack().withSubComponentItem().nth(4).withChild()
        .withElementButton().withComponentButton().withChild()
        .withElementSpan().withComponentButton().withSubComponentText()
        .build();
}
