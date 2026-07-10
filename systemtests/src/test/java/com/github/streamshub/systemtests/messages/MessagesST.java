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
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.testchecks.MessagesChecks;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private static final String TIMESTAMP_FILTER = "messages=timestamp:";
    private static final String EPOCH_FILTER = "messages=epoch:";

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
            Arguments.of(0, "messages=latest retrieve=40 " + KEY_FILTER_MESSAGE + " - 42", Map.of(
                MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_NO_DATA, "No messages data")),
            Arguments.of(50, "messages=totalyNotOkay retrieve=-9", Map.of(
                MessagesPageSelectors.getTableRowItems(1), VALUE_FILTER + " - 99",
                MessagesPageSelectors.getTableRowItems(2), VALUE_FILTER + " - 98"))
        );
    }

    /**
     * Verifies the Messages page search toolbar handles a variety of query strings correctly.
     *
     * <p>The test runs against the {@code VariousMessageTypes} bucket's shared topic
     * ({@code kafkaTopicName}), which holds 300 messages (offsets 0-299) produced in three
     * batches of {@code MESSAGE_COUNT} (100) messages each: offsets 0-99 carry the
     * {@code orderID} key with message {@code my-order - <n>}, offsets 100-199 carry the
     * {@code traceID=abc123} header with message {@code abc123 - <n>}, and offsets 200-299
     * contain {@code package=sent - <n>} as the message value.</p>
     *
     * <p>For each of the six {@link #searchUsingQueryScenarios()} scenarios, the test navigates
     * to the topic's Messages page, applies the given search query via the search toolbar, and:</p>
     * <ul>
     *   <li>Validates the number of rows displayed matches the expected count.</li>
     *   <li>Checks specific table cells (offset, key, header, or value columns) contain the
     *       expected text.</li>
     * </ul>
     *
     * <p>Scenarios covered:</p>
     * <ul>
     *   <li>An empty query, which falls back to the latest 50 messages (row 1 = offset 299,
     *       value {@code package=sent - 99}).</li>
     *   <li>{@code messages=latest retrieve=2}, returning only the 2 newest messages
     *       (offsets 299 and 298).</li>
     *   <li>{@code messages=offset:150 retrieve=20}, returning 20 messages starting at offset 150
     *       (value {@code abc123 - 50}).</li>
     *   <li>{@code messages=offset:10 retrieve=100 my-order - 42}, narrowing the result down to the
     *       single message at offset 42 whose value contains {@code my-order - 42}.</li>
     *   <li>{@code messages=latest retrieve=40 my-order - 42}, which finds no match because offset 42
     *       falls outside the latest 40 messages, resulting in "No messages data".</li>
     *   <li>An invalid query ({@code messages=totalyNotOkay retrieve=-9}), which falls back to the
     *       default latest-50 behavior.</li>
     * </ul>
     *
     * <p>This confirms that the Messages page search input correctly supports latest retrieval,
     * offset-based queries, message content filtering, and gracefully falls back on invalid input.</p>
     *
     * @param expectedResults number of results expected to be displayed in the table
     * @param searchQuery the search query string to apply
     * @param checks map of selectors to expected values used for content validation
     */
    @ParameterizedTest(name = "Query: {1}")
    @MethodSource("searchUsingQueryScenarios")
    @TestBucket(VARIOUS_MESSAGE_TYPES_BUCKET)
    void testMessageSearchUsingQueries(int expectedResults, String searchQuery, Map<String, String> checks) {
        LOGGER.info("Running message search scenario on topic '{}' with query [{}], expecting {} result row(s)", kafkaTopicName, searchQuery, expectedResults);
        final String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), kafkaTopicName);
        PwUtils.navigate(tcc, PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId));

        LOGGER.debug("Waiting for message search page toolbar to be fully loaded before filtering messages");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, kafkaTopicName, true);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);

        LOGGER.info("Filling search query [{}] and awaiting {} result row(s)", searchQuery, expectedResults);
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, searchQuery);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForLocatorCount(tcc, expectedResults, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);

        LOGGER.info("Validating {} result check(s) for query [{}]", checks.size(), searchQuery);
        checks.forEach((selector, expectedValue) -> {
            LOGGER.debug("Checking selector [{}] contains expected value [{}]", selector, expectedValue);
            PwUtils.waitForContainsText(tcc, selector, expectedValue, true);
            assertTrue(tcc.page().locator(selector).allInnerTexts().toString().contains(expectedValue));
        });
    }

    /**
     * Verifies message filtering functionality based on timestamps
     * in the Messages view.
     *
     * <p>The test validates both query-based and UI-based timestamp filtering
     * using ISO datetime and Unix epoch formats.</p>
     *
     * <p>The following scenarios are covered:</p>
     * <ul>
     *   <li>Produces messages with a known timestamp and verifies
     *      *       they appear when filtering the earlier time.</li>
     *   <li>Filters messages using an ISO-8601 timestamp query and verifies
     *       that only messages produced after the specified time are returned.</li>
     *   <li>Adjusts the timestamp to an earlier value and confirms that older
     *       messages become visible.</li>
     *   <li>Produces additional messages with a known timestamp and verifies
     *       they appear when filtering from the current time.</li>
     *   <li>Validates filtering using Unix epoch timestamps in the query bar.</li>
     *   <li>Verifies timestamp filtering through the UI popover form using:
     *       <ul>
     *           <li>Date + time selection (ISO-based filtering)</li>
     *           <li>Unix timestamp input</li>
     *       </ul>
     *   </li>
     * </ul>
     * <p><strong>Note:</strong> This test must run last in the {@code sharedResources} order
     *      * because it creates new messages as part of thetesting scenario.</p>
     * <p>The test ensures that timestamp-based filtering behaves consistently
     * across query input and UI form interactions, confirming correct backend
     * filtering logic and frontend rendering.</p>
     */
    @Test
    @Disabled
    // TODO: enable once the timestamp bug is fixed
    void testMessageFilteringByTimestamps() {
        final int oldMessageCount = 50;
        final int newMessageCount = 5;
        final String newMessageText = "testTimestampText";
        final String oldMessageText = "earlierMessages";
        final String topicPrefix = "timestamp-filter-topic";

        // Formatters
        final DateTimeFormatter timestampFormatterQuery = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        final DateTimeFormatter dateFormatterForm = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final DateTimeFormatter timeFormatterForm = DateTimeFormatter.ofPattern("HH:mm");

        String testTopic = KafkaTopicUtils
                .setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), topicPrefix, TOPIC_COUNT, 1, 1, 1)
                .getFirst()
                .getMetadata()
                .getName();

        // Set timestamps
        final OffsetDateTime earlierUtcTime = Instant.now().atOffset(ZoneOffset.UTC);
        final String earlierTimeQuery = earlierUtcTime.format(timestampFormatterQuery);
        final String earlierDateTimeUnix = String.valueOf(earlierUtcTime.toEpochSecond());
        final String earlierDateForm = earlierUtcTime.format(dateFormatterForm);
        final String earlierTimeForm = earlierUtcTime.atZoneSameInstant(ZoneId.systemDefault()).format(timeFormatterForm);

        LOGGER.info("Earlier ISO: {}, Earlier Unix: {}", earlierTimeQuery, earlierDateTimeUnix);

        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(testTopic)
            .withMessageCount(oldMessageCount)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(testTopic))
            .withConsumerName(KafkaNamingUtils.consumerName(testTopic))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(testTopic))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage(oldMessageText)
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        final String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), testTopic);
        LOGGER.info("Using topic '{}' with id '{}'", testTopic, topicId);

        PwUtils.navigate(tcc, PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId));
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, testTopic, true);

        LOGGER.info("Filtering messages using ISO query timestamp (current) - expect 0 new messages");
        MessagesChecks.checkQueryBarFilter(tcc, TIMESTAMP_FILTER, Instant.now().atOffset(ZoneOffset.UTC).format(timestampFormatterQuery), 1, null);

        LOGGER.info("Filtering messages using ISO query timestamp (earlier) - expect full page");
        MessagesChecks.checkQueryBarFilter(tcc, TIMESTAMP_FILTER, earlierTimeQuery, oldMessageCount, oldMessageText);

        final OffsetDateTime currentUtcTime = Instant.now().atOffset(ZoneOffset.UTC);
        final String currentDateTimeQuery = currentUtcTime.format(timestampFormatterQuery);
        final String currentDateTimeUnix = String.valueOf(currentUtcTime.toEpochSecond());
        final String currentDateForm = currentUtcTime.format(dateFormatterForm);
        final String currentTimeForm = currentUtcTime.atZoneSameInstant(ZoneId.systemDefault()).format(timeFormatterForm);

        LOGGER.info("Current ISO: {}, Current Unix: {}", currentDateTimeQuery, currentDateTimeUnix);
        LOGGER.info("Producing {} new messages with text '{}'", newMessageCount, newMessageText);
        clients = new KafkaClientsBuilder(clients)
            .withMessageCount(newMessageCount)
            .withMessage(newMessageText)
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        LOGGER.info("New messages successfully produced and consumed");

        // Verify new messages via query bar (ISO + Unix)
        PwUtils.navigate(tcc, PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId));

        LOGGER.info("Verifying ISO filtering returns newly produced messages");
        MessagesChecks.checkQueryBarFilter(tcc, TIMESTAMP_FILTER, currentDateTimeQuery, newMessageCount, newMessageText);

        LOGGER.info("Filtering messages using Unix query timestamp (current)");
        MessagesChecks.checkQueryBarFilter(tcc, EPOCH_FILTER, currentDateTimeUnix, newMessageCount, newMessageText);

        LOGGER.info("Filtering messages using Unix query timestamp (earlier)");
        MessagesChecks.checkQueryBarFilter(tcc, EPOCH_FILTER, earlierDateTimeUnix, oldMessageCount, oldMessageText);

        // Verify via UI popover (ISO mode)
        PwUtils.navigate(tcc, PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId));
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, testTopic, true);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);

        LOGGER.info("Verifying timestamp filtering using UI popover (ISO mode) (current)");
        MessagesChecks.checkPopoverIsoFilter(tcc, currentDateForm, currentTimeForm, newMessageCount, newMessageText);

        LOGGER.info("Verifying timestamp filtering using UI popover (ISO mode) (earlier)");
        MessagesChecks.checkPopoverIsoFilter(tcc, earlierDateForm, earlierTimeForm, oldMessageCount, oldMessageText);

        // Verify via UI popover (Unix mode)
        LOGGER.info("Verifying timestamp filtering using UI popover (Unix mode) (current)");
        MessagesChecks.checkPopoverUnixFilter(tcc, currentDateTimeUnix, newMessageCount, newMessageText);

        LOGGER.info("Verifying timestamp filtering using UI popover (Unix mode) (earlier)");
        MessagesChecks.checkPopoverUnixFilter(tcc, earlierDateTimeUnix, oldMessageCount, oldMessageText);
    }

    /**
     * Verifies message filtering through the popover-based filter form on the Messages page.
     *
     * <p>The test runs against the {@code VariousMessageTypes} bucket's shared topic
     * ({@code kafkaTopicName}), which holds 300 messages (offsets 0-299) produced in three
     * consecutive batches of 100: offsets 0-99 carry the {@code orderID} key, offsets 100-199
     * carry the {@code traceID=abc123} header, and offsets 200-299 contain {@code package=sent}
     * in their value.</p>
     *
     * <p>The test performs the following steps:</p>
     * <ul>
     *   <li>Verifies the default state: the latest 50 messages are shown (row 1 = offset 299,
     *       value {@code package=sent - 99}) and the query input reflects
     *       {@code messages=latest retrieve=50}.</li>
     *   <li>Opens the filter form, selects the <b>Key</b> filter with value {@code orderID} but no
     *       offset, and confirms "No messages data" is shown because the key only exists outside
     *       the latest window.</li>
     *   <li>Sets the offset to {@code 95} and reapplies the key filter, confirming the query becomes
     *       {@code messages=offset:95 retrieve=50 orderID where=key} and exactly 5 matching messages
     *       (offsets 95-99, ascending) are returned.</li>
     *   <li>Resets the filters and confirms the table returns to the default latest-messages view.</li>
     *   <li>Selects the <b>Headers</b> filter with lookup text {@code traceID} and offset {@code 95},
     *       confirming the query becomes {@code messages=offset:95 retrieve=50 traceID where=headers}
     *       and that only the 45 messages carrying the header (offsets 100-144) are shown, ascending,
     *       starting with value {@code abc123 - 0}.</li>
     *   <li>Resets the filters again and re-verifies the default latest-messages view.</li>
     *   <li>Selects the <b>Value</b> filter with text {@code package=sent} and offset {@code 195},
     *       confirming the query becomes {@code messages=offset:195 retrieve=50 package=sent where=value}
     *       and that only the 45 messages with the matching value (offsets 200-244) are shown, ascending,
     *       starting with value {@code package=sent - 0}.</li>
     * </ul>
     *
     * <p>Throughout the test, assertions are made on message offsets, keys, headers, values, table
     * row counts, and the resulting search-query attribute to ensure filtering logic and UI behavior
     * work as expected.</p>
     */
    @Test
    @TestBucket(VARIOUS_MESSAGE_TYPES_BUCKET)
    void testFilterMessagesUsingUIForm() {
        LOGGER.info("Running popover filter-form scenario on topic '{}' (key, headers, and value filters)", kafkaTopicName);
        final String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), kafkaTopicName);
        PwUtils.navigate(tcc, PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), topicId));

        LOGGER.debug("Waiting for page toolbar to be fully loaded before filtering");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, kafkaTopicName, true);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);

        LOGGER.info("Verifying default state: latest 50 messages shown, row 1 = offset 299, query = 'messages=latest retrieve=50'");
        PwUtils.waitForLocatorCount(tcc, 50, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(1), VALUE_FILTER + " - 99", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "299", true);
        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest", Constants.VALUE_ATTRIBUTE, true, true);
        assertTrue(PwUtils.attributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, Constants.VALUE_ATTRIBUTE,
             "messages=latest retrieve=50", true));

        LOGGER.info("Filtering messages by key [{}] with no offset specified - expecting 'No messages data'", KEY_FILTER);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS).nth(2).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, KEY_FILTER);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_EMPTY_FILTER_SEARCH_CONTENT, "No messages data", true);

        LOGGER.info("Setting offset to 95 and reapplying key filter [{}] - expecting 5 matching messages", KEY_FILTER);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        // Take last messages of the first set and let it overlap with second set to see if it filters them out
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "95");

        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:95", Constants.VALUE_ATTRIBUTE, true, true);
        assertTrue(PwUtils.attributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, Constants.VALUE_ATTRIBUTE, "messages=offset:95 retrieve=50 orderID where=key", true));

        // Order is ASC
        LOGGER.debug("Verifying key-filtered messages: expecting 5 rows (offsets 95-99, ascending) matching key [{}]", KEY_FILTER);
        PwUtils.waitForLocatorCount(tcc, 5, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "95", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 3), KEY_FILTER, true);

        LOGGER.debug("Resetting key filter - expecting fallback to default latest-50 view");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_RESET_BUTTON);
        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest", Constants.VALUE_ATTRIBUTE, true, true);
        assertTrue(PwUtils.attributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, Constants.VALUE_ATTRIBUTE, "messages=latest retrieve=50", true));

        // Order is DESC
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "299", true);

        LOGGER.info("Filtering messages by Headers lookup [{}] at offset 95 - expecting 45 matching messages", HEADER_FILTER_LOOK_UP_TEXT);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS).nth(3).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, HEADER_FILTER_LOOK_UP_TEXT);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "95");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:95", Constants.VALUE_ATTRIBUTE, true, true);
        assertTrue(PwUtils.attributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, Constants.VALUE_ATTRIBUTE,
            "messages=offset:95 retrieve=50 " + HEADER_FILTER_LOOK_UP_TEXT + " where=headers", true));

        // Because filter retrieve overlaps 5 messages from previous set, there should be only 45 with correct header
        // Order is ASC
        LOGGER.debug("Verifying header-filtered messages: expecting 45 rows (offsets 100-144, ascending) starting with value [{} - 0]", HEADER_FILTER_MESSAGE);
        PwUtils.waitForLocatorCount(tcc, 45, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "100", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 4), HEADER_FILTER_LOOK_UP_TEXT, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), HEADER_FILTER_MESSAGE + " - 0", true);

        LOGGER.debug("Resetting header filter - expecting fallback to default latest-50 view");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_RESET_BUTTON);
        // Order is DESC
        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest", Constants.VALUE_ATTRIBUTE, true, true);
        assertTrue(PwUtils.attributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, Constants.VALUE_ATTRIBUTE, "messages=latest retrieve=50", true));

        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "299", true);

        LOGGER.info("Filtering messages by Value [{}] at offset 195 - expecting 45 matching messages", VALUE_FILTER);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);

        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_POPUP_DROPDOWN_ITEMS).nth(4).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, VALUE_FILTER);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "195");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);

        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:195", Constants.VALUE_ATTRIBUTE, true, true);
        assertTrue(PwUtils.attributeContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, Constants.VALUE_ATTRIBUTE, "messages=offset:195 retrieve=50 " + VALUE_FILTER + " where=value", true));

        // Because filter retrieve overlaps 5 messages from previous set, there should be only 45 with correct message value
        // Order is ASC
        LOGGER.debug("Verifying value-filtered messages: expecting 45 rows (offsets 200-244, ascending) starting with value [{} - 0]", VALUE_FILTER);
        PwUtils.waitForLocatorCount(tcc, 45, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "200", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), VALUE_FILTER + " - 0", true);

        LOGGER.info("Completed popover filter-form scenarios (key, headers, value) on topic '{}'", kafkaTopicName);
    }

    /**
     * Prepares the {@code VariousMessageTypes} shared bucket by creating a single Kafka topic
     * (name prefix {@code filter-messages}) and producing 300 messages in three consecutive
     * batches of {@code MESSAGE_COUNT} (100) messages each, so that subsequent tests can filter
     * by key, header, and value:
     *
     * <ul>
     *   <li>Offsets 0-99: key {@code orderID}, message {@code my-order - <n>}.</li>
     *   <li>Offsets 100-199: key {@code NoDataInKey-True}, header {@code traceID=abc123},
     *       message {@code abc123 - <n>}.</li>
     *   <li>Offsets 200-299: header {@code NoDataInHeader=true}, message {@code package=sent - <n>}.</li>
     * </ul>
     *
     * <p>Each producer is executed and its message count is verified to succeed before continuing,
     * ensuring that all 300 messages are reliably available before the search/filter tests that
     * share this bucket run.</p>
     */
    @SetupTestBucket(VARIOUS_MESSAGE_TYPES_BUCKET)
    public void prepareVariousMessageTypes() {
        LOGGER.info("Preparing '{}' bucket: creating topic '{}*' and producing 3 batches of {} messages", VARIOUS_MESSAGE_TYPES_BUCKET, TOPIC_PREFIX, MESSAGE_COUNT);

        kafkaTopicName = KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), TOPIC_PREFIX, TOPIC_COUNT, 1, 1, 1)
            .getFirst().getMetadata().getName();
        LOGGER.debug("Created topic '{}' for message filtering scenarios", kafkaTopicName);

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

        LOGGER.info("Producing batch 1/3: {} messages on offsets 0-99 with key '{}' and value '{}'", MESSAGE_COUNT, KEY_FILTER, KEY_FILTER_MESSAGE);
        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(clients.getNamespaceName(), clients.getProducerName(), clients.getMessageCount(), true);
        LOGGER.debug("Batch 1/3 produced and consumed successfully");

        // create second set of messages with different HEADER and MESSAGE
        clients.setMessageKey("NoDataInKey-True");
        clients.setHeaders(HEADER_FILTER);
        clients.setMessage(HEADER_FILTER_MESSAGE);

        LOGGER.info("Producing batch 2/3: {} messages on offsets 100-199 with header '{}' and value '{}'", MESSAGE_COUNT, HEADER_FILTER, HEADER_FILTER_MESSAGE);
        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(clients.getNamespaceName(), clients.getProducerName(), clients.getMessageCount(), true);
        LOGGER.debug("Batch 2/3 produced and consumed successfully");
        // create third set of messages with different MESSAGE
        clients.setHeaders("NoDataInHeader=true");
        clients.setMessage(VALUE_FILTER);

        LOGGER.info("Producing batch 3/3: {} messages on offsets 200-299 with value '{}'", MESSAGE_COUNT, VALUE_FILTER);
        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(clients.getNamespaceName(), clients.getProducerName(), clients.getMessageCount(), true);
        LOGGER.debug("Batch 3/3 produced and consumed successfully");
        LOGGER.info("Filtering scenario prepared: topic '{}' now holds {} messages", kafkaTopicName, MESSAGE_COUNT * 3);
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
