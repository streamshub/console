package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.utils.resourceutils.ConsoleUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PwPageUrls {
    private PwPageUrls() {}

    public static String getConsoleUrl(TestCaseConfig tcc) {
        return ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true);
    }

    public static String getKafkaBaseUrl(TestCaseConfig tcc, String kafkaName) {
        // Page redirects by kafka name now, but this might come handy later by ID
        return getConsoleUrl(tcc) + "/kafka/" + kafkaName;
    }

    public static String getKafkaLoginPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/login";
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

    public static String getSingleTopicConsumerGroupsPage(TestCaseConfig tcc, String kafkaName, String topicId) {
        return getSingleTopicPage(tcc, kafkaName, topicId) + "/consumer-groups";
    }

    public static String getMessagesPage(TestCaseConfig tcc, String kafkaName, String topicId) {
        return getSingleTopicPage(tcc, kafkaName, topicId) + "/messages";
    }

    public static String getConsumerGroupsPage(TestCaseConfig tcc, String kafkaName, String consumerGroupName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/consumer-groups/" + consumerGroupName;
    }

    public static String getConsumerGroupsResetOffsetPage(TestCaseConfig tcc, String kafkaName, String consumerGroupName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/consumer-groups/" + consumerGroupName + "/reset-offset";
    }

    public static String getKafkaConnectPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/kafka-connect";
    }

    public static String getKafkaRebalancePage(TestCaseConfig tcc, String kafkaName) {
        return getNodesPage(tcc, kafkaName) + "/rebalances";
    }

    public static String getKafkaUsersPage(TestCaseConfig tcc, String kafkaName) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/kafka-users";
    }

    public static String getSingleKafkaUserPage(TestCaseConfig tcc, String kafkaName, String namespace, String kafkaUser) {
        return getKafkaBaseUrl(tcc, kafkaName) + "/kafka-users/" +
            Base64.getEncoder().encodeToString(namespace.getBytes(StandardCharsets.UTF_8)) +
            "," +
            Base64.getEncoder().encodeToString(kafkaUser.getBytes(StandardCharsets.UTF_8));
    }
}
