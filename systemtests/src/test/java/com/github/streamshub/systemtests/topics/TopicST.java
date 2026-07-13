package com.github.streamshub.systemtests.topics;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.enums.FilterType;
import com.github.streamshub.systemtests.enums.TopicStatus;
import com.github.streamshub.systemtests.enums.TopicsPerPage;
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
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaCmdUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import com.github.streamshub.systemtests.utils.testutils.TopicsTestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
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
    
    /**
     * Tests pagination behavior on the Topics page when handling a large set of topics.
     *
     * <p>The test first creates and validates the presence of 150 Kafka topics across both
     * the overview and topics pages.</p>
     *
     * <p>It then exercises the pagination controls in the UI, verifying that both the top
     * and bottom pagination components function correctly. The following checks are included:</p>
     * <ul>
     *   <li>Page size options for 10, 50, and 100 topics per page are available and work as expected.</li>
     *   <li>Navigation buttons (previous/next) perform correct page transitions.</li>
     *   <li>The pagination toggle text displays the correct topic range summary (e.g. "1 - 10 of 150") for the current page.</li>
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
        LOGGER.info("Verify {} topics are displayed on the overview and topics pages", TOTAL_TOPICS_COUNT);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        LOGGER.info("Verify pagination on topics page for {} total topics", TOTAL_TOPICS_COUNT);
        List<TopicsPerPage> topicsPerPageList = List.of(TopicsPerPage.TEN, TopicsPerPage.FIFTY, TopicsPerPage.HUNDRED);

        LOGGER.info("Verify top pagination navigation using page sizes {}", topicsPerPageList);
        TopicChecks.checkPaginationPage(tcc, TOTAL_TOPICS_COUNT, topicsPerPageList,
            TopicsPageSelectors.TPS_TOP_PAGINATION_DROPDOWN_BUTTON, TopicsPageSelectors.TPS_PAGINATION_DROPDOWN_ITEMS,
            TopicsPageSelectors.TPS_TOP_PAGINATION_DROPDOWN_BUTTON_TEXT,
            TopicsPageSelectors.TPS_TOP_PAGINATION_NAV_PREV_BUTTON, TopicsPageSelectors.TPS_TOP_PAGINATION_NAV_NEXT_BUTTON);

        LOGGER.info("Verify bottom pagination navigation using page sizes {}", topicsPerPageList);
        TopicChecks.checkPaginationPage(tcc, TOTAL_TOPICS_COUNT, topicsPerPageList,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON, TopicsPageSelectors.TPS_PAGINATION_DROPDOWN_ITEMS,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_DROPDOWN_BUTTON_TEXT,
            TopicsPageSelectors.TPS_BOTTOM_PAGINATION_NAV_PREV_BUTTON, TopicsPageSelectors.TPS_BOTTOM_PAGINATION_NAV_NEXT_BUTTON);
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
        LOGGER.info("Verify {} topics are displayed correctly before filtering", TOTAL_TOPICS_COUNT);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        LOGGER.info("Verify topics filtering by name, id, and status");
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()));

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).getFirst().getMetadata().getName();
        LOGGER.debug("Using broker pod '{}' to list unmanaged replicated topics with prefix '{}'", brokerPodName, Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX);
        final List<String> unmanagedReplicatedTopicsNames = KafkaCmdUtils.listKafkaTopicsByPrefix(tcc.namespace(), tcc.kafkaName(), brokerPodName,
            KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX);

        LOGGER.info("Verify filtering topics by name using {} unmanaged replicated topic(s)", unmanagedReplicatedTopicsNames.size());
        TopicChecks.checkTopicsFilterByName(tcc, unmanagedReplicatedTopicsNames);
        LOGGER.info("Verify filtering topics by id using replicated topics at indices {}-{}", REPLICATED_TOPICS_COUNT - 5, REPLICATED_TOPICS_COUNT - 1);
        TopicChecks.checkTopicsFilterById(tcc, ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), Constants.REPLICATED_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).toList().subList(REPLICATED_TOPICS_COUNT - 5, REPLICATED_TOPICS_COUNT - 1));
        LOGGER.info("Verify filtering topics by status using {} under-replicated topic(s)", UNDER_REPLICATED_TOPICS_COUNT);
        TopicChecks.checkTopicsFilterByStatus(tcc, ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), Constants.UNDER_REPLICATED_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).toList(), TopicStatus.UNDER_REPLICATED);
        LOGGER.info("Verify filtering topics by status using {} unavailable topic(s)", UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsFilterByStatus(tcc, ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), Constants.UNAVAILABLE_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).toList(), TopicStatus.OFFLINE);
    }

    /**
     * Tests the sorting functionality for Kafka topics by name and storage usage.
     *
     * <p>The test performs the following steps:
     * <ul>
     *   <li>Filters topics by "Offline" status and verifies that sorting by name in descending order works correctly.</li>
     *   <li>Clears the filter and then filters for "Fully Replicated" topics.</li>
     *   <li>Sorts the filtered topics by storage usage in descending order and verifies that the topic which
     *       received extra messages during setup (and therefore has the highest storage usage) appears at the
     *       top of the list.</li>
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
        LOGGER.info("Verify {} topics are displayed correctly before sorting", TOTAL_TOPICS_COUNT);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()));

        List<String> unavailableTopicsNames = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), Constants.UNAVAILABLE_TOPICS_PREFIX).stream().map(kt -> kt.getMetadata().getName()).sorted().toList();

        LOGGER.info("Verify sorting of offline topics by name in descending order");
        // Filter
        LOGGER.info("Filter topics by status {}", TopicStatus.OFFLINE.getName());
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, TopicStatus.OFFLINE);
        PwUtils.waitForLocatorCount(tcc, UNAVAILABLE_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Sort by name
        LOGGER.info("Sort {} offline topic(s) by name in descending order", UNAVAILABLE_TOPICS_COUNT);
        TopicsTestUtils.selectSortBy(tcc, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_NAME, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_NAME_BUTTON, "descending");
        PwUtils.waitForLocatorCount(tcc, UNAVAILABLE_TOPICS_COUNT, TopicsPageSelectors.TPS_TABLE_ROWS, false);
        LOGGER.debug("Expecting topic '{}' to appear first after descending name sort", unavailableTopicsNames.get(UNAVAILABLE_TOPICS_COUNT - 1));
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.getTopicsTableRowItems(1), unavailableTopicsNames.get(UNAVAILABLE_TOPICS_COUNT - 1), true);

        PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_TOP_TOOLBAR_SEARCH_CLEAR_ALL_FILTERS);

        LOGGER.info("Verify sorting of fully replicated topics by storage usage in descending order");

        // Filter
        LOGGER.info("Filter topics by status {}", TopicStatus.FULLY_REPLICATED.getName());
        TopicsTestUtils.selectFilter(tcc, FilterType.STATUS);
        TopicsTestUtils.selectTopicStatus(tcc, TopicStatus.FULLY_REPLICATED);
        PwUtils.waitForLocatorCount(tcc, Math.min(Constants.DEFAULT_TOPICS_PER_PAGE, TOTAL_REPLICATED_TOPICS_COUNT), TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Sort by storage
        LOGGER.info("Sort {} fully replicated topic(s) by storage usage in descending order", Math.min(Constants.DEFAULT_TOPICS_PER_PAGE, TOTAL_REPLICATED_TOPICS_COUNT));
        TopicsTestUtils.selectSortBy(tcc, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_STORAGE, TopicsPageSelectors.TPS_TABLE_HEADER_SORT_BY_STORAGE_BUTTON, "descending");
        PwUtils.waitForLocatorCount(tcc, Math.min(Constants.DEFAULT_TOPICS_PER_PAGE, TOTAL_REPLICATED_TOPICS_COUNT), TopicsPageSelectors.TPS_TABLE_ROWS, false);
        // Last managed replicated has utilized more storage
        final String topicWithLargestStorageUsage = Constants.REPLICATED_TOPICS_PREFIX + "-" + (REPLICATED_TOPICS_COUNT - 1);
        LOGGER.debug("Expecting topic '{}' with the highest storage usage to appear first", topicWithLargestStorageUsage);
        PwUtils.waitForContainsText(tcc, TopicsPageSelectors.getTopicsTableRowItem(1, 1), topicWithLargestStorageUsage, true);
    }

    /**
     * Tests the "Recently Viewed Topics" functionality on the Kafka Overview page.
     *
     * <p>The test simulates user activity by opening a subset of created topics, ensuring
     * they appear in the "Recently Viewed Topics" card on the overview page. It performs
     * the following checks:</p>
     * <ul>
     *   <li>Verifies the topic counts on the overview and topics pages before any topic has been visited.</li>
     *   <li>Visits several topic message pages, triggering them to appear in the "Recently Viewed Topics" card.</li>
     *   <li>Validates that the card correctly displays the visited topics in reverse chronological order.</li>
     *   <li>Deletes the underlying Kafka topics, including the visited ones, and ensures that the visited
     *       topics' entries persist in the card.</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This test must run last in the {@code VariousTopicTypes} test bucket order
     * because it deletes topics as part of the scenario.</p>
     *
     * <p>This ensures proper tracking and persistence of the "Recently Viewed Topics" feature in the UI.</p>
     */
    @Test
    @Order(Integer.MAX_VALUE)
    @TestBucket(VARIOUS_TOPIC_TYPES_BUCKET)
    void testRecentlyViewedTopics() {
        LOGGER.info("Verify {} topics are present but none has been viewed just yet", TOTAL_TOPICS_COUNT);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        List<KafkaTopic> topics = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), Constants.REPLICATED_TOPICS_PREFIX);
        List<String> topicNames = topics.stream().map(kt -> kt.getMetadata().getName()).sorted().toList().subList(0, 3);
        LOGGER.info("Mark {} topics as displayed on overview page by visiting their page: {}", topicNames.size(), topicNames);
        for (String topicName : topicNames) {
            String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
            LOGGER.info("Visiting topic '{}' (id={}) messages page to mark it as recently viewed", topicName, topicId);
            PwUtils.navigate(tcc, PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId), true, true);
            PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_EMPTY_BODY_CONTENT);
            PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_EMPTY_BODY_CONTENT, "No messages data", false);
        }

        LOGGER.info("Navigate to overview page to check the recently viewed card is not empty");
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        LOGGER.info("Verify recently viewed topics card lists visited topics in reverse chronological order: {}", topicNames.reversed());
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.getTableRowItemLink(1), topicNames.get(2), false);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.getTableRowItemLink(2), topicNames.get(1), false);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.getTableRowItemLink(3), topicNames.get(0), false);

        LOGGER.info("Delete {} topic(s), including the {} previously visited topic(s)", topics.size(), topicNames.size());
        KubeResourceManager.get().deleteResourceWithWait(topics.toArray(new KafkaTopic[0]));

        assertTrue(ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), KafkaNamingUtils.topicPrefixName(tcc.kafkaName())).isEmpty());

        LOGGER.info("Verify recently viewed topics card still lists deleted topics: {}", topicNames.reversed());
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(1).build(), topicNames.get(2), false);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(2).build(), topicNames.get(1), false);
        PwUtils.waitForContainsText(tcc, new CssBuilder(ClusterOverviewPageSelectors.COPS_RECENT_TOPICS_CARD_TABLE_ITEMS).nth(3).build(), topicNames.get(0), false);
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
     *   <li>Creates under-replicated topics by scaling brokers up so replicas can spread across them, then
     *       scaling brokers back down so that a broker holding a replica is removed.</li>
     *   <li>Creates unavailable topics to simulate offline partitions.</li>
     * </ul>
     *
     * <p>This scenario ensures that the test environment contains topics in all relevant replication states,
     * which is essential for verifying UI topic displays, filtering, sorting, and storage-based behaviors.</p>
     */
    @SetupTestBucket(VARIOUS_TOPIC_TYPES_BUCKET)
    public void prepareVariousTopicTypes() {
        LOGGER.info("Check default UI state before preparing test topics: expecting 0 topics on overview and topics pages");
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0, 0);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), 0, 0, 0, 0);

        LOGGER.info("Create all required topic types: {} replicated, {} unmanaged replicated, {} under-replicated, {} unavailable",
            TOTAL_REPLICATED_TOPICS_COUNT, UNMANAGED_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        final int scaledUpBrokerReplicas = Constants.REGULAR_BROKER_REPLICAS + 1;

        LOGGER.info("Create {} fully replicated topic(s) with prefix '{}'", REPLICATED_TOPICS_COUNT, Constants.REPLICATED_TOPICS_PREFIX);
        List<KafkaTopic> replicatedTopics = KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, REPLICATED_TOPICS_COUNT, 1, 1, 1);
        // Produce extra messages for the last fullyReplicated topic - use higher message count number to take more storage
        String topicWithMoreMessages = replicatedTopics.get(REPLICATED_TOPICS_COUNT - 1).getMetadata().getName();
        LOGGER.info("Produce {} extra message(s) to topic '{}' to increase its storage usage", Constants.MESSAGE_COUNT_HIGH, topicWithMoreMessages);
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
        LOGGER.debug("Waiting for producer/consumer clients of topic '{}' to finish successfully", topicWithMoreMessages);
        WaitUtils.waitForClientsSuccess(clients);

        LOGGER.info("Create {} unmanaged replicated, {} under-replicated (broker replicas scaled {} -> {}), and {} unavailable topic(s)",
            UNMANAGED_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, Constants.REGULAR_BROKER_REPLICAS, scaledUpBrokerReplicas, UNAVAILABLE_TOPICS_COUNT);
        KafkaTopicUtils.setupUnmanagedUnderReplicatedAndUnavailableTopics(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()),
            new KafkaTopicUtils.TopicTypeSpec(Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX, UNMANAGED_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1),
            new KafkaTopicUtils.TopicTypeSpec(Constants.UNDER_REPLICATED_TOPICS_PREFIX, UNDER_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas),
            new KafkaTopicUtils.TopicTypeSpec(Constants.UNAVAILABLE_TOPICS_PREFIX, UNAVAILABLE_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1));
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
