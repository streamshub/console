package com.github.streamshub.systemtests.locators;

public class KafkaUsersPageSelectors {
    public static final String KUPS_USER_FILTER_INPUT = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentToolbar().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContent().nth(1).withChild()
        .withElementDiv().withComponentToolbar().withSubComponentContentSection().withChild()
        .withElementDiv().withComponentToolbar().withSubComponentItem().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementDiv().withComponentInputGroup().withChild()
        .withElementSpan().withComponentFormControl().withChild()
        .withElementInput()
        .build();

    public static final String KUPS_USER_RESULTS_TABLE = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementTable().withComponentTable()
        .build();


    public static final String KUPS_USER_RESULTS_TABLE_ITEMS = new CssBuilder(KUPS_USER_RESULTS_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();


    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(KUPS_USER_RESULTS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }

    // Single user page
    public static final String KUPS_KAFKA_USER_NAME_HEADER = SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME;


    // Authorization table
    public static final String KUPS_AUTHORIZATION_TABLE_ROWS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementSection().withComponentTabContent().withChild()
        .withElementDiv().withComponentTabContent().withSubComponentBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementTable().withComponentTable().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static String getAuthTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(KUPS_AUTHORIZATION_TABLE_ROWS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getAuthTableRowStartingWith(int nth, String type) {
        return CssBuilder.joinLocators(new CssBuilder(KUPS_AUTHORIZATION_TABLE_ROWS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getAuthTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getAuthTableRowItems(nthRow)).nth(nthColumn).build();
    }

    // Description list
    public static final String KUPS_DESCRIPTION_LIST = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(1).withChild()
        .withElementDl().withComponentDescriptionList().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentGroup()
        .build();

    public static final String KUPS_DESCRIPTION_NAME = new CssBuilder(KUPS_DESCRIPTION_LIST)
        .nth(1).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String KUPS_DESCRIPTION_AUTH = new CssBuilder(KUPS_DESCRIPTION_LIST)
        .nth(3).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().nth(1).withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String KUPS_DESCRIPTION_USERNAME = new CssBuilder(KUPS_DESCRIPTION_LIST)
        .nth(2).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String KUPS_DESCRIPTION_NAMESPACE = new CssBuilder(KUPS_DESCRIPTION_LIST)
        .nth(4).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();

    public static final String KUPS_DESCRIPTION_CREATION_TIMESTAMP = new CssBuilder(KUPS_DESCRIPTION_LIST)
        .nth(5).withChild()
        .withElementDd().withComponentDescriptionList().withSubComponentDescription().withChild()
        .withElementDiv().withComponentDescriptionList().withSubComponentText()
        .build();
}
