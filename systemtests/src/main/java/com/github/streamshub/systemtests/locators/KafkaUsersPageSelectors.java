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
    public static final String KUPS_KAFKA_USER_NAME_HEADER = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > h1.pf-v6-c-title";


    // Authorization table
    public static final String KUPS_AUTHORIZATION_TABLE_ROWS = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div:nth-of-type(2) > section.pf-v6-c-tab-content > div.pf-v6-c-tab-content__body > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";

    public static String getAuthTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(KUPS_AUTHORIZATION_TABLE_ROWS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getAuthTableRowStartingWith(int nth, String type) {
        return CssBuilder.joinLocators(new CssBuilder(KUPS_AUTHORIZATION_TABLE_ROWS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getAuthTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getAuthTableRowItems(nthRow)).nth(nthColumn).build();
    }

    public static final String KUPS_DESCRIPTION_NAME = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div:nth-of-type(1) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(1) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String KUPS_DESCRIPTION_AUTH = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div:nth-of-type(1) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(3) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String KUPS_DESCRIPTION_USERNAME = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div:nth-of-type(1) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(2) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";

    public static final String KUPS_DESCRIPTION_CREATION_TIMESTAMP = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(3) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div:nth-of-type(1) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(5) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";
}
