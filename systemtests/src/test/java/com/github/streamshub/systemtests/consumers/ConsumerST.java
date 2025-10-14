package com.github.streamshub.systemtests.consumers;

import com.github.streamshub.console.support.Identifiers;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.locators.SingleConsumerGroupPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
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
import com.github.streamshub.systemtests.utils.testutils.ConsumerTestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag(TestTags.REGRESSION)
class ConsumerST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerST.class);
    private static final String RESET_OFFSET_BUCKET = "ResetOffset";

    // Shared
    protected TestCaseConfig tcc;
    private static final int MESSAGE_COUNT = Constants.MESSAGE_COUNT_HIGH;

    // ResetOffset TestBucket
    private static final String RESET_OFFSET_TOPIC_PREFIX = "rst-all-topics-var-offset";
    private static final int RESET_OFFSET_TOPIC_COUNT = 2;
    private static final String RESET_OFFSET_CONSUMER_GROUP_NAME = "reset-offset-consumer-group";

    /**
     * Provides parameterized scenarios for verifying consumer group offset reset functionality
     * across all topics and partitions using different reset types.
     *
     * <p>Each scenario defines:</p>
     * <ul>
     *   <li>The total number of messages in the topic.</li>
     *   <li>The type of offset reset to perform (EARLIEST, LATEST, DATE_TIME).</li>
     *   <li>The date/time type when using DATE_TIME reset (UNIX_EPOCH or ISO_8601).</li>
     *   <li>The expected offset value after the reset.</li>
     * </ul>
     *
     * <p>Scenarios include:</p>
     * <ul>
     *   <li>Resetting to the earliest offset.</li>
     *   <li>Resetting to the latest offset.</li>
     *   <li>Resetting based on a specific timestamp using UNIX epoch format.</li>
     *   <li>Resetting based on a specific timestamp using ISO-8601 format.</li>
     *   <li>Resetting to a midpoint offset (halfway through the messages).</li>
     * </ul>
     *
     * <p>This ensures comprehensive coverage of all supported offset reset types,
     * verifying that the consumer group offsets are updated correctly in Kafka
     * and that the UI reflects these changes accurately.</p>
     */
    public Stream<Arguments> offsetResetScenarios() {
        final String earliestOffsetIndex = "0";
        // Use index to reset consumers to previous offset to read timestamp
        final String latestOffsetIndex = String.valueOf(MESSAGE_COUNT - 1);
        final String middleOffsetIndex = String.valueOf((int) Math.ceil(MESSAGE_COUNT / 2.0) - 1);

        return Stream.of(
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.EARLIEST, null, earliestOffsetIndex),
            // Only one that uses `--to-latest` which sets the index to nth+1 for consuming the next message
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.LATEST, null, String.valueOf(MESSAGE_COUNT)),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, earliestOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, latestOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, middleOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, earliestOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, latestOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, middleOffsetIndex)
        );
    }

    /**
     * Executes parameterized tests for resetting Kafka consumer group offsets
     * across all topics and partitions using the UI.
     *
     * <p> For each scenario provided by {@link #offsetResetScenarios()}:</p>
     * <ul>
     *   <li>Navigates to the Consumer Groups page for the test consumer group.</li>
     *   <li>Verifies the current default offset for each topic.</li>
     *   <li> If a DATE_TIME reset is used, calculates the appropriate timestamp
     *       based on the expected offset and selected date/time format (UNIX_EPOCH or ISO-8601).</li>
     *   <li>Performs a dry-run and actual reset of offsets using the UI reset page.</li>
     *   <li>Validates that the consumer group offsets are updated correctly in Kafka
     *       by querying the broker pod directly.</li>
     * </ul>
     *
     * <p>This ensures that all supported reset types (EARLIEST, LATEST, and DATE_TIME)
     * work as expected for multiple topics and partitions, and that the UI commands
     * synchronize correctly with the underlying Kafka consumer group state.</p>
     *
     * @param messageCount the total number of messages in the topic
     * @param resetType the type of offset reset to perform (EARLIEST, LATEST, DATE_TIME)
     * @param dateTimeType the date/time format used for DATE_TIME resets (UNIX_EPOCH or ISO-8601), null otherwise
     * @param expectedOffset the expected offset value after the reset operation
     */
    @TestBucket(RESET_OFFSET_BUCKET)
    @ParameterizedTest(name = "Type: {1} - DateTime: {2} - Offset: {3}")
    @MethodSource("offsetResetScenarios")
    void testResetConsumerOffsetAllTopicsAllPartitions(int messageCount,
        ResetOffsetType resetType, ResetOffsetDateTimeType dateTimeType, String expectedOffset) {

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();

        // Get topics for test from prepared scenario
        List<String> kafkaTopicNames = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), RESET_OFFSET_TOPIC_PREFIX)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList();

        assertFalse(kafkaTopicNames.isEmpty());

        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
        PwUtils.waitForElementEnabledState(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);

        // Look at the offset in UI
        for (String kafkaTopicName : kafkaTopicNames) {
            LOGGER.info("Verify default consumer offset");
            KafkaCmdUtils.setConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName, String.valueOf(messageCount),
                KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT));

            assertEquals(String.valueOf(messageCount),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));

            String resetValue = expectedOffset;
            // To determine offset timestamp from offsetNumber
            if (dateTimeType != null) {
                if (dateTimeType.equals(ResetOffsetDateTimeType.UNIX_EPOCH)) {
                    resetValue = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                         KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), expectedOffset, 0, 1);
                } else {
                    String epoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                        KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), expectedOffset, 0, 1);
                    resetValue = Instant.ofEpochMilli(Long.parseLong(epoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }

            tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
            PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER, RESET_OFFSET_CONSUMER_GROUP_NAME, true);

            ConsumerTestUtils.execDryRunAndReset(tcc, resetType, dateTimeType, resetValue);

            LOGGER.info("Verify expected consumer offset value");
            assertEquals(String.valueOf(expectedOffset),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));
        }
    }

    /**
     * Prepares the Kafka consumer offset test scenario by creating topics
     * and producing/consuming messages for each topic.
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *   <li>Sets the total message count for the test context.</li>
     *   <li>Creates one or more Kafka topics with the configured prefix and count.</li>
     *   <li>For each topic, creates a Kafka producer and consumer using the
     *       {@link #RESET_OFFSET_CONSUMER_GROUP_NAME} consumer group.</li>
     *   <li>Produces the specified number of messages to each topic and consumes them
     *       to set initial consumer offsets.</li>
     *   <li>Waits for all client operations to complete successfully, ensuring offsets
     *       are properly initialized for testing offset reset scenarios.</li>
     * </ul>
     *
     * <p>This setup ensures that the consumer offset reset tests have a consistent
     * initial state across all topics and partitions.</p>
     */
    @SetupTestBucket(RESET_OFFSET_BUCKET)
    public void setupConsumerGroupResetOffset() {
        tcc.setMessageCount(MESSAGE_COUNT);

        LOGGER.info("Prepare consumer offset scenario by creating topic(s) and then producing and consuming messages");

        List<String> kafkaTopicNames = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), RESET_OFFSET_TOPIC_PREFIX, RESET_OFFSET_TOPIC_COUNT, true, 1, 1, 1)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList();

        for (String kafkaTopicName : kafkaTopicNames) {
            KafkaClients clients = new KafkaClientsBuilder()
                .withNamespaceName(tcc.namespace())
                .withTopicName(kafkaTopicName)
                .withMessageCount(tcc.messageCount())
                .withDelayMs(0)
                .withProducerName(KafkaNamingUtils.producerName(kafkaTopicName))
                .withConsumerName(KafkaNamingUtils.consumerName(kafkaTopicName))
                .withConsumerGroup(RESET_OFFSET_CONSUMER_GROUP_NAME)
                .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
                .withUsername(tcc.kafkaUserName())
                .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
                .build();

            KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        }

        LOGGER.info("Reset consumer offset scenario ready");
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
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
