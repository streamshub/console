package com.github.streamshub.systemtests.locators;

public class MessagesPageSelectors {
    private MessagesPageSelectors() {}

    public static final String MPS_EMPTY_BODY_CONTENT = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-c-empty-state:nth-of-type(2) > div.pf-v6-c-empty-state__content > h2.pf-v6-c-title";

    public static final String MPS_SEARCH_TOOLBAR_QUERY_INPUT = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(1) > div > div.pf-v6-c-input-group > div.pf-v6-c-input-group__item:nth-of-type(1) > div.pf-v6-c-text-input-group > div.pf-v6-c-text-input-group__main:nth-of-type(1) > span.pf-v6-c-text-input-group__text > input.pf-v6-c-text-input-group__text-input";

    public static final String MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(1) > div > div.pf-v6-c-input-group > div.pf-v6-c-input-group__item:nth-of-type(2) > button.pf-v6-c-button";

    public static final String MPS_TPF_HAS_WORDS_INPUT = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(1) > div.pf-v6-l-grid:nth-of-type(2) > div.pf-v6-l-grid__item:nth-of-type(1) > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control:nth-of-type(2) > span.pf-v6-c-form-control > input";
    public static final String MPS_TPF_WHERE_DROPDOWN_BUTTON = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(1) > div.pf-v6-l-grid:nth-of-type(2) > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control:nth-of-type(2) > button.pf-v6-c-menu-toggle";
    public static final String MPS_TPF_WHERE_DROPDOWN_ITEMS = "body > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item";

    public static final String MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_BUTTON = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-l-grid:nth-of-type(2) > div.pf-v6-l-grid__item:nth-of-type(1) > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control:nth-of-type(2) > div.pf-v6-l-flex > div > button.pf-v6-c-menu-toggle";
    public static final String MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS = "body > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(2) > button.pf-v6-c-menu__item";

    public static final String MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-l-grid:nth-of-type(2) > div.pf-v6-l-grid__item:nth-of-type(1) > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > span.pf-v6-c-form-control > input";

    public static final String MPS_TPF_PARAMETERS_MESSAGES_UNIX_TIMESTAMP_INPUT = MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT;
    public static final String MPS_TPF_PARAMETERS_MESSAGES_TIMESTAMP_INPUT = "input[aria-label='Specify timestamp']";

    public static final String MPS_TPF_RETRIEVE_DROPDOWN_BUTTON = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-l-grid:nth-of-type(2) > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control:nth-of-type(2) > div.pf-v6-l-flex > div > button.pf-v6-c-menu-toggle";
    public static final String MPS_TPF_RETRIEVE_LIMIT_DROPDOWN_BUTTON = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-l-grid:nth-of-type(2) > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > button.pf-v6-c-menu-toggle";
    public static final String MPS_TPF_PARTITION_DROPDOWN_BUTTON = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-l-grid:nth-of-type(2) > div.pf-v6-l-grid__item:nth-of-type(3) > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control:nth-of-type(2) > button.pf-v6-c-menu-toggle";

    // Generic locator for whichever dropdown's menu is currently open (PatternFly portals exactly one at a time
    // to this same position under body) - filter by exact item text rather than position, since the Messages
    // dropdown has a non-selectable divider taking up a sibling slot, and numeric labels (5/50, 1/10) would
    // collide under substring matching.
    public static final String MPS_TPF_OPEN_MENU_ITEM_BUTTONS = "body > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item > button.pf-v6-c-menu__item";

    public static final String MPS_TPF_SEARCH_BUTTON = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control > div.pf-v6-c-form__actions > button.pf-v6-c-button:nth-of-type(1)";
    public static final String MPS_TPF_RESET_BUTTON = "body > div.pf-v6-c-panel:nth-of-type(2) > div.pf-v6-c-panel__main > div.pf-v6-c-panel__main-body > form.pf-v6-c-form > div.pf-v6-c-form__group > div.pf-v6-c-form__group-control > div.pf-v6-c-form__actions > button.pf-v6-c-button:nth-of-type(2)";
    public static final String MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-c-toolbar:nth-of-type(1) > div.pf-v6-c-toolbar__content:nth-of-type(1) > div.pf-v6-c-toolbar__content-section > div.pf-v6-c-toolbar__item:nth-of-type(1) > div > div.pf-v6-c-input-group > div.pf-v6-c-input-group__item:nth-of-type(3) > button.pf-v6-c-button";
    public static final String MPS_SEARCH_RESULTS_TABLE_ITEMS = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div:nth-of-type(2) > table.pf-v6-c-table > tbody.pf-v6-c-table__tbody > tr.pf-v6-c-table__tr";
    public static final String MPS_SEARCH_RESULTS_TABLE_NO_DATA = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-c-empty-state:nth-of-type(2) > div.pf-v6-c-empty-state__content > h2.pf-v6-c-title";


    public static String getTableRowItems(int nth) {
        return CssBuilder.joinLocators(new CssBuilder(MPS_SEARCH_RESULTS_TABLE_ITEMS).nth(nth).build(), CssSelectors.PAGES_AD_TABLE_ROW_ITEMS);
    }

    public static String getTableRowItem(int nthRow, int nthColumn) {
        return new CssBuilder(getTableRowItems(nthRow)).nth(nthColumn).build();
    }

    public static final String MPS_MESSAGE_SIDEBAR_VALUE_FORMAT = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__panel:nth-of-type(2) > div.pf-v6-c-drawer__panel-main:nth-of-type(2) > div.pf-v6-c-drawer__body:nth-of-type(2) > dl.pf-v6-c-description-list > div.pf-v6-c-description-list__group:nth-of-type(8) > dd.pf-v6-c-description-list__description > div.pf-v6-c-description-list__text";
    public static final String MPS_MESSAGE_SIDEBAR_SCHEMA_NAME = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__panel:nth-of-type(2) > div.pf-v6-c-drawer__panel-main:nth-of-type(2) > div.pf-v6-c-drawer__body:nth-of-type(2) > div > section.pf-v6-c-tab-content:nth-of-type(1) > div > div:nth-of-type(2) > p";
    public static final String MPS_MESSAGE_SIDEBAR_SCHEMA_CODE = "body > div > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(4) > div.pf-v6-c-page__main-body > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__panel:nth-of-type(2) > div.pf-v6-c-drawer__panel-main:nth-of-type(2) > div.pf-v6-c-drawer__body:nth-of-type(2) > div > section.pf-v6-c-tab-content:nth-of-type(1) > div > div.pf-v6-c-code-block:nth-of-type(1) > div.pf-v6-c-code-block__content > pre.pf-v6-c-code-block__pre > code.pf-v6-c-code-block__code";


}
