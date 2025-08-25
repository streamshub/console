package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.locators.ConsumerGroupsPageSelectors;
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

@Tag(TestTags.REGRESSION)
public class ConsumerST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerST.class);
    private static TestCaseConfig tcc;

    private static final String TOPIC_PREFIX = "rst-all-topics-var-offset";
    private static final int MESSAGE_COUNT = Constants.MESSAGE_COUNT_HIGH;
    private static final int TOPIC_COUNT = 2;
    private static final String RESET_OFFSET_CONSUMER_GROUP = "reset-offset-consumer-group";

    /**
     * Tests resetting Kafka consumer group offsets across multiple topics using various offset reset types via the UI.
     *
     * <p>The test class first prepares the scenario by creating topics and producing/consuming messages to set initial offsets.</p>
     * <p>Test method then retrieves these prepared topics for each reset scenario.</p>
     * <p>It verifies the default consumer offsets could be reset using UI `Reset offsets` button.</p>
     * <p>The test then navigates to the Consumer Groups page and uses the UI to reset offsets for all topics using:</p>
     * <ul>
     *   <li>Earliest offset reset</li>
     *   <li>Latest offset reset</li>
     *   <li>Date/time-based earliest / latest / half-way offset resets with UNIX epoch format</li>
     *   <li>Date/time-based earliest / latest / half-way offset resets with ISO-8601 format</li>
     * </ul>
     * <p>For each reset type, the test verifies that the consumer group offsets are updated accordingly by querying Kafka through the broker pod.</p>
     * <p>The test ensures that both dry-run and actual reset operations work as expected and that offsets reflect the UI commands.</p>
     *
     * <p>This comprehensive test validates the correctness of consumer offset reset functionality across multiple topics and offset types,
     * ensuring synchronization between UI actions and Kafka consumer group state.</p>
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

    @ParameterizedTest(name = "Type: {1} - DateTime: {2} - Offset: {3}")
    @MethodSource("offsetResetScenarios")
    void testResetConsumerOffsetAllTopicsAllPartitions(int messageCount,
        ResetOffsetType resetType, ResetOffsetDateTimeType dateTimeType, String expectedOffset) {

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();

        // Get topics for test from prepared scenario
        List<String> kafkaTopicNames = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), TOPIC_PREFIX)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList();

        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), RESET_OFFSET_CONSUMER_GROUP));
        PwUtils.waitForElementEnabledState(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);

        // Look at the offset in UI
        for (String kafkaTopicName : kafkaTopicNames) {
            LOGGER.info("Verify default consumer offset");
            KafkaCmdUtils.setConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP, kafkaTopicName, String.valueOf(messageCount),
                KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT));

            assertEquals(String.valueOf(messageCount),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP, kafkaTopicName,
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

            tcc.page().navigate(PwPageUrls.getConsumerGroupsResetOffsetPage(tcc, tcc.kafkaName(), RESET_OFFSET_CONSUMER_GROUP));
            ConsumerTestUtils.execDryRunAndReset(tcc, resetType, dateTimeType, resetValue);

            LOGGER.info("Verify expected consumer offset value");
            assertEquals(String.valueOf(expectedOffset),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP, kafkaTopicName,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));
        }
    }

    public void setupConsumersScenario() {
        // Test class specific
        tcc.setMessageCount(MESSAGE_COUNT);

        LOGGER.info("Prepare consumer offset scenario by creating topic(s) and then producing and consuming messages");

        List<String> kafkaTopicNames = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), TOPIC_PREFIX, TOPIC_COUNT, true, 1, 1, 1)
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
                .withConsumerGroup(RESET_OFFSET_CONSUMER_GROUP)
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

        setupConsumersScenario();
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
