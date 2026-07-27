/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators;

/** Accessible names, visible text, and placeholder strings used by locator classes in this package to find elements by role/label/text. */
public final class TextConstants {
    private TextConstants() {}

    // Shared across multiple pages
    public static final String SEARCH = "Search";
    public static final String SEARCH_INPUT = "Search input";
    public static final String CANCEL = "Cancel";
    public static final String CONFIRM = "Confirm";
    public static final String CLEAR_ALL_FILTERS = "Clear all filters";
    public static final String RESET_OFFSETS = "Reset offsets";
    public static final String RESUME = "Resume";
    public static final String NOT_AUTHORIZED = "Not Authorized";
    public static final String NAME = "Name";
    public static final String STORAGE = "Storage";
    public static final String GO_TO_PREVIOUS_PAGE = "Go to previous page";
    public static final String GO_TO_NEXT_PAGE = "Go to next page";
    public static final String DETAILS = "Details";
    public static final String ACTIONS = "Actions";
    public static final String VIEW = "View";

    // Masthead
    public static final String KAFKA_CLUSTERS = "Kafka Clusters";
    public static final String LOGOUT = "Logout";

    // Groups page
    public static final String GROUPS = "Groups";
    public static final String FILTER_BY_NAME = "Filter by name";
    public static final String NO_GROUPS_AVAILABLE = "No groups available";

    // Nodes page
    public static final String FORBIDDEN_STATUS_CODE = "403";
    public static final String NODE_POOL = "Node Pool";
    public static final String ROLES = "Roles";
    public static final String STATUS = "Status";
    public static final String REBALANCES_TABLE = "Rebalances table";

    // Kafka Users page
    public static final String AUTHORIZATION = "Authorization";

    // Topics page
    public static final String NO_RESULTS_FOUND = "No results found";

    // Kafka Connect page
    public static final String CONNECTORS = "Connectors";
    public static final String CLUSTERS = "Clusters";
    public static final String KAFKA_CONNECT = "Kafka Connect";
    public static final String CONNECTORS_TABLE = "Connectors table";
    public static final String CONNECT_CLUSTERS_TABLE = "Connect clusters table";

    // Single Group / reset-offset page
    public static final String EARLIEST_OFFSET = "Earliest offset";
    public static final String LATEST_OFFSET = "Latest offset";
    public static final String SPECIFIC_DATETIME_ISO = "Specific date/time (ISO 8601)";
    public static final String SPECIFIC_DATETIME_UNIX = "Specific date/time (Unix epoch timestamp)";
    public static final String DELETE_COMMITTED_OFFSETS = "Delete committed offsets";
    public static final String DRY_RUN = "Dry run";
    public static final String CLI_COMMAND = "CLI command";

    // Messages page
    public static final String MESSAGES_TABLE = "Messages table";
    public static final String NO_MESSAGES_DATA = "No messages data";
    public static final String OPEN_ADVANCED_SEARCH = "Open advanced search";
    public static final String SPECIFY_OFFSET = "Specify offset";
    public static final String SPECIFY_TIMESTAMP = "Specify timestamp";
    public static final String SPECIFY_UNIX_TIMESTAMP = "Specify Unix timestamp";
    public static final String RESET = "Reset";
    public static final String VALUE_FORMAT = "Value format";

    // Cluster Overview page
    public static final String PAUSE_OR_RESUME_RECONCILIATION_PATTERN = "Pause Reconciliation|Resume Reconciliation";
}
