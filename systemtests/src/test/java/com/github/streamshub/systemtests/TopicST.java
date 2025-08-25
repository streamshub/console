package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.MessagesPageSelectors;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaCmdUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.wildfly.common.Assert.assertTrue;

@Tag(TestTags.REGRESSION)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TopicST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicST.class);
    private static TestCaseConfig tcc;

    // Topics
    // Note for pagination scenario it's best to have total of 150 topics
    private static final int REPLICATED_TOPICS_COUNT = 140;
    private static final int UNMANAGED_REPLICATED_TOPICS_COUNT = 5;
    private static final int TOTAL_REPLICATED_TOPICS_COUNT = REPLICATED_TOPICS_COUNT + UNMANAGED_REPLICATED_TOPICS_COUNT;
    //
    private static final int UNDER_REPLICATED_TOPICS_COUNT = 3;
    private static final int UNAVAILABLE_TOPICS_COUNT = 2;
    private static final int TOTAL_TOPICS_COUNT = TOTAL_REPLICATED_TOPICS_COUNT + UNDER_REPLICATED_TOPICS_COUNT + UNAVAILABLE_TOPICS_COUNT;
    // Topic Prefixes
    private static final String REPLICATED_TOPICS_PREFIX = "replicated";
    private static final String UNMANAGED_REPLICATED_TOPICS_PREFIX = "unmanaged-replicated";
    private static final String UNDER_REPLICATED_TOPICS_PREFIX = "underreplicated";
    private static final String UNAVAILABLE_TOPICS_PREFIX = "unavailable";
    
    /**
     * Tests the pagination functionality on the topics page when a large number of topics are present.
     *
     * <p>The test creates 150 Kafka topics and verifies their presence on both the overview and topics pages.</p>
     * <p>It then tests the pagination UI components (both top and bottom), using various pagination sizes (10, 20, 50, 100 topics per page).</p>
     * <p>Navigation buttons and dropdowns are validated to ensure correct page transitions and content display.</p>
     *
     * <p>This ensures that the pagination mechanism in the topics page works correctly and is user-friendly at scale.</p>
     */
    @Test
    @Order(Order.DEFAULT)
    void testPaginationWithManyTopics() {
        LOGGER.info("Verify topics are displayed");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        LOGGER.info("Verify pagination on topics page");
        List<Integer> topicsPerPageList = List.of(10, 20, 50, 100);

        LOGGER.info("Verify top navigation");
        TopicChecks.checkPaginationPage(tcc, TOTAL_REPLICATED_TOPICS_COUNT, topicsPerPageList,
            TopicsPageSelectors.TPS_TOP_PAGINATION_DROPDOWN_BUTTON, TopicsPageSelectors.TPS_PAGINATION_DROPDOWN_ITEMS,
            TopicsPageSelectors.TPS_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT,
            TopicsPageSelectors.TPS_TOP_PAGINATION_NAV_PREV_BUTTON, TopicsPageSelectors.TPS_TOP_PAGINATION_NAV_NEXT_BUTTON);

        LOGGER.info("Verify bottom navigation");
        TopicChecks.checkPaginationPage(tcc, TOTAL_REPLICATED_TOPICS_COUNT, topicsPerPageList,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON, TopicsPageSelectors.TPS_PAGINATION_DROPDOWN_ITEMS,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_NAV_PREV_BUTTON, TopicsPageSelectors.TPS_BOTTOM_PAGINATION_NAV_NEXT_BUTTON);
    }

    /**
     * Tests the "Recently Viewed Topics" feature on the Kafka overview page.
     *
     * <p>The test opens a subset of created topics to simulate recent activity, then navigates to the overview page to validate that
     * the recently viewed topics are correctly displayed in the UI card.</p>
     * <p>It also verifies that the entries persist even after the topics are deleted, and that clicking on a deleted topic leads to
     * a "Resource not found" message on the messages page.</p>
     * <p>NOTE: This test must be the last executed in the sharedResources order as it deletes topics during testing</p>
     * <p>This ensures proper tracking, rendering, and graceful fallback behavior for recently viewed topics in the UI.</p>
     */
    @Test
    @Order(Integer.MAX_VALUE)
    void testRecentlyViewedTopics() {
        LOGGER.info("Verify topics are present but none has been viewed just yet");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        List<KafkaTopic> topics = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), REPLICATED_TOPICS_PREFIX);
        List<String> topicNames = topics.stream().map(kt -> kt.getMetadata().getName()).sorted().toList().subList(0, 3);
        LOGGER.info("Mark topics as displayed on overview page by visiting their page");
        for (String topicName : topicNames) {
            String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
            tcc.page().navigate(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultNavigateOpts());
            tcc.page().waitForURL(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultWaitForUrlOpts());
            PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_EMPTY_BODY_CONTENT);
            PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_EMPTY_BODY_CONTENT, MessageStore.noDataTitle(), true);
            PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_EMPTY_BODY_CONTENT, MessageStore.noDataBody(), true);
        }

        LOGGER.info("Go to homepage and check the recently viewed card is not empty");
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        LOGGER.info("Check topics table for recently visited topics");
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(1).build(), topicNames.get(2), false);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(2).build(), topicNames.get(1), false);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(3).build(), topicNames.get(0), false);

        LOGGER.info("Delete topics");
        KubeResourceManager.get().deleteResourceWithWait(topics.toArray(new KafkaTopic[0]));

        assertTrue(ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName())).isEmpty());

        LOGGER.info("Check that in recently visited topics table topics are still present");
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(1).build(), topicNames.get(2), false);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(2).build(), topicNames.get(1), false);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(3).build(), topicNames.get(0), false);

        LOGGER.info("Check that one of the recently viewed topics page cannot be found as it's deleted");
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(1).build());
        // Verify empty body message
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_EMPTY_BODY_CONTENT, "Resource not found", true);
    }

    /**
     * Tests display and filtering of topics in various replication states.
     *
     * <p>The test creates four categories of topics: fully replicated, unmanaged replicated, under-replicated, and unavailable.</p>
     * <p>It verifies that the UI displays correct topic counts for each state on the overview and topics pages.</p>
     * <p>Filtering capabilities are tested by filtering topics by name, ID, and replication status to ensure accurate and expected results.</p>
     *
     * <p>This confirms that all topic states are properly represented and filterable in the UI.</p>
     */
    @Test
    @Order(Order.DEFAULT)
    void testFilterTopics() {
        LOGGER.info("Verify Topics are displayed correctly first");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        LOGGER.info("Verify Topics Filtering");
        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();
        final List<String> unmanagedReplicatedTopicsNames = KafkaCmdUtils.listKafkaTopicsByPrefix(tcc.namespace(), tcc.kafkaName(), brokerPodName,
            KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), UNMANAGED_REPLICATED_TOPICS_PREFIX);

        TopicChecks.checkTopicsFilterByName(tcc, unmanagedReplicatedTopicsNames);
        TopicChecks.checkTopicsFilterById(tcc, ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), REPLICATED_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).toList().subList(REPLICATED_TOPICS_COUNT - 5, REPLICATED_TOPICS_COUNT - 1));
        TopicChecks.checkTopicsFilterByStatus(tcc, ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), UNDER_REPLICATED_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).toList(), TopicStatus.UNDER_REPLICATED);
        TopicChecks.checkTopicsFilterByStatus(tcc, ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), UNAVAILABLE_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).toList(), TopicStatus.OFFLINE);
    }

    /**
     * Tests the sorting functionality for Kafka topics based on name and storage usage.
     *
     * <p>The test first filters topics by the "Offline" status and verifies sorting by name in descending order.</p>
     * <p>Then it filters for "Fully Replicated" topics and simulates message production to increase storage for one topic.</p>
     * <p>It verifies that sorting by storage usage places the topic with the highest usage at the top of the list.</p>
     *
     * <p>This ensures that sorting logic is correctly implemented and provides meaningful ordering to users.</p>
     */
    @Test
    @Order(Order.DEFAULT)
    void testSortTopics() {
        LOGGER.info("Verify Topics are displayed correctly first");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();

        List<String> replicatedTopicsNames = new ArrayList<>();
        replicatedTopicsNames.addAll(ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), REPLICATED_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).toList());
        replicatedTopicsNames.addAll(KafkaCmdUtils.listKafkaTopicsByPrefix(tcc.namespace(), tcc.kafkaName(), brokerPodName,
            KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), UNMANAGED_REPLICATED_TOPICS_PREFIX));

        replicatedTopicsNames = replicatedTopicsNames.stream().sorted().toList();

        List<String> unavailableTopicsNames = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), UNAVAILABLE_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).sorted().toList();

        LOGGER.info("Sort topics offline topics by name");
        // Filter
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, TopicStatus.OFFLINE);
        PwUtils.waitForLocatorCount(tcc, UNAVAILABLE_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Sort by name
        TopicsTestUtils.selectSortBy(tcc, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_NAME, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_NAME_BUTTON, "descending");
        PwUtils.waitForLocatorCount(tcc, UNAVAILABLE_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.getTableRowItems(1), unavailableTopicsNames.get(UNAVAILABLE_TOPICS_COUNT - 1), true);

        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);

        LOGGER.info("Sort replicated topics by storage");

        // Filter
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, TopicStatus.FULLY_REPLICATED);
        PwUtils.waitForLocatorCount(tcc, TOTAL_REPLICATED_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Sort by storage
        TopicsTestUtils.selectSortBy(tcc, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_STORAGE, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_STORAGE_BUTTON, "descending");
        PwUtils.waitForLocatorCount(tcc, TOTAL_REPLICATED_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Last managed replicated has utilized more storage
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.getTableRowItems(1), replicatedTopicsNames.get(REPLICATED_TOPICS_COUNT - 1), true);
    }

    // ------
    // Setup
    // ------
    public void prepareTopicsScenario() {
        LOGGER.info("Check default UI state before preparing test topics");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0);

        LOGGER.info("Create all types of topics");
        final int scaledUpBrokerReplicas = Constants.REGULAR_BROKER_REPLICAS + 1;

        List<KafkaTopic> replicatedTopics = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), REPLICATED_TOPICS_PREFIX, REPLICATED_TOPICS_COUNT, true, 1, 1, 1);
        // Produce extra messages for the last fullyReplicated topic - use higher message count number to take more storage
        String topicWithMoreMessages = replicatedTopics.get(REPLICATED_TOPICS_COUNT - 1).getMetadata().getName();
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicWithMoreMessages)
            .withMessageCount(Constants.MESSAGE_COUNT_HIGH)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicWithMoreMessages))
            .withConsumerName(KafkaNamingUtils.consumerName(topicWithMoreMessages))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicWithMoreMessages))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();
        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        KafkaTopicUtils.setupUnmanagedTopicsAndReturnNames(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), UNMANAGED_REPLICATED_TOPICS_PREFIX, UNMANAGED_REPLICATED_TOPICS_COUNT, tcc.messageCount(), 1, 1, 1);
        KafkaTopicUtils.setupUnderReplicatedTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), UNDER_REPLICATED_TOPICS_PREFIX, UNDER_REPLICATED_TOPICS_COUNT, tcc.messageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas);
        KafkaTopicUtils.setupUnavailableTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), UNAVAILABLE_TOPICS_PREFIX, UNAVAILABLE_TOPICS_COUNT, tcc.messageCount(), 1, 1, 1);
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
        
        prepareTopicsScenario();
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
