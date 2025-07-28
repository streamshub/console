package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
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
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MessagesST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(MessagesST.class);

    /**
     * Verifies message filtering functionality on the Messages page using various search text inputs.
     *
     * <p>This test performs the following steps:
     * <ul>
     *     <li>Creates a Kafka topic and produces a batch of messages to it.</li>
     *     <li>Waits for the messages page to load and verifies default message count and ordering.</li>
     *     <li>Tests filtering the latest messages (e.g., retrieve latest 2 messages).</li>
     *     <li>Tests retrieving messages from a specific offset.</li>
     *     <li>Tests filtering messages by matching content ("world - 42").</li>
     *     <li>Tests that no unrelated messages are shown when filters do not match any result.</li>
     *     <li>Tests the fallback behavior when an invalid retrieve value is provided.</li>
     * </ul>
     *
     * <p>Checks are performed on both message content and the number of results returned.
     */
    @Test
    void testMessageSearchUsingQueries() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final String topicPrefix = "filter-text";

        LOGGER.info("Create topic and produce messages for filtering test");
        String kafkaTopicName = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), topicPrefix, 1, true, 1, 1, 1)
            .get(0).getMetadata().getName();

        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(kafkaTopicName)
            .withMessageCount(Constants.MESSAGE_COUNT_HIGH)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(kafkaTopicName))
            .withConsumerName(KafkaNamingUtils.consumerName(kafkaTopicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(kafkaTopicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        tcc.page().navigate(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), ResourceUtils.getKubeResource(KafkaTopic.class, tcc.namespace(), kafkaTopicName).getStatus().getTopicId()));

        LOGGER.info("Wait for message search page toolbar to be fully there before filtering messages");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, kafkaTopicName, true);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);

        LOGGER.info("Verify default order and number of displayed messages");
        PwUtils.waitForLocatorCount(tcc, 50, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(1), "Hello-world - 9999", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "9,999", true);

        LOGGER.info("Start filtering messages by input text");

        LOGGER.info("Get latest 2 messages");
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest retrieve=2");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 2, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(1), "Hello-world - 9999", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(2), "Hello-world - 9998", true);

        LOGGER.info("Get 20 messages from offset 365");
        PwUtils.fill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:365 retrieve=20");
        PwUtils.click(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 20, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), "Hello-world - 365", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "365", true);

        LOGGER.info("Get message containing word from all messages");
        PwUtils.fill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:1 retrieve=100 world - 42");
        PwUtils.click(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), "Hello-world - 42", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "42", true);

        LOGGER.info("Get NONE message containing word from different messages while fetching another message");
        PwUtils.fill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest retrieve=40 world - 42");
        PwUtils.click(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, "No messages data", true);

        LOGGER.info("Test that invalid number retrieve input gets reverted to a default input");
        PwUtils.fill(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=totalyNotOkay retrieve=-9");
        PwUtils.click(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_ENTER_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 50, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(1), "Hello-world - 9999", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(2), "Hello-world - 9998", true);
    }


    /**
     * Verifies message filtering functionality using the form-based UI filter panel on the Messages page.
     *
     * <p>This test performs the following steps:
     * <ul>
     *     <li>Creates a Kafka topic and sends three distinct sets of messages with different:
     *         <ul>
     *             <li>Message keys (e.g., "orderID")</li>
     *             <li>Headers (e.g., "traceID=abc123")</li>
     *             <li>Message values (e.g., "package=sent")</li>
     *         </ul>
     *     </li>
     *     <li>Verifies the initial default state of the message table.</li>
     *     <li>Tests key-based filtering using the form panel, including showing "No messages data" when offset is not set.</li>
     *     <li>Tests applying offset for proper filtering and verifies correct message is returned.</li>
     *     <li>Tests header-based filtering using the form panel and verifies correct subset is returned.</li>
     *     <li>Tests value-based filtering using the form panel and verifies correct subset is returned.</li>
     *     <li>Performs filter reset between each filtering step and verifies table content.</li>
     * </ul>
     *
     * <p>Checks are made on message content, offsets, keys, headers, and values to ensure correctness of filtering logic.
     */
    @Test
    void testFilterMessagesUsingUIForm() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final String topicPrefix = "ui-form-filter";
        final String keyFilter = "orderID";
        final String keyFilterMessage = "my-order";

        final String headerFilter = "traceID=abc123";
        final String headerFilterLookUpText = "traceID";
        final String headerFilterMessage = "abc123";

        final String valueFilter = "package=sent";

        LOGGER.info("Create topic and produce multiple different messages for filtering test");
        String kafkaTopicName = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), topicPrefix, 1, true, 1, 1, 1)
            .get(0).getMetadata().getName();

        // Set clients to send messages with KEY
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(kafkaTopicName)
            .withMessageCount(Constants.MESSAGE_COUNT)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(kafkaTopicName))
            .withConsumerName(KafkaNamingUtils.consumerName(kafkaTopicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(kafkaTopicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage(keyFilterMessage)
            .withMessageKey(keyFilter)
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        // create second set of messages with different HEADER and MESSAGE
        clients.setMessageKey("NoDataInKey-True");
        clients.setHeaders(headerFilter);
        clients.setMessage(headerFilterMessage);

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);
        // create third set of messages with different MESSAGE
        clients.setHeaders("NoDataInHeader=true");
        clients.setMessage(valueFilter);

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);
        tcc.page().navigate(PwPageUrls.getMessagesPage(tcc, tcc.kafkaName(), ResourceUtils.getKubeResource(KafkaTopic.class, tcc.namespace(), kafkaTopicName).getStatus().getTopicId()));

        LOGGER.info("Wait for page toolbar to be fully loaded before filtering");
        PwUtils.waitForContainsText(tcc, CssSelectors.PAGES_CONTENT_HEADER_TITLE_CONTENT, kafkaTopicName, true);
        PwUtils.waitForLocatorVisible(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT);

        LOGGER.info("Verify default state of displayed messages");
        PwUtils.waitForLocatorCount(tcc, 50, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItems(1), valueFilter + " - 99", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "299", true);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=latest retrieve=50", Constants.VALUE_ATTRIBUTE, true);

        LOGGER.info("Filter messages by key - because no offset is specified, first `No message data` should appear");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_OPEN_POPOVER_FORM_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_WHERE_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_FILTER_WHERE_DROPDOWN_ITEMS).nth(2).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, keyFilter);
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
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 3), keyFilter, true);

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
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, headerFilterLookUpText);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "95");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:95 retrieve=50 " + headerFilterLookUpText + " where=headers", Constants.VALUE_ATTRIBUTE, true);
        // Because filter retrieve overlaps 5 messages from previous set, there should be only 45 with correct header
        // Order is ASC
        LOGGER.debug("Verify filtered messages with specific header");
        PwUtils.waitForLocatorCount(tcc, 45, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "100", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 4), headerFilterLookUpText, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), headerFilterMessage + " - 0", true);

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
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_HAS_WORDS_INPUT, valueFilter);
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES);
        PwUtils.waitForLocatorAndClick(tcc, new CssBuilder(MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_DROPDOWN_ITEMS).nth(1).build());
        PwUtils.waitForLocatorAndFill(tcc, MessagesPageSelectors.MPS_TPF_PARAMETERS_MESSAGES_OFFSET_INPUT, "195");
        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.MPS_TPF_SEARCH_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, MessagesPageSelectors.MPS_SEARCH_TOOLBAR_QUERY_INPUT, "messages=offset:195 retrieve=50 " + valueFilter + " where=value", Constants.VALUE_ATTRIBUTE, true);
        // Because filter retrieve overlaps 5 messages from previous set, there should be only 45 with correct message value
        // Order is ASC
        LOGGER.debug("Verify filtered messages with specific message value");
        PwUtils.waitForLocatorCount(tcc, 45, MessagesPageSelectors.MPS_SEARCH_RESULTS_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 1), "200", true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.getTableRowItem(1, 5), valueFilter + " - 0", true);
    }


    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
