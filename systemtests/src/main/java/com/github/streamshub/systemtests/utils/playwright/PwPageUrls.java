package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.utils.resourceutils.ConsoleUtils;

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
}
