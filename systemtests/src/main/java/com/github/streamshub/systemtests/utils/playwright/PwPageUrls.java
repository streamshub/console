package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.utils.ConsoleUtils;

public class PwPageUrls {
    private PwPageUrls() {}

    public static String getConsoleUrl(TestCaseConfig tcc) {
        return ConsoleUtils.getConsoleUiUrl(tcc.namespace(), tcc.consoleInstanceName(), true);
    }

    private static String getKafkaBaseUrl(String kafkaName) {
        // Page redirects by kafka name now, but this might come handy later by ID
        return "/kafka/" + kafkaName;
    }

    public static String getOverviewPage(TestCaseConfig tcc, String kafkaName) {
        return getConsoleUrl(tcc) + getKafkaBaseUrl(kafkaName) + "/overview";
    }

    public static String getNodesPage(TestCaseConfig tcc, String kafkaName) {
        return getConsoleUrl(tcc) + getKafkaBaseUrl(kafkaName) + "/nodes";
    }
}
