/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators;

/** {@code data-ouia-component-id} values set by webui components, referenced by the locator classes in this package. */
public final class OuiaIdConstants {
    private OuiaIdConstants() {}

    // Masthead
    public static final String TOTAL_AVAILABLE_KAFKA_COUNT = "total-available-kafka-count";

    // Kafka Users page
    public static final String KAFKA_USERS_LISTING = "kafka-users-listing";
    public static final String USER_DETAIL_NAME = "user-detail-name";
    public static final String USER_DETAIL_USERNAME = "user-detail-username";
    public static final String USER_DETAIL_AUTHENTICATION = "user-detail-authentication";
    public static final String USER_DETAIL_CREATION_TIME = "user-detail-creation-time";

    // Nodes page
    public static final String NODES_LISTING = "nodes-listing";
    public static final String NODES_TOTAL_BADGE = "nodes-total-badge";
    public static final String NODES_HEALTHY_BADGE = "nodes-healthy-badge";
    public static final String NODES_UNHEALTHY_BADGE = "nodes-unhealthy-badge";
    public static final String OVERVIEW_TOTAL_NODES = "overview-total-nodes";
    public static final String OVERVIEW_CONTROLLER_ROLE = "overview-controller-role";
    public static final String OVERVIEW_BROKER_ROLE = "overview-broker-role";
    public static final String OVERVIEW_LEAD_CONTROLLER = "overview-lead-controller";
    public static final String REBALANCE_AUTO_APPROVAL_VALUE = "rebalance-auto-approval-value";
    public static final String REBALANCE_MODE_VALUE = "rebalance-mode-value";
    public static final String REBALANCE_DATA_TO_MOVE_MB = "rebalance-data-to-move-mb";
    public static final String REBALANCE_MONITORED_PARTITIONS_PERCENTAGE = "rebalance-monitored-partitions-percentage";
    public static final String REBALANCE_NUM_REPLICA_MOVEMENTS = "rebalance-num-replica-movements";
    public static final String REBALANCE_BALANCEDNESS_BEFORE = "rebalance-balancedness-before";
    public static final String REBALANCE_BALANCEDNESS_AFTER = "rebalance-balancedness-after";

    // Groups page
    public static final String GROUPS_LISTING = "groups-listing";

    // Messages page
    public static final String HAS_WORDS_INPUT = "has-words-input";
    public static final String WHERE_DROPDOWN_BUTTON = "where-dropdown-button";
    public static final String MESSAGES_FROM_DROPDOWN_BUTTON = "messages-from-dropdown-button";
    public static final String RETRIEVE_TYPE_DROPDOWN_BUTTON = "retrieve-type-dropdown-button";
    public static final String RETRIEVE_LIMIT_DROPDOWN_BUTTON = "retrieve-limit-dropdown-button";
    public static final String PARTITION_DROPDOWN_BUTTON = "partition-dropdown-button";

    // Kafka Dashboard page
    public static final String KAFKA_CLUSTERS_TABLE = "kafka-clusters-table";

    // Single Group page
    public static final String BREADCRUMB_GROUP_NAME = "breadcrumb-group-name";
    public static final String DRY_RUN_COMMAND_TEXT = "dry-run-command-text";

    // Topics page
    public static final String TOPICS_TABLE = "topics-table";
    public static final String TOPICS_TOTAL_BADGE = "topics-total-badge";
    public static final String TOPICS_FULLY_REPLICATED_BADGE = "topics-fully-replicated-badge";
    public static final String TOPICS_UNDER_REPLICATED_BADGE = "topics-under-replicated-badge";
    public static final String TOPICS_OFFLINE_BADGE = "topics-offline-badge";
    public static final String TOPIC_GROUPS_LISTING = "topic-groups-listing";

    // Cluster Overview page
    public static final String RECONCILIATION_PAUSED_BANNER = "reconciliation-paused-banner";
    public static final String CLUSTER_KAFKA_VERSION = "cluster-kafka-version";
    public static final String CLUSTER_WARNINGS_LIST = "cluster-warnings-list";
    public static final String RECENT_TOPICS_LIST = "recent-topics-list";
    public static final String FULLY_REPLICATED_STATUS_LINK = "fully-replicated-status-link";
    public static final String UNDER_REPLICATED_STATUS_LINK = "under-replicated-status-link";
    public static final String OFFLINE_STATUS_LINK = "offline-status-link";
}
