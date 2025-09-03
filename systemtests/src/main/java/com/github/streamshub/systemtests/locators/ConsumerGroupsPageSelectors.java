package com.github.streamshub.systemtests.locators;

public class ConsumerGroupsPageSelectors {

    public static final String CGPS_HEADER_TITLE = CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT;

    public static final String CGPS_TABLE = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withComponentScrollOuterWrapper().withChild()
        .withElementDiv().withComponentScrollInnerWrapper().withChild()
        .withElementTable().withComponentTable()
        .build();

    public static final String CGPS_TABLE_ITEMS = new CssBuilder(CGPS_TABLE)
        .withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();


    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(CGPS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }

}
