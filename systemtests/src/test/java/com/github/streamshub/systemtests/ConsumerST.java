package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.locators.ConsumerGroupsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaConsumerUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsumerST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerST.class);

    @Test
    void testResetConsumerOffset() {
        final TestCaseConfig tcc = getTestCaseConfig();
        final String topicPrefix = "offset-reset";

        LOGGER.info("Create topic and produce messages for filtering test");
        String kafkaTopicName = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), topicPrefix, 1, true, 1, 1, 1)
            .get(0).getMetadata().getName();

        final String consumerGroupName = KafkaNamingUtils.consumerGroupName(kafkaTopicName);

        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(kafkaTopicName)
            .withMessageCount(Constants.MESSAGE_COUNT_HIGH)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(kafkaTopicName))
            .withConsumerName(KafkaNamingUtils.consumerName(kafkaTopicName))
            .withConsumerGroup(consumerGroupName)
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupName));

        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_CONSUMER_OFFSET_BUTTON);

        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_CONSUMER_GROUP_NAME, consumerGroupName, true);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_EARLIEST_OFFSET);
        // Dryrun
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_DRY_RUN_BUTTON);
        // Check selector for text
        String dryRunCommand = String.format("$ kafka-consumer-groups --bootstrap-server ${bootstrap-Server} --group %s --reset-offsets --all-topics --to-earliest --dry-run", consumerGroupName);
        PwUtils.waitForContainsAttribute(tcc, ConsumerGroupsPageSelectors.CGPS_DRY_RUN_COMMAND, dryRunCommand, Constants.VALUE_ATTRIBUTE, true);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_BACK_TO_EDIT_OFFSET_BUTTON);

        // Reset for real now
        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_CONSUMER_GROUP_NAME, consumerGroupName, true);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_EARLIEST_OFFSET);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_RESET_BUTTON);
        // Check offset
        String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, clients), "0");
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
