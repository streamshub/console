package com.github.streamshub.systemtests.topics;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.MessageStore;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
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

import java.util.List;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.REGRESSION)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TopicST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(TopicST.class);
    private static final String VARIOUS_TOPIC_TYPES_BUCKET = "VariousTopicTypes";
    private TestCaseConfig tcc;

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
     * Tests pagination behavior on the Topics page when handling a large set of topics.
     *
     * <p>The test first creates and validates the presence of 150 Kafka topics across both
     * the overview and topics pages.</p>
     *
     * <p>It then exercises the pagination controls in the UI, verifying that both the top
     * and bottom pagination components function correctly. The following checks are included:</p>
     * <ul>
     *   <li>Page size options for 10, 20, 50, and 100 topics per page are available and work as expected.</li>
     *   <li>Navigation buttons (previous/next) perform correct page transitions.</li>
     *   <li>Dropdown menus display the correct selected page size.</li>
     *   <li>Topics displayed on each page match the expected counts.</li>
     * </ul>
     *
     * <p>This ensures that pagination remains reliable and user-friendly even when
     * scaling to large numbers of topics.</p>
     */
    @Test
    @Order(Order.DEFAULT)
    @TestBucket(VARIOUS_TOPIC_TYPES_BUCKET)
    void testPaginationWithManyTopics() {
        LOGGER.info("Verify topics are displayed");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        LOGGER.info("Verify pagination on topics page");
        List<Integer> topicsPerPageList = List.of(10, 20, 50, 100);

        LOGGER.info("Verify top navigation");
        TopicChecks.checkPaginationPage(tcc, TOTAL_TOPICS_COUNT, topicsPerPageList,
            TopicsPageSelectors.TPS_TOP_PAGINATION_DROPDOWN_BUTTON, TopicsPageSelectors.TPS_PAGINATION_DROPDOWN_ITEMS,
            TopicsPageSelectors.TPS_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT,
            TopicsPageSelectors.TPS_TOP_PAGINATION_NAV_PREV_BUTTON, TopicsPageSelectors.TPS_TOP_PAGINATION_NAV_NEXT_BUTTON);

        LOGGER.info("Verify bottom navigation");
        TopicChecks.checkPaginationPage(tcc, TOTAL_TOPICS_COUNT, topicsPerPageList,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON, TopicsPageSelectors.TPS_PAGINATION_DROPDOWN_ITEMS,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_NAV_PREV_BUTTON, TopicsPageSelectors.TPS_BOTTOM_PAGINATION_NAV_NEXT_BUTTON);
    }

    /**
     * Tests the "Recently Viewed Topics" functionality on the Kafka Overview page.
     *
     * <p>The test simulates user activity by opening a subset of created topics, ensuring
     * they appear in the "Recently Viewed Topics" card on the overview page. It performs
     * the following checks:</p>
     * <ul>
     *   <li>Initially verifies that no topics are marked as recently viewed.</li>
     *   <li>Visits several topic message pages, triggering them to appear in the "Recently Viewed Topics" card.</li>
     *   <li>Validates that the card correctly displays the visited topics in reverse chronological order.</li>
     *   <li>Deletes the visited topics and ensures that their entries persist in the card.</li>
     *   <li>Confirms that clicking a deleted topic leads to a "Resource not found" message.</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This test must run last in the {@code sharedResources} order
     * because it deletes topics as part of the scenario.</p>
     *
     * <p>This ensures proper tracking, persistence, and graceful fallback behavior of the
     * "Recently Viewed Topics" feature in the UI.</p>
     */
    @Test
    @Order(Integer.MAX_VALUE)
    @TestBucket(VARIOUS_TOPIC_TYPES_BUCKET)
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
     * Tests the display and filtering of Kafka topics in different replication states.
     *
     * <p>The test creates and categorizes topics into four replication states:
     * <ul>
     *   <li>Fully replicated</li>
     *   <li>Unmanaged replicated</li>
     *   <li>Under-replicated</li>
     *   <li>Unavailable</li>
     * </ul>
     * </p>
     *
     * <p>It verifies that the UI correctly displays the topic counts for each category
     * on both the overview and topics pages. The test also validates filtering behavior by:</p>
     * <ul>
     *   <li>Filtering topics by <strong>name</strong> (matching unmanaged replicated topics).</li>
     *   <li>Filtering topics by <strong>ID</strong> (subset of replicated topics).</li>
     *   <li>Filtering topics by <strong>replication status</strong> (under-replicated and unavailable topics).</li>
     * </ul>
     *
     * <p>This ensures that the UI properly represents all topic states and that filtering
     * works reliably across different criteria.</p>
     */
    @Test
    @Order(Order.DEFAULT)
    @TestBucket(VARIOUS_TOPIC_TYPES_BUCKET)
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
     * Tests the sorting functionality for Kafka topics by name and storage usage.
     *
     * <p>The test performs the following steps:
     * <ul>
     *   <li>Filters topics by "Offline" status and verifies that sorting by name in descending order works correctly.</li>
     *   <li>Clears the filter and then filters for "Fully Replicated" topics.</li>
     *   <li>Simulates message production to increase storage for a topic and verifies that sorting by storage usage
     *       places the topic with the highest storage at the top of the list.</li>
     * </ul>
     *
     * <p>Checks include topic counts, correct ordering of topics in the table, and that sorting buttons
     * in the UI respond appropriately to user interactions.</p>
     *
     * <p>This ensures that topic sorting by different criteria is correctly implemented and provides meaningful
     * ordering for end users in the UI.</p>
     */
    @Test
    @Order(Order.DEFAULT)
    @TestBucket(VARIOUS_TOPIC_TYPES_BUCKET)
    void testSortTopics() {
        LOGGER.info("Verify Topics are displayed correctly first");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        tcc.page().navigate(PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()), PwUtils.getDefaultNavigateOpts());

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
        PwUtils.waitForLocatorCount(tcc, Math.min(Constants.DEFAULT_TOPICS_PER_PAGE, TOTAL_REPLICATED_TOPICS_COUNT), TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Sort by storage
        TopicsTestUtils.selectSortBy(tcc, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_STORAGE, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_STORAGE_BUTTON, "descending");
        PwUtils.waitForLocatorCount(tcc, Math.min(Constants.DEFAULT_TOPICS_PER_PAGE, TOTAL_REPLICATED_TOPICS_COUNT), TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Last managed replicated has utilized more storage
        final String topicWithLargestStorageUsage = REPLICATED_TOPICS_PREFIX + "-" + (REPLICATED_TOPICS_COUNT - 1);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.getTableRowItem(1, 1), topicWithLargestStorageUsage, true);
    }

    // ------
    // Setup
    // ------
    /**
     * Prepares a comprehensive topic scenario for testing topic states and UI behaviors.
     *
     * <p>The method performs the following steps:
     * <ul>
     *   <li>Verifies that the overview and topics pages initially show zero topics.</li>
     *   <li>Creates a set of fully replicated topics and produces extra messages for the last topic to simulate higher storage usage.</li>
     *   <li>Creates a set of unmanaged replicated topics.</li>
     *   <li>Creates under-replicated topics by scaling up broker replicas.</li>
     *   <li>Creates unavailable topics to simulate offline partitions.</li>
     * </ul>
     *
     * <p>This scenario ensures that the test environment contains topics in all relevant replication states,
     * which is essential for verifying UI topic displays, filtering, sorting, and storage-based behaviors.</p>
     */
    @SetupTestBucket(VARIOUS_TOPIC_TYPES_BUCKET)
    public void prepareVariousTopicTypes() {
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
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
