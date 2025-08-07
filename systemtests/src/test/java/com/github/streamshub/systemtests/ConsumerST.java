package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.locators.ConsumerGroupsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaConsumerUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testutils.ConsumerTestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.TestFrameConstants;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsumerST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerST.class);

    @Test
    void testResetConsumerOffsetAllTopicsVariousOffsets() {
        final TestCaseConfig tcc = getTestCaseConfig();
        tcc.setMessageCount(Constants.MESSAGE_COUNT_HIGH);

        final String topicPrefix = "rst-all-topics-var-offset";
        final String consumerGroupName = KafkaNamingUtils.consumerGroupName(topicPrefix);
        final String clientsConfig = KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT);
        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();

        final String earliestEpoch = "0";
        final String earliestOffset = "0";
        final String latestOffset = String.valueOf(tcc.messageCount());
        final String latestOffsetIndex = String.valueOf(tcc.messageCount() - 1);
        final String middleOffset = String.valueOf((int) Math.ceil(tcc.messageCount() / 2.0));

        ConsumerTestUtils.prepareConsumerOffsetScenario(tcc, topicPrefix, consumerGroupName, 2, 1, 1, 1);

        LOGGER.info("Verify default offset");
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), latestOffset);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), latestOffset);

        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupName));
        // Reset offset button needs to wait a bit after consumers are done consuming,
        // the button is disabled and goes to enabled when it's ready to reset offsets, to check this verify that disabled attribute is not null
        PwUtils.waitForElementEnabledState(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_CONSUMER_OFFSET_BUTTON);

        LOGGER.info("Verify earliest offset reset");
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.EARLIEST, null, null);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), earliestOffset);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), earliestOffset);

        LOGGER.info("Verify latest offset reset");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.LATEST, null, null);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), latestOffset);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), latestOffset);

        LOGGER.info("Verify UNIX datetime from earliest");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, earliestEpoch);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), earliestOffset);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), earliestOffset);

        LOGGER.info("Verify UNIX datetime latest for topic 0");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        String latestEpoch = KafkaTopicUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-0", clientsConfig, latestOffsetIndex, 0, 1);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, latestEpoch);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), latestOffsetIndex);

        LOGGER.info("Verify UNIX datetime latest for topic 1");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        latestEpoch = KafkaTopicUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-1", clientsConfig, latestOffsetIndex, 0, 1);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, latestEpoch);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), latestOffsetIndex);

        LOGGER.info("Verify ISO datetime from middle offset for topic 0");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        String middleEpoch = KafkaTopicUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-0", clientsConfig, middleOffset, 0, 1);
        String middleIsoTime = Instant.ofEpochMilli(Long.parseLong(middleEpoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, middleIsoTime);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), middleOffset);

        LOGGER.info("Verify ISO datetime from middle offset for topic 1");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        middleEpoch = KafkaTopicUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-1", clientsConfig, middleOffset, 0, 1);
        middleIsoTime = Instant.ofEpochMilli(Long.parseLong(middleEpoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, middleIsoTime);
        assertEquals(KafkaConsumerUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), middleOffset);
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
