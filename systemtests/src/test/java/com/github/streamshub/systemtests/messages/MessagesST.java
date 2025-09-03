package com.github.streamshub.systemtests.messages;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.MessagesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;

@Tag(TestTags.REGRESSION)
public class MessagesST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(MessagesST.class);
    private static final String VARIOUS_MESSAGE_TYPES_BUCKET = "VariousMessageTypes";
    private TestCaseConfig tcc;

    private static final String TOPIC_PREFIX = "filter-messages";
    // If message count is changed, verify message values inside tests
    private static final int MESSAGE_COUNT = Constants.MESSAGE_COUNT;
    private static final int TOPIC_COUNT = 1;
    private static String kafkaTopicName;
    // 1. Filter by key
    private static final String KEY_FILTER = "orderID";
    private static final String KEY_FILTER_MESSAGE = "my-order";
    // 2. Filter by message header
    private static final String HEADER_FILTER = "traceID=abc123";
    private static final String HEADER_FILTER_LOOK_UP_TEXT = "traceID";
    private static final String HEADER_FILTER_MESSAGE = "abc123";
    // 3. Filter by message value
    private static final String VALUE_FILTER = "package=sent";

    /**
     * Provides parameterized scenarios for verifying message search functionality
     * on the Messages page using different query strings.
     *
     * <p>Each scenario defines:</p>
     * <ul>
     *   <li>The expected number of results returned.</li>
     *   <li>The query string used (e.g., latest, offset, invalid values, etc.).</li>
     *   <li>The expected content checks to validate in the result table.</li>
     * </ul>
     *
     * <p>Scenarios include:</p>
     * <ul>
     *   <li>Retrieving the latest messages.</li>
     *   <li>Retrieving messages from a specific offset.</li>
     *   <li>Filtering by message content.</li>
     *   <li>Handling no-match queries.</li>
     *   <li>Validating fallback behavior for invalid query parameters.</li>
     * </ul>
     */
    public Stream<Arguments> searchUsingQueryScenarios() {
        return Stream.of(
            Arguments.of(50, "", Map.of(
                MessagesPageSelectors.getTableRowItems(1), VALUE_FILTER + " - 99",
                MessagesPageSelectors.getTableRowItem(1, 1), "299")),
            Arguments.of(2, "messages=latest retrieve=2", Map.of(
                MessagesPageSelectors.getTableRowItems(1), VALUE_FILTER + " - 99",
                MessagesPageSelectors.getTableRowItems(2), VALUE_FILTER + " - 98")),
            Arguments.of(20, "messages=offset:150 retrieve=20", Map.of(
                MessagesPageSelectors.getTableRowItem(1, 5), HEADER_FILTER_MESSAGE + " - 50",
                MessagesPageSelectors.getTableRowItem(1, 1), "150")),
            Arguments.of(1, "messages=offset:10 retrieve=100 " + KEY_FILTER_MESSAGE + " - 42", Map.of(
                MessagesPageSelectors.getTableRowItem(1, 5), KEY_FILTER_MESSAGE + " - 42",
                MessagesPageSelectors.getTableRowItem(1, 1), "42")),
            Arguments.of(1, "messages=latest retrieve=40 " + KEY_FILTER_MESSAGE + " - 42", Map.of(
                MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, "No messages data")),
            Arguments.of(50, "messages=totalyNotOkay retrieve=-9", Map.of(
                MessagesPageSelectors.getTableRowItems(1), VALUE_FILTER + " - 99",
                MessagesPageSelectors.getTableRowItems(2), VALUE_FILTER + " - 98"))
        );
    }

    /**
     * Executes parameterized search scenarios on the Messages page.
     *
     * <p>For each query, the test:</p>
     * <ul>
     *   <li>Navigates to the topic's Messages page.</li>
     *   <li>Applies the provided search query via the search toolbar.</li>
     *   <li>Validates the number of results displayed matches the expected count.</li>
     *   <li>Performs additional content checks to confirm message keys, values, headers,
     *       and ordering are correctly filtered or displayed.</li>
     * </ul>
     *
     * <p>This ensures that the Messages page search input correctly supports latest retrieval,
     * offset-based queries, message content filtering, and gracefully handles invalid inputs.</p>
     *
     * @param expectedResults number of results expected to be displayed in the table
     * @param searchQuery the search query string to apply
     * @param checks map of selectors to expected values used for content validation
     */
    @ParameterizedTest(name = "Query: {1}")
    @MethodSource("searchUsingQueryScenarios")
    @TestBucket(VARIOUS_MESSAGE_TYPES_BUCKET)
    void testMessageSearchUsingQueries(int expectedResults, String searchQuery, Map<String, String> checks) {
        final String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), kafkaTopicName);
        tcc.page().navigate(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId));

        LOGGER.info("Wait for message search page toolbar to be fully there before filtering messages");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, kafkaTopicName, true);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);

        LOGGER.info("Search query [{}] expecting results count of {}", searchQuery, expectedResults);
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, searchQuery);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForLocatorCount(tcc, expectedResults, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);

        LOGGER.info("Run checks on results");
        checks.forEach((selector, expectedValue) -> PwUtils.waitForContainsText(tcc, selector, expectedValue, true));
    }

    /**
     * Verifies message filtering functionality using the form-based UI filter panel on the Messages page.
     *
     * <p>The test covers filtering by message key, header, and value, ensuring that only the
     * expected subset of messages is displayed after each filter is applied. It also validates
     * filter reset behavior between steps.</p>
     *
     * <p>The test performs the following steps:</p>
     * <ul>
     *   <li>Create a Kafka topic and send three distinct sets of messages with different keys, headers, and values.</li>
     *   <li>Verify the default state of the Messages page (latest messages displayed, correct attributes in query input).</li>
     *   <li>Apply a <b>key-based filter</b> without an offset and confirm "No messages data" is shown.</li>
     *   <li>Set an offset, reapply the key filter, and verify only messages with the matching key are displayed.</li>
     *   <li>Reset filters and confirm the table shows the default latest messages again.</li>
     *   <li>Apply a <b>header-based filter</b> with offset and confirm only matching messages appear in ascending order.</li>
     *   <li>Reset filters and re-verify default state.</li>
     *   <li>Apply a <b>value-based filter</b> with offset and confirm the correct subset of messages is shown.</li>
     * </ul>
     *
     * <p>Throughout the test, assertions are made on message offsets, keys, headers, values,
     * and table counts to ensure filtering logic and UI behavior work as expected.</p>
     */
    @Test
    @TestBucket(VARIOUS_MESSAGE_TYPES_BUCKET)
    void testFilterMessagesUsingUIForm() {
        final String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), kafkaTopicName);
        tcc.page().navigate(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId));

        LOGGER.info("Wait for page toolbar to be fully loaded before filtering");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, kafkaTopicName, true);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);

        LOGGER.info("Verify default state of displayed messages");
        PwUtils.waitForLocatorCount(tcc, 50, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(1), VALUE_FILTER + " - 99", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "299", true);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest retrieve=50", Constants.VALUE_ATTRIBUTE, true);

        LOGGER.info("Filter messages by key - because no offset is specified, first `No message data` should appear");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_WHERE_DROPDOWN_ITEMS).nth(2).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, KEY_FILTER);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_EMPTY_FILTER_SEARCH_CONTENT, "No messages data", true);

        LOGGER.info("Set correct offset to display messages");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        // Take last messages of the first set and let it overlap with second set to see if it filters them out
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "95");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:95 retrieve=50 orderID where=key", Constants.VALUE_ATTRIBUTE, true);
        // Order is ASC
        LOGGER.debug("Verify filtered messages with specific key");
        PwUtils.waitForLocatorCount(tcc, 5, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "95", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 3), KEY_FILTER, true);

        LOGGER.debug("Reset messages filter");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_RESET_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest retrieve=50", Constants.VALUE_ATTRIBUTE, true);
        // Order is DESC
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "299", true);

        LOGGER.info("Filter messages by Headers");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_WHERE_DROPDOWN_ITEMS).nth(3).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, HEADER_FILTER_LOOK_UP_TEXT);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "95");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:95 retrieve=50 " + HEADER_FILTER_LOOK_UP_TEXT + " where=headers", Constants.VALUE_ATTRIBUTE, true);
        // Because filter retrieve overlaps 5 messages from previous set, there should be only 45 with correct header
        // Order is ASC
        LOGGER.debug("Verify filtered messages with specific header");
        PwUtils.waitForLocatorCount(tcc, 45, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "100", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 4), HEADER_FILTER_LOOK_UP_TEXT, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), HEADER_FILTER_MESSAGE + " - 0", true);

        LOGGER.debug("Reset messages filter");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_RESET_BUTTON);
        // Order is DESC
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest retrieve=50", Constants.VALUE_ATTRIBUTE, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "299", true);

        LOGGER.info("Filter messages by setting MessageValue");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);

        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_WHERE_DROPDOWN_ITEMS).nth(4).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, VALUE_FILTER);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "195");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:195 retrieve=50 " + VALUE_FILTER + " where=value", Constants.VALUE_ATTRIBUTE, true);
        // Because filter retrieve overlaps 5 messages from previous set, there should be only 45 with correct message value
        // Order is ASC
        LOGGER.debug("Verify filtered messages with specific message value");
        PwUtils.waitForLocatorCount(tcc, 45, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "200", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), VALUE_FILTER + " - 0", true);
    }

    /**
     * Prepares a Kafka topic and produces a diverse set of messages for message filtering tests.
     *
     * <p>This scenario sets up a single-topic environment with multiple batches of messages
     * containing variations in keys, headers, and values to support different filter queries.</p>
     *
     * <p>The preparation includes:</p>
     * <ul>
     *   <li>Creating a Kafka topic for testing.</li>
     *   <li>Producing the first batch of messages with a specific key and message content.</li>
     *   <li>Producing a second batch of messages with distinct headers and message values.</li>
     *   <li>Producing a third batch of messages with alternative values and headers.</li>
     * </ul>
     *
     * <p>Each producer/consumer pair is executed and verified for success before continuing,
     * ensuring that messages are reliably available for filtering tests.</p>
     *
     * <p>At the end, the topic contains a mixture of messages with variations in keys, headers,
     * and payloads, allowing subsequent tests to validate filtering by key, header, or value.</p>
     */
    @SetupTestBucket(VARIOUS_MESSAGE_TYPES_BUCKET)
    public void prepareVariousMessageTypes() {
        LOGGER.info("Prepare filter messages scenario by creating topic and producing various messages");

        kafkaTopicName = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), TOPIC_PREFIX, TOPIC_COUNT, true, 1, 1, 1)
            .get(0).getMetadata().getName();

        // Setup UI form filtering
        // First set clients to send messages with KEY
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(kafkaTopicName)
            .withMessageCount(MESSAGE_COUNT)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(kafkaTopicName))
            .withConsumerName(KafkaNamingUtils.consumerName(kafkaTopicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(kafkaTopicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage(KEY_FILTER_MESSAGE)
            .withMessageKey(KEY_FILTER)
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        // create second set of messages with different HEADER and MESSAGE
        clients.setMessageKey("NoDataInKey-True");
        clients.setHeaders(HEADER_FILTER);
        clients.setMessage(HEADER_FILTER_MESSAGE);

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);
        // create third set of messages with different MESSAGE
        clients.setHeaders("NoDataInHeader=true");
        clients.setMessage(VALUE_FILTER);

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        LOGGER.info("Filtering scenario prepared");
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
