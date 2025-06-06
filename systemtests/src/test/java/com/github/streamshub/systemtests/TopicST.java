package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.playwright.locators.CssSelectors;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.streamshub.systemtests.utils.playwright.locators.CssSelectors.getLocator;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicST.class);

    @Test
    void testPaginationWithManyTopics() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int topicsCount = 150;

        KafkaTopicUtils.createTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName()), topicsCount,
            true, 1, 1, 1);

        LOGGER.info("Verify topics are present but none has been viewed just yet");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), topicsCount, topicsCount, topicsCount, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), topicsCount, topicsCount, 0, 0);

        LOGGER.info("Verify pagination on topics page");
        List<Integer> topicsPerPageList = List.of(10, 20, 50, 100);

        LOGGER.info("Verify top navigation");
        TopicChecks.checkPaginationPage(tcc, topicsCount, topicsPerPageList,
            CssSelectors.TOPICS_PAGE_TOP_PAGINATION_DROPDOWN_BUTTON, CssSelectors.TOPICS_PAGE_PAGINATION_DROPDOWN_ITEMS,
            CssSelectors.TOPICS_PAGE_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT,
            CssSelectors.TOPICS_PAGE_TOP_PAGINATION_NAV_PREV_BUTTON, CssSelectors.TOPICS_PAGE_TOP_PAGINATION_NAV_NEXT_BUTTON);

        LOGGER.info("Verify bottom navigation");
        TopicChecks.checkPaginationPage(tcc, topicsCount, topicsPerPageList,
            CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_DROPDOWN_BUTTON, CssSelectors.TOPICS_PAGE_PAGINATION_DROPDOWN_ITEMS,
            CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT,
            CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_NAV_PREV_BUTTON, CssSelectors.TOPICS_PAGE_BOTTOM_PAGINATION_NAV_NEXT_BUTTON);
    }

    @Test
    void testRecentlyViewedTopics() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int topicsCount = 12;

        // Create topics
        List<KafkaTopic> topics = KafkaTopicUtils.createTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName()), topicsCount,
            true, 1, 1, 1);

        LOGGER.info("View topics, so that they show up in overview page");

        List<String> topicNames = topics.stream().map(kt -> kt.getMetadata().getName()).sorted().toList().subList(0, 3);

        for (String topicName : topicNames) {
            String topicId = ResourceUtils.getKubeResource(KafkaTopic.class, tcc.namespace(), topicName).getStatus().getTopicId();
            tcc.page().navigate(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultNavigateOpts());
            tcc.page().waitForURL(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultWaitForUrlOpts());
            PwUtils.waitForLocatorVisible(tcc, CssSelectors.MESSAGES_PAGE_EMPTY_BODY_CONTENT);
            assertTrue(CssSelectors.getLocator(tcc, CssSelectors.MESSAGES_PAGE_EMPTY_BODY_CONTENT).allInnerTexts().toString().contains(MessageStore.noDataTitle()));
        }

        LOGGER.info("Go to homepage and check the recently viewed card is not empty");
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check topics table for recently visited topics");
        PwUtils.waitForContainsText(tcc, getLocator(tcc, CssSelectors.C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(0), topicNames.get(2), false);
        PwUtils.waitForContainsText(tcc, getLocator(tcc, CssSelectors.C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(1), topicNames.get(1), false);
        PwUtils.waitForContainsText(tcc, getLocator(tcc, CssSelectors.C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(2), topicNames.get(0), false);

        LOGGER.info("Delete topics");
        KubeResourceManager.get().deleteResourceWithWait(topics.toArray(new KafkaTopic[0]));

        LOGGER.info("Check that in recently visited topics table topics are still present");
        PwUtils.waitForContainsText(tcc, getLocator(tcc, CssSelectors.C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(0), topicNames.get(2), false);
        PwUtils.waitForContainsText(tcc, getLocator(tcc, CssSelectors.C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(1), topicNames.get(1), false);
        PwUtils.waitForContainsText(tcc, getLocator(tcc, CssSelectors.C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(2), topicNames.get(0), false);

        LOGGER.info("Check that one of the recently viewed topics page cannot be found as it's deleted");
        getLocator(tcc, CssSelectors.C_OVERVIEW_PAGE_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(0).click();
        // Verify empty body message
        assertTrue(getLocator(tcc, CssSelectors.MESSAGES_PAGE_EMPTY_BODY_CONTENT).innerText().contains("Resource not found"));
    }

    @Test
    void testDisplayAndFilterAllTopicStates() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int replicatedTopicsCount = 5;
        final int unmanagedReplicatedTopicsCount = 2;
        final int underReplicatedTopicsCount = 3;
        final int unavailableTopicsCount = 2;
        final int totalTopicsCount = replicatedTopicsCount + unmanagedReplicatedTopicsCount + underReplicatedTopicsCount + unavailableTopicsCount;

        final String replicatedTopicsPrefix = KafkaNamingUtils.topicPrefixName(tcc.kafkaName()) + "-replicated";
        final String unmanagedReplicatedTopicsPrefix = KafkaNamingUtils.topicPrefixName(tcc.kafkaName()) + "-unmanaged-rep";
        final String underReplicatedTopicsPrefix =  KafkaNamingUtils.topicPrefixName(tcc.kafkaName()) + "-underreplicated";
        final String unavailableTopicsPrefix = KafkaNamingUtils.topicPrefixName(tcc.kafkaName()) + "-unavailable";

        final int scaledUpBrokerReplicas = Constants.REGULAR_BROKER_REPLICAS + 1;

        LOGGER.info("Check default UI state regarding topics");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0);

        LOGGER.info("Create all types of topics");
        List<KafkaTopic> replicatedTopics = KafkaTopicUtils.createTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), replicatedTopicsPrefix, replicatedTopicsCount, true, 1, 1, 1);
        List<String> unmanagedReplicatedTopics = KafkaTopicUtils.createUnmanagedTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), unmanagedReplicatedTopicsPrefix, unmanagedReplicatedTopicsCount, tcc.messageCount(), 1, 1, 1);
        List<KafkaTopic> underReplicatedTopics = KafkaTopicUtils.createUnderReplicatedTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), underReplicatedTopicsPrefix, underReplicatedTopicsCount, tcc.messageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas);
        List<KafkaTopic> unavailableTopics = KafkaTopicUtils.createUnavailableTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), unavailableTopicsPrefix, unavailableTopicsCount, tcc.messageCount(), 1, 1, 1);

        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), totalTopicsCount, totalTopicsCount, replicatedTopicsCount + unmanagedReplicatedTopicsCount, underReplicatedTopicsCount, unavailableTopicsCount);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), totalTopicsCount, replicatedTopicsCount + unmanagedReplicatedTopicsCount, underReplicatedTopicsCount, unavailableTopicsCount);

        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        TopicChecks.checkTopicsFilterByName(tcc, unmanagedReplicatedTopics);
        TopicChecks.checkTopicsFilterById(tcc, replicatedTopics.stream().map(kt -> kt.getMetadata().getName()).toList());
        TopicChecks.checkTopicsFilterByStatus(tcc, underReplicatedTopics.stream().map(kt -> kt.getMetadata().getName()).toList(), TopicStatus.UNDER_REPLICATED);
        TopicChecks.checkTopicsFilterByStatus(tcc, unavailableTopics.stream().map(kt -> kt.getMetadata().getName()).toList(), TopicStatus.OFFLINE);
    }

    @Test
    void testSortTopics() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final int replicatedTopicsCount = 5;
        final int unavailableTopicsCount = 4;

        final String replicatedTopicsPrefix = KafkaNamingUtils.topicPrefixName(tcc.kafkaName()) + "-replicated";
        final String unavailableTopicsPrefix = KafkaNamingUtils.topicPrefixName(tcc.kafkaName()) + "-unavailable";

        LOGGER.info("Check default UI state regarding topics");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0);

        LOGGER.info("Create all types of topics");
        List<KafkaTopic> replicatedTopics = KafkaTopicUtils.createTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), replicatedTopicsPrefix, replicatedTopicsCount, true, 1, 1, 1);
        List<KafkaTopic> unavailableTopics = KafkaTopicUtils.createUnavailableTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), unavailableTopicsPrefix, unavailableTopicsCount, tcc.messageCount(), 1, 1, 1);

        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());
        LOGGER.info("Sort topics offline topics by name");
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, TopicStatus.OFFLINE);
        PwUtils.waitForLocatorCount(tcc, unavailableTopicsCount, CssSelectors.TOPICS_PAGE_TABLE_ROWS, false);
        // Sort by name
        TopicsTestUtils.selectSortBy(tcc, CssSelectors.TOPICS_PAGE_TABLE_HEADER_SORT_BY_NAME, CssSelectors.TOPICS_PAGE_TABLE_HEADER_SORT_BY_NAME_BUTTON, "descending");
        PwUtils.waitForLocatorCount(tcc, unavailableTopicsCount, CssSelectors.TOPICS_PAGE_TABLE_ROWS, false);
        assertTrue(CssSelectors.getLocator(tcc, CssSelectors.getTopicsPageTableRowItems(0)).allInnerTexts().toString()
            .contains(unavailableTopics.stream().map(kt -> kt.getMetadata().getName()).sorted().toList().get(unavailableTopicsCount - 1)));

        tcc.page().click(CssSelectors.TOPICS_PAGE_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);

        LOGGER.info("Sort replicated topics by storage");
        // Produce more messages for the last fullyReplicated topic - use higher message count number to take more storage
        KafkaClients clients = KafkaClientsUtils.scramShaPlainTextClientBuilder(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()),
             replicatedTopics.stream().map(kt -> kt.getMetadata().getName()).sorted().toList().get(replicatedTopicsCount - 1), Constants.MESSAGE_COUNT_HIGH).build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);
        // Filter
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, TopicStatus.FULLY_REPLICATED);
        PwUtils.waitForLocatorCount(tcc, replicatedTopicsCount, CssSelectors.TOPICS_PAGE_TABLE_ROWS, false);
        // Sort by storage
        TopicsTestUtils.selectSortBy(tcc, CssSelectors.TOPICS_PAGE_TABLE_HEADER_SORT_BY_STORAGE, CssSelectors.TOPICS_PAGE_TABLE_HEADER_SORT_BY_STORAGE_BUTTON, "descending");
        PwUtils.waitForLocatorCount(tcc, replicatedTopicsCount, CssSelectors.TOPICS_PAGE_TABLE_ROWS, false);
        assertTrue(CssSelectors.getLocator(tcc, CssSelectors.getTopicsPageTableRowItems(0)).allInnerTexts().toString()
            .contains(replicatedTopics.stream().map(kt -> kt.getMetadata().getName()).sorted().toList().get(replicatedTopicsCount - 1)));
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
