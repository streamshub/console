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
import com.github.streamshub.systemtests.utils.resourceutils.KafkaCmdUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
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

    /**
     * Tests resetting Kafka consumer group offsets across multiple topics using various offset reset types via the UI.
     *
     * <p>The test first prepares the scenario by creating topics and producing/consuming messages to set initial offsets.</p>
     * <p>It verifies the default consumer offsets correspond to the latest message offsets before any reset.</p>
     * <p>The test then navigates to the Consumer Groups page and uses the UI to reset offsets for all topics using:</p>
     * <ul>
     *   <li>Earliest offset reset</li>
     *   <li>Latest offset reset</li>
     *   <li>Date/time-based earliest latest offset resets with UNIX epoch format</li>
     *   <li>Date/time-based middle offset resets with ISO-8601 format</li>
     * </ul>
     * <p>For each reset type, the test verifies that the consumer group offsets are updated accordingly by querying Kafka through the broker pod.</p>
     * <p>The test ensures that both dry-run and actual reset operations work as expected and that offsets reflect the UI commands.</p>
     *
     * <p>This comprehensive test validates the correctness of consumer offset reset functionality across multiple topics and offset types,
     * ensuring synchronization between UI actions and Kafka consumer group state.</p>
     */
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
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), latestOffset);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), latestOffset);

        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupName));
        // Reset offset button needs to wait a bit after consumers are done consuming,
        // the button is disabled and goes to enabled when it's ready to reset offsets, to check this verify that disabled attribute is not null
        PwUtils.waitForElementEnabledState(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_CONSUMER_OFFSET_BUTTON);

        LOGGER.info("Verify earliest offset reset");
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.EARLIEST, null, null);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), earliestOffset);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), earliestOffset);

        LOGGER.info("Verify latest offset reset");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.LATEST, null, null);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), latestOffset);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), latestOffset);

        LOGGER.info("Verify UNIX datetime from earliest");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, earliestEpoch);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), earliestOffset);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), earliestOffset);

        LOGGER.info("Verify UNIX datetime latest for topic 0");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        String latestEpoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-0", clientsConfig, latestOffsetIndex, 0, 1);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, latestEpoch);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), latestOffsetIndex);

        LOGGER.info("Verify UNIX datetime latest for topic 1");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        latestEpoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-1", clientsConfig, latestOffsetIndex, 0, 1);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, latestEpoch);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), latestOffsetIndex);

        LOGGER.info("Verify ISO datetime from middle offset for topic 0");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        String middleEpoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-0", clientsConfig, middleOffset, 0, 1);
        String middleIsoTime = Instant.ofEpochMilli(Long.parseLong(middleEpoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, middleIsoTime);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-0", clientsConfig), middleOffset);

        LOGGER.info("Verify ISO datetime from middle offset for topic 1");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
        middleEpoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, topicPrefix + "-1", clientsConfig, middleOffset, 0, 1);
        middleIsoTime = Instant.ofEpochMilli(Long.parseLong(middleEpoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ConsumerTestUtils.execDryRunAndReset(tcc, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, middleIsoTime);
        assertEquals(KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, topicPrefix + "-1", clientsConfig), middleOffset);
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
