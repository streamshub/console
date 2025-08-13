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
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

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

    private Stream<Arguments> offsetResetScenarios() {
        final int messageCount = Constants.MESSAGE_COUNT_HIGH;

        final String earliestOffsetIndex = "0";
        // Use index to reset consumers to previous offset to read timestamp
        final String latestOffsetIndex = String.valueOf(messageCount - 1);
        final String middleOffsetIndex = String.valueOf((int) Math.ceil(messageCount / 2.0) - 1);

        return Stream.of(
            Arguments.of(messageCount, ResetOffsetType.EARLIEST, null, earliestOffsetIndex),
            // Only one that uses `--to-latest` which sets the index to nth+1 for consuming the next message
            Arguments.of(messageCount, ResetOffsetType.LATEST, null, messageCount),
            Arguments.of(messageCount, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, earliestOffsetIndex),
            Arguments.of(messageCount, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, latestOffsetIndex),
            Arguments.of(messageCount, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, middleOffsetIndex),
            Arguments.of(messageCount, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, earliestOffsetIndex),
            Arguments.of(messageCount, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, latestOffsetIndex),
            Arguments.of(messageCount, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, middleOffsetIndex)
        );
    }

    @ParameterizedTest(name = "Type: {1} - DateTime: {2} - Offset: {3}")
    @MethodSource("offsetResetScenarios")
    void testResetConsumerOffsetAllTopicsAllPartitions(int messageCount,
        ResetOffsetType resetType, ResetOffsetDateTimeType dateTimeType, String expectedOffset) {

        final TestCaseConfig tcc = getTestCaseConfig();
        tcc.setMessageCount(messageCount);

        final String topicPrefix = "rst-all-topics-var-offset";
        final String consumerGroupName = KafkaNamingUtils.consumerGroupName(topicPrefix);
        final String clientsConfig = KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT);
        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();
        final int topicCount = 2;

        // Set up if first test or if previous test failed and removed kafka topics or list if previous test passed
        List<String> kafkaTopicNames = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), topicPrefix)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList();

        if (kafkaTopicNames.isEmpty()) {
            kafkaTopicNames = ConsumerTestUtils.prepareConsumerGroupOffsetScenarioAndReturnTopicNames(tcc, topicPrefix, consumerGroupName, topicCount, 1, 1, 1);
        }

        LOGGER.info("Verify default consumer offset");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupName));
        PwUtils.waitForElementEnabledState(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);
        // Look at the offset in UI
        for (String kafkaTopicName : kafkaTopicNames) {
            KafkaCmdUtils.setConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, kafkaTopicName, String.valueOf(messageCount), clientsConfig);
            assertEquals(String.valueOf(messageCount),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, kafkaTopicName, clientsConfig));

            String resetValue = expectedOffset;
            // To determine offset timestamp from offsetNumber
            if (dateTimeType != null) {
                if (dateTimeType.equals(ResetOffsetDateTimeType.UNIX_EPOCH)) {
                    resetValue = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                         clientsConfig, expectedOffset, 0, 1);
                } else {
                    String epoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                        clientsConfig, expectedOffset, 0, 1);
                    resetValue = Instant.ofEpochMilli(Long.parseLong(epoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }

            tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), consumerGroupName));
            ConsumerTestUtils.execDryRunAndReset(tcc, resetType, dateTimeType, resetValue);

            assertEquals(String.valueOf(expectedOffset),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, consumerGroupName, kafkaTopicName, clientsConfig));
        }
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
    }
}
