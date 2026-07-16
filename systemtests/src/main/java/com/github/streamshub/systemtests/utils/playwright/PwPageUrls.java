package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PwPageUrls {
    private PwPageUrls() {}

    public static String getConsoleUrl(TestCaseConfig tcc) {
        return ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true);
    }

    public static String getKafkaBaseUrl(TestCaseConfig tcc, String kafkaName) {
        // Page redirects by kafka name now, but this might come handy later by ID
        return getConsoleUrl(tcc) + "/kafka/" + kafkaName;
    }

    public static String getOverviewPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/overview";
    }

    public static String getNodesPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/nodes";
    }

    public static String getTopicsPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/topics";
    }

    public static String getSingleTopicPage(TestCaseConfig tcc, String kafkaName, String topicId) {
        return getTopicsPage(tcc, kafkaName) + "/" + topicId;
    }

    public static String getSingleTopicGroupsPage(TestCaseConfig tcc, String kafkaName, String topicId) {
        return getSingleTopicPage(tcc, kafkaName, topicId) + "/groups";
    }

    public static String getMessagesPage(TestCaseConfig tcc, String kafkaName, String topicId) {
        return getSingleTopicPage(tcc, kafkaName, topicId) + "/messages";
    }

    public static String getGroupsMembersPage(TestCaseConfig tcc, String kafkaName, String consumerGroupName) {
        consumerGroupName = consumerGroupName.isBlank() ? "" : "/" + consumerGroupName;
        return getGroupsPage(tcc, kafkaName) + consumerGroupName + "/members";
    }

    public static String getGroupsPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/groups";
    }

    public static String getKafkaConnectPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/connect";
    }

    public static String getKafkaConnectClusterPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaConnectPage(tcc, kafkaName) + "/clusters";
    }

    public static String getKafkaConnectClusterPage(TestCaseConfig tcc, String kafkaName, String namespace, String connectName) {
        return getKafkaConnectClusterPage(tcc, kafkaName) + "/" +
            Base64.getEncoder().encodeToString(namespace.getBytes(StandardCharsets.UTF_8)) +
            "/" +
            Base64.getEncoder().encodeToString(connectName.getBytes(StandardCharsets.UTF_8));
    }

    public static String getKafkaRebalancePage(TestCaseConfig tcc, String kafkaName) {
        return getNodesPage(tcc, kafkaName) + "/rebalances";
    }

    public static String getSingleKafkaUserPage(TestCaseConfig tcc, String kafkaName, String namespace, String kafkaUser) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/users/" +
            Base64.getEncoder().encodeToString(namespace.getBytes(StandardCharsets.UTF_8)) +
            "," +
            Base64.getEncoder().encodeToString(kafkaUser.getBytes(StandardCharsets.UTF_8));
    }
}
