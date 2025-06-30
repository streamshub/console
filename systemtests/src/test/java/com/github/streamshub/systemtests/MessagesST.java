package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
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

    @Test
    void testFilterMessages() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final String topicPrefix = "filter-messages";

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

        PwUtils.waitForLocatorVisible(tcc, CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_TEXT);

        LOGGER.info("Start filtering messages by input text");

        LOGGER.info("Get latest 2 messages");
        tcc.page().fill(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_TEXT, "messages=latest retrieve=2");
        tcc.page().click(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 2, CssSelectors.MESSAGES_PAGE_TABLE_SEARCH_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.getMessagesPageTableRowItems(1), "Hello-world - 9999", true);
        PwUtils.waitForContainsText(tcc, CssSelectors.getMessagesPageTableRowItems(2), "Hello-world - 9998", true);

        LOGGER.info("Get 20 messages from offset 365");
        tcc.page().fill(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_TEXT, "messages=offset:365 retrieve=20");
        tcc.page().click(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 20, CssSelectors.MESSAGES_PAGE_TABLE_SEARCH_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.getMessagesPageTableRowItem(1, 5), "Hello-world - 365", true);

        LOGGER.info("Get message containing word from all messages");
        tcc.page().fill(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_TEXT, "messages=offset:1 retrieve=100 world - 42");
        tcc.page().click(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, CssSelectors.MESSAGES_PAGE_TABLE_SEARCH_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.getMessagesPageTableRowItem(1, 5), "Hello-world - 42", true);

        LOGGER.info("Get NONE message containing word from different messages while fetching another message");
        tcc.page().fill(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_TEXT, "messages=latest retrieve=40 world - 42");
        tcc.page().click(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 1, CssSelectors.MESSAGES_PAGE_TABLE_SEARCH_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.MESSAGES_PAGE_TABLE_SEARCH_TABLE_ITEMS, "No messages data", true);

        LOGGER.info("Test that invalid number retrieve input gets reverted to a default input");
        tcc.page().fill(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_TEXT, "messages=totalyNotOkay retrieve=-9");
        tcc.page().click(CssSelectors.MESSAGES_PAGE_SEARCH_TOOLBAR_SEARCH_BUTTON);
        PwUtils.waitForLocatorCount(tcc, 50, CssSelectors.MESSAGES_PAGE_TABLE_SEARCH_TABLE_ITEMS, true);
        PwUtils.waitForContainsText(tcc, CssSelectors.getMessagesPageTableRowItems(1), "Hello-world - 9999", true);
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
