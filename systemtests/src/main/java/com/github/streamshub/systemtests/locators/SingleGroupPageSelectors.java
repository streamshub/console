package com.github.streamshub.systemtests.locators;

public class SingleGroupPageSelectors {
    private SingleGroupPageSelectors() {}

    public static final String SGPS_PAGE_HEADER_NAME = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div:nth-of-type(1) > h1.pf-v6-c-title";

    public static final String SGPS_HEADER_BREADCRUMB_GROUP_NAME = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-breadcrumb:nth-of-type(1) > div.pf-v6-c-page__main-body > div.pf-v6-c-page__main-body > nav.pf-v6-c-breadcrumb > ol.pf-v6-c-breadcrumb__list > li.pf-v6-c-breadcrumb__item:nth-of-type(4)";

    public static final String SGPS_RESET_CONSUMER_OFFSET_BUTTON = "body > div#root > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div:nth-of-type(2) > button.pf-v6-c-button > span.pf-v6-c-button__text";

    public static final String SGPS_RESET_PAGE_TOPIC_NAME_DROPDOWN_RESULT = "body.pf-v6-c-backdrop__open > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content:nth-of-type(2) > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(2) > button.pf-v6-c-menu__item > span.pf-v6-c-menu__item-main > span.pf-v6-c-menu__item-text";

    public static final String SGPS_RESET_PAGE_OFFSET_DROPDOWN_BUTTON = "body.pf-v6-c-backdrop__open > div:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body:nth-of-type(2) > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-c-form__group:nth-of-type(2) > div.pf-v6-c-form__group-control:nth-of-type(2) > button.pf-v6-c-menu-toggle";


    public static final String SGPS_RESET_PAGE_OFFSET_EARLIEST_OFFSET = "body.pf-v6-c-backdrop__open > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(1) > button.pf-v6-c-menu__item > span.pf-v6-c-menu__item-main > span.pf-v6-c-menu__item-text";

    public static final String SGPS_RESET_PAGE_OFFSET_LATEST_OFFSET = "body.pf-v6-c-backdrop__open > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(2) > button.pf-v6-c-menu__item > span.pf-v6-c-menu__item-main > span.pf-v6-c-menu__item-text";

    public static final String SGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_SPECIFIC_DATETIME_OFFSET_ISO = "body.pf-v6-c-backdrop__open > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(3) > button.pf-v6-c-menu__item > span.pf-v6-c-menu__item-main > span.pf-v6-c-menu__item-text";

    public static final String SGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_SPECIFIC_DATETIME_OFFSET_UNIX = "body.pf-v6-c-backdrop__open > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(4) > button.pf-v6-c-menu__item > span.pf-v6-c-menu__item-main > span.pf-v6-c-menu__item-text";

    public static final String SGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_DELETE_COMMITED_OFFSETS = "body.pf-v6-c-backdrop__open > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__content > ul.pf-v6-c-menu__list > li.pf-v6-c-menu__list-item:nth-of-type(5) > button.pf-v6-c-menu__item > span.pf-v6-c-menu__item-main > span.pf-v6-c-menu__item-text";

    public static final String SGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_INPUT_UNIX = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body:nth-of-type(2) > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-c-form__group:nth-of-type(3) > div.pf-v6-c-form__group-control:nth-of-type(2) > div:nth-of-type(1) > div > span.pf-v6-c-form-control > input";

    public static final String SGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_INPUT_ISO = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body:nth-of-type(2) > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(2) > div.pf-v6-c-form__group:nth-of-type(3) > div.pf-v6-c-form__group-control:nth-of-type(2) > div:nth-of-type(1) > div:nth-of-type(2) > span.pf-v6-c-form-control > input";

    public static final String SGPS_RESET_PAGE_OFFSET_RESET_BUTTON = "body.pf-v6-c-backdrop__open > div#pf-modal-part-2.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div#pf-modal-part-1.pf-v6-c-modal-box > footer.pf-v6-c-modal-box__footer > button.pf-v6-c-button:nth-of-type(1) > span.pf-v6-c-button__text";

    public static final String SGPS_RESET_PAGE_OFFSET_DRY_RUN_BUTTON = "body.pf-v6-c-backdrop__open > div#pf-modal-part-2.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div#pf-modal-part-1.pf-v6-c-modal-box > footer.pf-v6-c-modal-box__footer > button.pf-v6-c-button:nth-of-type(2) > span.pf-v6-c-button__text";

    public static final String SGPS_DRY_RUN_COMMAND = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body > div.pf-v6-c-expandable-section > div.pf-v6-c-expandable-section__content > div.pf-v6-c-code-block > div.pf-v6-c-code-block__content > pre.pf-v6-c-code-block__pre > code.pf-v6-c-code-block__code";
    public static final String SGPS_DRY_RUN_COMMAND_DROPDOWN = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body > div.pf-v6-c-expandable-section > div.pf-v6-c-expandable-section__toggle > button.pf-v6-c-button > span.pf-v6-c-button__text";
    public static final String SGPS_CANCEL_DRY_RUN_OFFSET_BUTTON = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(3) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > footer.pf-v6-c-modal-box__footer > button.pf-v6-c-button > span.pf-v6-c-button__text";

    public static final String SGPS_SELECTED_TOPIC_DROPDOWN_BUTTON = "body.pf-v6-c-backdrop__open > div.pf-v6-c-backdrop:nth-of-type(2) > div.pf-v6-l-bullseye > div.pf-v6-c-modal-box > div.pf-v6-c-modal-box__body:nth-of-type(2) > form.pf-v6-c-form > section.pf-v6-c-form__section:nth-of-type(1) > div.pf-v6-c-form__group:nth-of-type(2) > div.pf-v6-c-form__group-control:nth-of-type(2) > button.pf-v6-c-menu-toggle";

    public static final String SGPS_SELECTED_TOPIC_DROPDOWN_SEARCH_INPUT = "body.pf-v6-c-backdrop__open > div.pf-v6-c-menu:nth-of-type(3) > div.pf-v6-c-menu__search:nth-of-type(1) > div.pf-v6-c-menu__search-input > div.pf-v6-c-text-input-group > div.pf-v6-c-text-input-group__main:nth-of-type(1) > span.pf-v6-c-text-input-group__text > input.pf-v6-c-text-input-group__text-input";

}
