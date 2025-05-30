package com.github.streamshub.systemtests.system;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.playwright.locators.CssSelectors;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TopicST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicST.class);

    @Test
    void testPaginationWithManyTopics() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int topicsCount = 150;

        // -----
        // 1. Setup topics and check none of them are viewed yet
        // -----
        KafkaTopicUtils.createTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName()), topicsCount,
            true, 1, 1, 1);


        LOGGER.info("Verify topics are present but none has been viewed just yet");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), topicsCount, topicsCount, topicsCount, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), topicsCount, topicsCount, 0, 0);

        LOGGER.info("Verify pagination on topics page");
        List<Integer> topicsPerPageList = List.of(10, 20, 50, 100);

        // Top navigation
        TopicChecks.checkPaginationPage(tcc, topicsCount, topicsPerPageList,
            CssSelectors.TOPICS_PAGE_TOP_PAGINATION_DROPDOWN_BUTTON, CssSelectors.TOPICS_PAGE_PAGINATION_DROPDOWN_ITEMS,
            CssSelectors.TOPICS_PAGE_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT,
            CssSelectors.TOPICS_PAGE_TOP_PAGINATION_NAV_PREV_BUTTON, CssSelectors.TOPICS_PAGE_TOP_PAGINATION_NAV_NEXT_BUTTON);

        // Bottom navigation
        TopicChecks.checkPaginationPage(tcc, topicsCount, topicsPerPageList,
            CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_DROPDOWN_BUTTON, CssSelectors.TOPICS_PAGE_PAGINATION_DROPDOWN_ITEMS,
            CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT,
            CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_NAV_PREV_BUTTON, CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_NAV_NEXT_BUTTON);
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
