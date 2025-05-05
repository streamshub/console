/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.utils.playwright.locators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextResources {

    @SuppressWarnings("checkstyle:StaticVariableName")
    private static JsonNode JSON_RESOURCES;

    static {
        try {
            String source = Files.readString(Path.of(System.getProperty("user.dir"), "resources", "text", "en.json"));
            JSON_RESOURCES = new ObjectMapper().readTree(source);

            source = source
                .replace("{product}", getProductName())
                .replace("{brand}", getBrand());

            JSON_RESOURCES = new ObjectMapper().readTree(source);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // First level
    static final String COMMON = "common";
    static final String FEEDBACK = "feedback";
    static final String LEARNING = "learning";
    static final String TABLE = "Table";
    static final String HOMEPAGE = "homepage";
    static final String OVERVIEW = "overview";
    static final String NODES = "nodes";
    static final String NODE_CONFIG_TABLE = "node-config-table";
    static final String TOPICS = "topics";
    static final String TOPIC_CREATOR = "topic-creator";
    static final String MESSAGE_BROWSER = "message-browser";
    static final String MESSAGE_PRODUCER = "message-producer";
    static final String LOADING = "Loading";
    static final String MANAGED_TOPIC_LABEL = "ManagedTopicLabel";
    static final String ADVANCED_SEARCH = "AdvancedSearch";
    static final String APP_LAYOUT = "AppLayout";
    static final String CLUSTER_CARD = "ClusterCard";
    static final String CLUSTER_CHARTS_CARD = "ClusterChartsCard";
    static final String TOPIC_METRICS_CARD = "topicMetricsCard";
    static final String CLUSTER_CONNECTION_DETAILS = "ClusterConnectionDetails";
    static final String CLUSTER_DRAWER = "ClusterDrawer";
    static final String CLUSTERS_TABLE = "ClustersTable";
    static final String CONFIG_TABLE = "ConfigTable";
    static final String CONNECT_BUTTON = "ConnectButton";
    static final String ALERT_TOPIC_GONE = "AlertTopicGone";
    static final String ALERT_CONTINUOUS_MODE = "AlertContinuousMode";
    static final String CONSUMER_GROUPS_TABLE = "ConsumerGroupsTable";
    static final String CREATE_TOPIC = "CreateTopic";
    static final String DISTRIBUTION_CHART = "DistributionChart";
    static final String EMPTY_STATE_NO_TOPICS = "EmptyStateNoTopics";
    static final String APPLICATION_ERROR = "ApplicationError";
    static final String CLUSTER_OVERVIEW = "ClusterOverview";
    static final String MEMBER_TABLE = "MemberTable";
    static final String CONSUMER_GROUP = "ConsumerGroup";

    // Second level
    static final String PRODUCT = "product";
    static final String TITLE = "title";
    static final String LABELS = "labels";
    static final String LINKS = "links";
    static final String BRAND = "brand";

    // ------------
    // Overview page
    // ------------

    public static String getProductName() {
        return JSON_RESOURCES.get(COMMON).get(PRODUCT).textValue();
    }

    public static String getBrand() {
        return JSON_RESOURCES.get(COMMON).get(BRAND).textValue();
    }

    // --------------
    // Overview Page


    // -----
    // Page title

    public static String overviewPageTitle() {
        return JSON_RESOURCES.get(HOMEPAGE).get("page_header").textValue();
    }

    public static String overviewPageSubtitle() {
        return JSON_RESOURCES.get(HOMEPAGE).get("page_subtitle").textValue();
    }
    // -----
    // Cluster card
    public static String overviewClusterCardSubtitle() {
        return JSON_RESOURCES.get(HOMEPAGE).get("connected_kafka_clusters").textValue();
    }

    // Topics card
    public static String overviewRecentTopicsCardTitle() {
        return JSON_RESOURCES.get(HOMEPAGE).get("recently_viewed_topics_header").textValue();
    }

    public static String overviewRecentTopicsCardSubtitle() {
        return JSON_RESOURCES.get(HOMEPAGE).get("last_accessed_topics").textValue();
    }

    public static String overviewRecentTopicsCardPopover() {
        return JSON_RESOURCES.get(HOMEPAGE).get("recently_viewed_topics_header_popover").textValue();
    }

    public static String overviewRecentTopicsCardLinkName() {
        return JSON_RESOURCES.get(LEARNING).get(LABELS).get("topicOperatorUse").textValue();
    }
    public static String overviewRecentTopicsCardLink() {
        return JSON_RESOURCES.get(LEARNING).get(LINKS).get("topicOperatorUse").textValue();
    }

    // -----
    // Learning links
    public static String overviewLearningCardTitle() {
        return JSON_RESOURCES.get(HOMEPAGE).get("recommended_learning_resources_label").textValue();
    }

    public static String overviewLearningCardOverview() {
        return JSON_RESOURCES.get(LEARNING).get(LABELS).get(OVERVIEW).textValue();
    }

    public static String overviewLearningCardOverviewLink() {
        return JSON_RESOURCES.get(LEARNING).get(LINKS).get(OVERVIEW).textValue();
    }

    public static String overviewLearningCardTopic() {
        return JSON_RESOURCES.get(LEARNING).get(LABELS).get("topicOperatorUse").textValue();
    }

    public static String overviewLearningCardTopicLink() {
        return JSON_RESOURCES.get(LEARNING).get(LINKS).get("topicOperatorUse").textValue();
    }

    // ------------
    // Cluster overview page
    // ------------

    public static String clusterOverviewPageSubtitle() {
        return JSON_RESOURCES.get(CLUSTER_OVERVIEW).get("description").textValue();
    }

    public static String clusterOverviewConnectButton() {
        return JSON_RESOURCES.get(CONNECT_BUTTON).get("cluster_connection_details").textValue();
    }

    public static String clusterOverviewClusterErrorsButton() {
        return JSON_RESOURCES.get(CLUSTER_CARD).get("cluster_errors_and_warnings").textValue();
    }

    public static String clusterOverviewClusterNoMessages() {
        return JSON_RESOURCES.get(CLUSTER_CARD).get("no_messages").textValue();
    }

    public static String clusterOverviewMetricsDisk() {
        return JSON_RESOURCES.get(CLUSTER_CHARTS_CARD).get("used_disk_space").textValue();
    }

    public static String clusterOverviewMetricsCpu() {
        return JSON_RESOURCES.get(CLUSTER_CHARTS_CARD).get("cpu_usage").textValue();
    }

    public static String clusterOverviewMetricsMemory() {
        return JSON_RESOURCES.get(CLUSTER_CHARTS_CARD).get("memory_usage").textValue();
    }

    public static String clusterOverviewTopicMetricsTitle() {
        return JSON_RESOURCES.get(TOPIC_METRICS_CARD).get("topic_metric").textValue();
    }

    public static String clusterOverviewTopicMetricsSubtitle() {
        return JSON_RESOURCES.get(TOPIC_METRICS_CARD).get("topics_bytes_incoming_and_outgoing").textValue();
    }

    // Topics page
    public static String noResultsMatch() {
        return JSON_RESOURCES.get(TABLE).get("no_results_match_the_filter_criteria_clear_all_fil").textValue();
    }

    public static String clearAllFilters() {
        return JSON_RESOURCES.get(TABLE).get("clear_all_filters").textValue();
    }
}
