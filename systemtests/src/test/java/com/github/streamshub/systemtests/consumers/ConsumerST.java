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
import com.github.streamshub.systemtests.locators.ConsumerGroupsPageSelectors;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.CssSelectors;
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
import com.microsoft.playwright.Locator;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.REGRESSION)
public class ConsumerST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerST.class);
    private static final String RESET_OFFSET_BUCKET = "ResetOffset";

    // Shared
    protected TestCaseConfig tcc;
    private static final int MESSAGE_COUNT = Constants.MESSAGE_COUNT_HIGH;

    // ResetOffset TestBucket
    private static final String RESET_OFFSET_TOPIC_PREFIX = "rst-all-topics-var-offset";
    private static final int RESET_OFFSET_TOPIC_COUNT = 2;
    private static final String RESET_OFFSET_CONSUMER_GROUP_NAME = "reset-offset-consumer-group";

    public Stream<Arguments> variableConsumerGroupNamesScenario() {
        final int messageCount = Constants.MESSAGE_COUNT;
        return Stream.of(
            Arguments.of("Special chars", messageCount, "group$$$$$%^^&*"),
            Arguments.of("Semicolon separated", messageCount, "group;part;;1"),
            Arguments.of("Dot separated", messageCount, "group.1.3.5"),
            Arguments.of("Colon separated", messageCount, "group:12::3:"),
            Arguments.of("Symbols", messageCount, "group'@!\"#?"),
            Arguments.of("Underscores", messageCount, "group_with__underscores_"),
            Arguments.of("Hyphenated", messageCount, "group-hyphen--name-"),
            Arguments.of("With slash", messageCount, "group/with//slash/"),
            Arguments.of("Equals sign", messageCount, "group=equals==two"),
            Arguments.of("Comma separated", messageCount, "group,comma,separated,,"),
            Arguments.of("With spaces", messageCount, "group space allowed"),
            Arguments.of("Pipe symbol", messageCount, "group|pipe||secondpipe"),
            Arguments.of("Tilde", messageCount, "group~tilde~~name"),
            Arguments.of("Very long name", messageCount,
                "consumer_group_with_really_really_really_long_name_1234567890-1234567890-1234567890")
        );
    }

    @ParameterizedTest(name = "Scenario: {0} - ConsumerGroupName: [{2}]")
    @MethodSource("variableConsumerGroupNamesScenario")
    void testVariableConsumerGroupNames(String displayName, int messageCount, String consumerGroupName) {
        LOGGER.info("NAMESPACE {}", tcc.namespace());

        String topicName = "topic-" + Utils.hashStub(displayName);
        String consumerGroupEncodedName = Identifiers.encode(consumerGroupName);

        // Must be done due to k8s ENV parsing results
        // https://jellepelgrims.com/posts/dollar_signs
        String k8sFriendlyName = consumerGroupName.replace("$", "$$");

        LOGGER.info("Create KafkaTopic CR");
        KubeResourceManager.get().createResourceWithWait(
            KafkaTopicUtils.defaultTopic(tcc.namespace(), tcc.kafkaName(), topicName, 1, 1, 1).build());

        LOGGER.info("Produce and consume messages");
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicName)
            .withMessageCount(messageCount)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(k8sFriendlyName)
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        LOGGER.info("Verify consumer group is displaying correctly");

        LOGGER.info("Navigate to single consumer group page encoded to '{}'", consumerGroupEncodedName);
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupEncodedName));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, consumerGroupName, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(CssSelectors.PAGES_HEADER_BREADCRUMB_ITEMS).nth(4).build(), consumerGroupName, true);

        LOGGER.info("Navigate to consumer groups page to check group is present");
        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), ""), PwUtils.getDefaultNavigateOpts());

        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_HEADER_TITLE, "Consumer Groups", true);
        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS, consumerGroupName, true);

        LOGGER.info("Try click-through link and get redirected to a correct page");
        PwUtils.waitForLocatorAndClick(tcc.page()
            .locator(ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS)
            .locator("a", new Locator.LocatorOptions().setHasText(consumerGroupName)));

        tcc.page().waitForURL(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupEncodedName), PwUtils.getDefaultWaitForUrlOpts());
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, consumerGroupName, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(CssSelectors.PAGES_HEADER_BREADCRUMB_ITEMS).nth(4).build(), consumerGroupName, true);

        LOGGER.info("Check topic page if consumer group is present");
        final String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);

        tcc.page().navigate(PwPageUrls.getSingleTopicConsumerGroupsPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultNavigateOpts());
        tcc.page().waitForURL(PwPageUrls.getSingleTopicConsumerGroupsPage(tcc, tcc.kafkaName(), topicId), PwUtils.getDefaultWaitForUrlOpts());

        PwUtils.waitForContainsText(tcc, ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS, consumerGroupName, true);

        LOGGER.info("Try click-through link and get redirected to a correct page");
        PwUtils.waitForLocatorAndClick(tcc.page()
            .locator(ConsumerGroupsPageSelectors.CGPS_TABLE_ITEMS)
            .locator("a", new Locator.LocatorOptions().setHasText(consumerGroupName)));

        tcc.page().waitForURL(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), consumerGroupEncodedName), PwUtils.getDefaultWaitForUrlOpts());
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, consumerGroupName, true);
        PwUtils.waitForContainsText(tcc, new CssBuilder(CssSelectors.PAGES_HEADER_BREADCRUMB_ITEMS).nth(4).build(), consumerGroupName, true);
    }

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
    public Stream<Arguments> resetOffsetAllTopicsScenarios() {
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
     * Provides a few parameterized scenarios for verifying consumer group offset reset functionality
     * across all topics and partitions using different reset types.
     * Same types as above with added specific offset reset types for single topic.
     */
    public Stream<Arguments> resetOffsetSpecificTopicScenarios() {
        final String earliestOffsetIndex = "0";
        // Use index to reset consumers to previous offset to read timestamp
        final String latestOffsetIndex = String.valueOf(MESSAGE_COUNT - 1);
        final String middleOffsetIndex = String.valueOf((int) Math.ceil(MESSAGE_COUNT / 2.0) - 1);

        return Stream.of(
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.EARLIEST, null, earliestOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.UNIX_EPOCH, latestOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DATE_TIME, ResetOffsetDateTimeType.ISO_8601, middleOffsetIndex),
            Arguments.of(MESSAGE_COUNT, ResetOffsetType.DELETE_COMMITED_OFFSETS, null, earliestOffsetIndex)
        );
    }

    /**
     * Executes parameterized tests for resetting Kafka consumer group offsets
     * across all topics and partitions using the UI.
     *
     * <p> For each scenario provided by {@link #resetOffsetAllTopicsScenarios()}:</p>
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
    @MethodSource("resetOffsetAllTopicsScenarios")
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

            ConsumerTestUtils.execDryRun(tcc, resetType, dateTimeType, resetValue);
            ConsumerTestUtils.execResetOffset(tcc, resetType, dateTimeType, resetValue);

            LOGGER.info("Verify expected consumer offset value");
            assertEquals(String.valueOf(expectedOffset),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));
        }
    }

    @TestBucket(RESET_OFFSET_BUCKET)
    @ParameterizedTest(name = "Type: {1} - DateTime: {2} - Offset: {3}")
    @MethodSource("resetOffsetSpecificTopicScenarios")
    void testResetConsumerOffsetSelectedTopic(int messageCount,
        ResetOffsetType resetType, ResetOffsetDateTimeType dateTimeType, String expectedOffset) {

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).get(0).getMetadata().getName();

        // Get topics for test from prepared scenario
        String kafkaTopicName = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), RESET_OFFSET_TOPIC_PREFIX)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList().getFirst();

        assertFalse(kafkaTopicName.isEmpty());

        tcc.page().navigate(PwPageUrls.getConsumerGroupsPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
        PwUtils.waitForElementEnabledState(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);

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
        // Dry-run
        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_SELECTED_TOPIC_RADIO);
        PwUtils.waitForLocatorAndFill(tcc, SingleConsumerGroupPageSelectors.SCGPS_SELECTED_TOPIC_INPUT, kafkaTopicName);
        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_TOPIC_NAME_DROPDOWN_BUTTON);
        ConsumerTestUtils.execDryRun(tcc, resetType, dateTimeType, resetValue);

        // Reset offset
        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_SELECTED_TOPIC_RADIO);
        PwUtils.waitForLocatorAndFill(tcc, SingleConsumerGroupPageSelectors.SCGPS_SELECTED_TOPIC_INPUT, kafkaTopicName);
        PwUtils.waitForLocatorAndClick(tcc, SingleConsumerGroupPageSelectors.SCGPS_RESET_PAGE_TOPIC_NAME_DROPDOWN_BUTTON);
        ConsumerTestUtils.execResetOffset(tcc, resetType, dateTimeType, resetValue);

        LOGGER.info("Verify expected consumer offset value");

        if (resetType.equals(ResetOffsetType.DELETE_COMMITED_OFFSETS)) {
            assertTrue(KafkaCmdUtils.verifyConsumerGroupHasDeletedOffsets(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));
        } else {
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
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
