/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators;

/** CSS {@code #id} selectors used directly by locator classes in this package. */
public final class IdConstants {
    private IdConstants() {}

    public static final String MASTHEAD_TOOLBAR = "#masthead-toolbar";
    public static final String USERNAME_FILTER = "#username-filter";
    public static final String FILTER_TYPE_SELECT = "#filter-type-select";
    public static final String STATUS_FILTER_SELECT = "#status-filter-select";
    public static final String TOPIC_SELECT = "#topic-select";
    public static final String OFFSET_SELECT = "#offset-select";
    public static final String DATETIME_INPUT = "#datetime-input";
    public static final String CLUSTER_WARNINGS_TOGGLE = "#cluster-warnings-toggle";

    /** PatternFly's own default {@code widgetId} ("options-menu") for the Pagination component, unchanged by this app. */
    public static final String PAGINATION_TOP_TOGGLE = "#options-menu-top-toggle";
    public static final String PAGINATION_BOTTOM_TOGGLE = "#options-menu-bottom-toggle";

    /** TopicsFilterToolbar.tsx sets {@code id={`${selectedFilterType}-filter-input`}} - an attribute-suffix match, not a full #id selector. */
    public static final String FILTER_INPUT_ID_SUFFIX = "-filter-input";
}
