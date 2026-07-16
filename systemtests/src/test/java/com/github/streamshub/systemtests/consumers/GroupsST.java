package com.github.streamshub.systemtests.consumers;

import com.github.streamshub.console.support.Identifiers;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.locators.GroupsPageSelectors;
import com.github.streamshub.systemtests.locators.SingleGroupPageSelectors;
import com.github.streamshub.systemtests.locators.TopicsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaCmdUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.testutils.GroupsTestUtils;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.REGRESSION)
public class GroupsST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(GroupsST.class);
    private static final String RESET_OFFSET_BUCKET = "ResetOffset";

    // Shared
    protected TestCaseConfig tcc;

    // ResetOffset TestBucket
    private static final String RESET_OFFSET_TOPIC_PREFIX = "rst-all-topics-var-offset";
    private static final int RESET_OFFSET_TOPIC_COUNT = 2;
    private static final String RESET_OFFSET_CONSUMER_GROUP_NAME = "reset-offset-consumer-group";

    /**
     * Tests that consumer group names containing unusual or special characters are handled correctly
     * throughout the Groups UI.
     *
     * <p>For each of the following consumer group name variations, the test creates a dedicated Kafka topic,
     * produces {@link Constants#MESSAGE_COUNT_HIGH} messages, and starts a producer/consumer pair using that
     * group name:</p>
     * <ul>
     *   <li>Special characters (e.g. {@code group$$$$$%^^&*}).</li>
     *   <li>Semicolon-separated names (e.g. {@code group;part;;1}).</li>
     *   <li>Dot-separated names (e.g. {@code group.1.3.5}).</li>
     *   <li>Colon-separated names (e.g. {@code group:12::3:}).</li>
     *   <li>Names with quote/symbol characters (e.g. {@code group'@!"#?}).</li>
     *   <li>Names with underscores, hyphens, slashes, equals signs, spaces, pipe symbols, and tildes.</li>
     *   <li>A very long consumer group name (over 90 characters).</li>
     * </ul>
     *
     * <p>For each group name variation, the test verifies:</p>
     * <ul>
     *   <li>The group is found and displayed correctly when filtered by name on the Groups page.</li>
     *   <li>Navigating directly to the single-group page (using the URL-encoded group name) shows the
     *       correct group name in the page header and breadcrumb.</li>
     *   <li>Clicking through from the filtered Groups page row leads to the correct single-group page.</li>
     *   <li>The group is listed on its topic's Groups tab, and clicking it navigates to the correct
     *       single-group page.</li>
     * </ul>
     *
     * <p>This ensures that consumer group names with unusual characters are correctly encoded, displayed,
     * filtered, and linked across the Groups, single-group, and topic Groups-tab pages.</p>
     */
    @Test
    void testVariableConsumerGroupNames() {
        List<Map.Entry<String, String>> scenarios = List.of(
            Map.entry("Special chars", "group$$$$$%^^&*"),
            Map.entry("Semicolon separated", "group;part;;1"),
            Map.entry("Dot separated", "group.1.3.5"),
            Map.entry("Colon separated", "group:12::3:"),
            Map.entry("Symbols", "group'@!\"#?"),
            Map.entry("Underscores", "group_with__underscores_"),
            Map.entry("Hyphenated", "group-hyphen--name-"),
            Map.entry("With slash", "group/with//slash/"),
            Map.entry("Equals sign", "group=equals==two"),
            Map.entry("Comma separated", "group,comma,separated,,"),
            Map.entry("With spaces", "group space allowed"),
            Map.entry("Pipe symbol", "group|pipe||secondpipe"),
            Map.entry("Tilde", "group~tilde~~name"),
            Map.entry("Very long name", "consumer_group_with_really_really_really_long_name_1234567890-1234567890-1234567890")
        );

        LOGGER.info("Testing {} consumer group name variations", scenarios.size());
        for (var scenario : scenarios) {
            String displayName = scenario.getKey();
            String consumerGroupName = scenario.getValue();
            String topicName = "topic-" + Utils.hashStub(displayName);

            LOGGER.info("Testing consumer group name variation '{}': '{}'", displayName, consumerGroupName);

            // Must be done due to k8s ENV parsing results
            // https://jellepelgrims.com/posts/dollar_signs
            String k8sFriendlyName = consumerGroupName.replace("$", "$$");
            LOGGER.debug("Using k8s-friendly consumer group name '{}' for topic '{}'", k8sFriendlyName, topicName);

            LOGGER.info("Create KafkaTopic CR for '{}'", displayName);
            KubeResourceManager.get().createResourceWithWait(
                KafkaTopicUtils.defaultTopic(tcc.namespace(), tcc.kafkaName(), topicName, 1, 1, 1).build());

            LOGGER.info("Produce and consume messages for '{}'", displayName);
            KafkaClients clients = new KafkaClientsBuilder()
                .withNamespaceName(tcc.namespace())
                .withTopicName(topicName)
                .withMessageCount(Constants.MESSAGE_COUNT)
                .withDelayMs(0)
                .withProducerName(KafkaNamingUtils.producerName(topicName))
                .withConsumerName(KafkaNamingUtils.consumerName(topicName))
                .withConsumerGroup(k8sFriendlyName)
                .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
                .withUsername(tcc.kafkaUserName())
                .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
                .build();

            KubeResourceManager.get().createResourceAsyncWait(clients.producer(), clients.consumer());

            // Job readiness is a no-op in fabric8 (Job is not a "Readiness-applicable" kind), so the wait above only
            // confirms the Job was accepted by the API - it does not wait for the pod to be scheduled, its image
            // pulled, or the consumer to actually join the group. Wait for the consumer pod itself to be running so
            // that pod startup latency (which varies a lot between clusters) doesn't eat into the UI's retry budget.
            LOGGER.info("Wait for consumer '{}' to be running before checking the Groups UI", clients.getConsumerName());
            WaitUtils.waitForPodsReady(tcc.namespace(),
                new LabelSelectorBuilder().withMatchLabels(Labels.getClientsLabels(clients.getConsumerName())).build(),
                1, true, () -> { });

            LOGGER.info("Verifying group name '{}' is correctly encoded, displayed and linked across pages", consumerGroupName);
            String consumerGroupEncodedName = Identifiers.encode(consumerGroupName);
            LOGGER.debug("URL-encoded consumer group name for '{}': '{}'", consumerGroupName, consumerGroupEncodedName);

            // Verify row on groups page
            LOGGER.info("Verify group '{}' ('{}') is present in groups table", displayName, consumerGroupName);
            PwUtils.navigate(tcc, PwPageUrls.getGroupsPage(tcc, tcc.kafkaName()));
            PwUtils.waitForContainsText(tcc, GroupsPageSelectors.GPS_HEADER_TITLE, "Groups", true);
            PwUtils.fill(tcc, GroupsPageSelectors.GPS_GROUP_NAME_INPUT, consumerGroupName);
            PwUtils.waitForContainsText(tcc, GroupsPageSelectors.GPS_RESULT_FIRST_NAME, consumerGroupName, false);

            // Verify single group page
            LOGGER.info("Navigate to single consumer group page for '{}' ('{}')", displayName, consumerGroupName);
            PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), consumerGroupEncodedName));
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, consumerGroupName, true);
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_HEADER_BREADCRUMB_GROUP_NAME, consumerGroupName, true);

            // Click through from groups page
            LOGGER.info("Navigate back to groups page and test click-through for '{}' ('{}')", displayName, consumerGroupName);
            PwUtils.navigate(tcc, PwPageUrls.getGroupsPage(tcc, tcc.kafkaName()));
            PwUtils.waitForContainsText(tcc, GroupsPageSelectors.GPS_HEADER_TITLE, "Groups", true);
            PwUtils.fill(tcc, GroupsPageSelectors.GPS_GROUP_NAME_INPUT, consumerGroupName);
            PwUtils.waitForContainsText(tcc, GroupsPageSelectors.GPS_RESULT_FIRST_NAME, consumerGroupName, false);
            tcc.page().click(GroupsPageSelectors.GPS_RESULT_FIRST_NAME);

            PwUtils.waitForUrl(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), consumerGroupEncodedName), true);
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, consumerGroupName, true);
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_HEADER_BREADCRUMB_GROUP_NAME, consumerGroupName, true);

            // Verify group on topic page
            LOGGER.info("Check topic page if consumer group '{}' ('{}') is present", displayName, consumerGroupName);
            final String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
            LOGGER.debug("Resolved topic ID '{}' for topic '{}'", topicId, topicName);

            PwUtils.navigate(tcc, PwPageUrls.getSingleTopicGroupsPage(tcc, tcc.kafkaName(), topicId), true, true);

            // Topic page is focused on one topic so filter by text is safer than row index
            PwUtils.waitForLocatorAndClick(tcc, TopicsPageSelectors.TPS_GROUPS_TABLE_FIRST_GROUP);

            PwUtils.waitForUrl(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), consumerGroupEncodedName), true);
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, consumerGroupName, true);
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_HEADER_BREADCRUMB_GROUP_NAME, consumerGroupName, true);

            // Confirm the producer/consumer didn't error during the scenario, and clean up the Jobs before the next one
            WaitUtils.waitForClientsSuccess(clients);
        }
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
        final String latestOffsetIndex = String.valueOf(Constants.MESSAGE_COUNT_HIGH - 1);
        final String middleOffsetIndex = String.valueOf((int) Math.ceil(Constants.MESSAGE_COUNT_HIGH / 2.0) - 1);

        return Stream.of(
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.EARLIEST, null, earliestOffsetIndex),
            // Only one that uses `--to-latest` which sets the index to nth+1 for consuming the next message
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.LATEST, null, String.valueOf(Constants.MESSAGE_COUNT_HIGH)),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_UNIX, ResetOffsetDateTimeType.UNIX_EPOCH, earliestOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_UNIX, ResetOffsetDateTimeType.UNIX_EPOCH, latestOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_UNIX, ResetOffsetDateTimeType.UNIX_EPOCH, middleOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_ISO, ResetOffsetDateTimeType.ISO_8601, earliestOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_ISO, ResetOffsetDateTimeType.ISO_8601, latestOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_ISO, ResetOffsetDateTimeType.ISO_8601, middleOffsetIndex)
        );
    }

    /**
     * Executes parameterized tests for resetting Kafka consumer group offsets
     * across all topics and partitions using the UI.
     *
     * <p> For each scenario provided by {@link #resetOffsetAllTopicsScenarios()}:</p>
     * <ul>
     *   <li>Navigates to the Groups page for the test consumer group.</li>
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

        LOGGER.info("Resetting offset across all topics for group '{}': type={}, dateTimeType={}, expectedOffset={}",
            RESET_OFFSET_CONSUMER_GROUP_NAME, resetType, dateTimeType, expectedOffset);

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).getFirst().getMetadata().getName();

        // Get topics for test from prepared scenario
        List<String> kafkaTopicNames = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), RESET_OFFSET_TOPIC_PREFIX)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList();

        assertFalse(kafkaTopicNames.isEmpty());
        LOGGER.debug("Found {} topic(s) with prefix '{}' for offset reset: {}", kafkaTopicNames.size(), RESET_OFFSET_TOPIC_PREFIX, kafkaTopicNames);

        PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
        PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
        PwUtils.waitForElementEnabledState(tcc, SingleGroupPageSelectors.SGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true);

        // Look at the offset in UI
        for (String kafkaTopicName : kafkaTopicNames) {
            LOGGER.info("Verifying offset reset behavior for topic '{}'", kafkaTopicName);
            LOGGER.info("Setting baseline consumer offset {} for topic '{}', group '{}'", messageCount, kafkaTopicName, RESET_OFFSET_CONSUMER_GROUP_NAME);
            KafkaCmdUtils.setConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName, String.valueOf(messageCount),
                KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT));

            assertEquals(String.valueOf(messageCount),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));

            String resetValue = expectedOffset;
            // To determine offset timestamp from offsetNumber
            if (dateTimeType != null) {
                LOGGER.debug("Resolving reset timestamp for topic '{}' from expected offset {} using dateTimeType={}", kafkaTopicName, expectedOffset, dateTimeType);
                if (dateTimeType.equals(ResetOffsetDateTimeType.UNIX_EPOCH)) {
                    resetValue = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                         KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), expectedOffset, 0, 1);
                    LOGGER.debug("Resolved UNIX epoch reset value '{}' for topic '{}'", resetValue, kafkaTopicName);
                } else {
                    String epoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                        KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), expectedOffset, 0, 1);
                    resetValue = Instant.ofEpochMilli(Long.parseLong(epoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                    LOGGER.debug("Resolved ISO-8601 reset value '{}' (from epoch '{}') for topic '{}'", resetValue, epoch, kafkaTopicName);
                }
            }

            PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
            PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_CONSUMER_OFFSET_BUTTON);
            LOGGER.info("Performing dry-run offset reset for topic '{}' with value '{}'", kafkaTopicName, resetValue);
            GroupsTestUtils.execDryRun(tcc, resetType, dateTimeType, resetValue);

            PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
            PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
            PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_CONSUMER_OFFSET_BUTTON);
            LOGGER.info("Performing offset reset for topic '{}' with value '{}'", kafkaTopicName, resetValue);
            GroupsTestUtils.execResetOffset(tcc, resetType, dateTimeType, resetValue);

            Utils.sleepWait(TimeConstants.ACTION_WAIT_MEDIUM);

            LOGGER.info("Verifying consumer offset for topic '{}' matches expected value '{}'", kafkaTopicName, expectedOffset);
            assertEquals(String.valueOf(expectedOffset),
                KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));
        }
    }


    /**
     * Provides a few parameterized scenarios for verifying consumer group offset reset functionality
     * across all topics and partitions using different reset types.
     * Same types as above with added specific offset reset types for single topic.
     */
    public Stream<Arguments> resetOffsetSpecificTopicScenarios() {
        final String earliestOffsetIndex = "0";
        // Use index to reset consumers to previous offset to read timestamp
        final String latestOffsetIndex = String.valueOf(Constants.MESSAGE_COUNT_HIGH - 1);
        final String middleOffsetIndex = String.valueOf((int) Math.ceil(Constants.MESSAGE_COUNT_HIGH / 2.0) - 1);

        return Stream.of(
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.EARLIEST, null, earliestOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_UNIX, ResetOffsetDateTimeType.UNIX_EPOCH, latestOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DATE_TIME_ISO, ResetOffsetDateTimeType.ISO_8601, middleOffsetIndex),
            Arguments.of(Constants.MESSAGE_COUNT_HIGH, ResetOffsetType.DELETE_COMMITED_OFFSETS, null, earliestOffsetIndex)
        );
    }
    /**
     * Executes parameterized tests for resetting a Kafka consumer group's offset on a single, explicitly
     * selected topic using the UI, as opposed to resetting across all topics at once.
     *
     * <p>For each scenario provided by {@link #resetOffsetSpecificTopicScenarios()}, the test targets the
     * first topic created by {@link #setupConsumerGroupResetOffset()} and:</p>
     * <ul>
     *   <li>Sets the consumer group's offset for that topic to the total message count via the Kafka CLI
     *       and verifies it.</li>
     *   <li>If a DATE_TIME reset is used, calculates the appropriate timestamp based on the expected
     *       offset and the selected date/time format (UNIX_EPOCH or ISO-8601).</li>
     *   <li>Opens the offset reset dialog from the single-group page and explicitly selects the target
     *       topic from the topic dropdown before performing a dry-run reset.</li>
     *   <li>Repeats the topic selection and performs the actual reset via the UI.</li>
     *   <li>Verifies the outcome: for {@code DELETE_COMMITED_OFFSETS} it asserts the consumer group's
     *       committed offsets were deleted for that topic; for all other reset types it asserts the
     *       consumer group offset matches the expected value.</li>
     * </ul>
     *
     * <p>Covered reset types are EARLIEST, DATE_TIME (UNIX epoch and ISO-8601 formats), and
     * DELETE_COMMITED_OFFSETS.</p>
     *
     * <p>This ensures that offset resets scoped to a single topic behave correctly and independently
     * from resets applied across all topics, and that deleting committed offsets works as expected.</p>
     *
     * @param messageCount the total number of messages produced to the topic before the reset
     * @param resetType the type of offset reset to perform (EARLIEST, DATE_TIME, or DELETE_COMMITED_OFFSETS)
     * @param dateTimeType the date/time format used for DATE_TIME resets (UNIX_EPOCH or ISO-8601), null otherwise
     * @param expectedOffset the expected offset value after the reset operation (not checked for DELETE_COMMITED_OFFSETS)
     */
    @TestBucket(RESET_OFFSET_BUCKET)
    @ParameterizedTest(name = "Type: {1} - DateTime: {2} - Offset: {3}")
    @MethodSource("resetOffsetSpecificTopicScenarios")
    void testResetConsumerOffsetSpecificTopic(int messageCount,
        ResetOffsetType resetType, ResetOffsetDateTimeType dateTimeType, String expectedOffset) {

        LOGGER.info("Resetting offset for a single topic, group '{}': type={}, dateTimeType={}, expectedOffset={}",
            RESET_OFFSET_CONSUMER_GROUP_NAME, resetType, dateTimeType, expectedOffset);

        final String brokerPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), KafkaNamingUtils.brokerPodNamePrefix(tcc.kafkaName())).getFirst().getMetadata().getName();

        // Get topics for test from prepared scenario
        String kafkaTopicName = ResourceUtils.listKubeResourcesByPrefix(KafkaTopic.class, tcc.namespace(), RESET_OFFSET_TOPIC_PREFIX)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList().getFirst();

        assertFalse(kafkaTopicName.isEmpty());
        LOGGER.debug("Selected target topic '{}' for single-topic offset reset", kafkaTopicName);

        PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
        PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
        PwUtils.waitForElementEnabledState(tcc, SingleGroupPageSelectors.SGPS_RESET_CONSUMER_OFFSET_BUTTON, true, true);

        LOGGER.info("Setting baseline consumer offset {} for topic '{}', group '{}'", messageCount, kafkaTopicName, RESET_OFFSET_CONSUMER_GROUP_NAME);
        KafkaCmdUtils.setConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName, String.valueOf(messageCount),
            KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT));

        assertEquals(String.valueOf(messageCount),
            KafkaCmdUtils.getConsumerGroupOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));

        String resetValue = expectedOffset;
        // To determine offset timestamp from offsetNumber
        if (dateTimeType != null) {
            LOGGER.debug("Resolving reset timestamp for topic '{}' from expected offset {} using dateTimeType={}", kafkaTopicName, expectedOffset, dateTimeType);
            if (dateTimeType.equals(ResetOffsetDateTimeType.UNIX_EPOCH)) {
                resetValue = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                     KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), expectedOffset, 0, 1);
                LOGGER.debug("Resolved UNIX epoch reset value '{}' for topic '{}'", resetValue, kafkaTopicName);
            } else {
                String epoch = KafkaCmdUtils.getConsumerOffsetTimestampFromOffset(tcc.namespace(), tcc.kafkaName(), brokerPodName, kafkaTopicName,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT), expectedOffset, 0, 1);
                resetValue = Instant.ofEpochMilli(Long.parseLong(epoch)).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                LOGGER.debug("Resolved ISO-8601 reset value '{}' (from epoch '{}') for topic '{}'", resetValue, epoch, kafkaTopicName);
            }
        }


        // Dry-run
        PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
        PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_CONSUMER_OFFSET_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_SELECTED_TOPIC_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndFill(tcc, SingleGroupPageSelectors.SGPS_SELECTED_TOPIC_DROPDOWN_SEARCH_INPUT, kafkaTopicName);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_TOPIC_NAME_DROPDOWN_RESULT);
        LOGGER.info("Performing dry-run offset reset for topic '{}' with value '{}'", kafkaTopicName, resetValue);
        GroupsTestUtils.execDryRun(tcc, resetType, dateTimeType, resetValue);

        // Reset offset
        PwUtils.navigate(tcc, PwPageUrls.getGroupsMembersPage(tcc, tcc.kafkaName(), Identifiers.encode(RESET_OFFSET_CONSUMER_GROUP_NAME)));
        PwUtils.waitForContainsText(tcc, SingleGroupPageSelectors.SGPS_PAGE_HEADER_NAME, RESET_OFFSET_CONSUMER_GROUP_NAME, true);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_CONSUMER_OFFSET_BUTTON);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_SELECTED_TOPIC_DROPDOWN_BUTTON);
        PwUtils.waitForLocatorAndFill(tcc, SingleGroupPageSelectors.SGPS_SELECTED_TOPIC_DROPDOWN_SEARCH_INPUT, kafkaTopicName);
        PwUtils.waitForLocatorAndClick(tcc, SingleGroupPageSelectors.SGPS_RESET_PAGE_TOPIC_NAME_DROPDOWN_RESULT);
        LOGGER.info("Performing offset reset for topic '{}' with value '{}'", kafkaTopicName, resetValue);
        GroupsTestUtils.execResetOffset(tcc, resetType, dateTimeType, resetValue);

        LOGGER.info("Verifying outcome of '{}' reset for topic '{}'", resetType, kafkaTopicName);

        if (resetType.equals(ResetOffsetType.DELETE_COMMITED_OFFSETS)) {
            LOGGER.info("Expecting committed offsets to be deleted for group '{}', topic '{}'", RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName);
            assertTrue(KafkaCmdUtils.verifyConsumerGroupHasDeletedOffsets(tcc.namespace(), tcc.kafkaName(), brokerPodName, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName,
                KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)));
        } else {
            LOGGER.info("Expecting consumer offset '{}' for group '{}', topic '{}'", expectedOffset, RESET_OFFSET_CONSUMER_GROUP_NAME, kafkaTopicName);
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
     *   <li>Creates {@value #RESET_OFFSET_TOPIC_COUNT} Kafka topics using the
     *       {@value #RESET_OFFSET_TOPIC_PREFIX} prefix (reusing existing ones if already present).</li>
     *   <li>For each topic, creates a Kafka producer and consumer using the
     *       {@link #RESET_OFFSET_CONSUMER_GROUP_NAME} consumer group.</li>
     *   <li>Produces and consumes {@link Constants#MESSAGE_COUNT_HIGH} messages per topic
     *       to establish the initial consumer offsets.</li>
     *   <li>Waits for all client operations to complete successfully, ensuring offsets
     *       are properly initialized for testing offset reset scenarios.</li>
     * </ul>
     *
     * <p>This setup ensures that the consumer offset reset tests have a consistent
     * initial state across all topics and partitions.</p>
     */
    @SetupTestBucket(RESET_OFFSET_BUCKET)
    public void setupConsumerGroupResetOffset() {
        LOGGER.info("Preparing reset-offset test bucket: creating {} topic(s) with prefix '{}' for group '{}'",
            RESET_OFFSET_TOPIC_COUNT, RESET_OFFSET_TOPIC_PREFIX, RESET_OFFSET_CONSUMER_GROUP_NAME);

        List<String> kafkaTopicNames = KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), RESET_OFFSET_TOPIC_PREFIX, RESET_OFFSET_TOPIC_COUNT, 1, 1, 1)
            .stream()
            .map(kt -> kt.getMetadata().getName())
            .toList();

        LOGGER.debug("Reset-offset test bucket will use topics: {}", kafkaTopicNames);

        List<KafkaClients> clientsList = kafkaTopicNames.stream()
            .map(kafkaTopicName -> new KafkaClientsBuilder()
                .withNamespaceName(tcc.namespace())
                .withTopicName(kafkaTopicName)
                .withMessageCount(Constants.MESSAGE_COUNT_HIGH)
                .withDelayMs(0)
                .withProducerName(KafkaNamingUtils.producerName(kafkaTopicName))
                .withConsumerName(KafkaNamingUtils.consumerName(kafkaTopicName))
                .withConsumerGroup(RESET_OFFSET_CONSUMER_GROUP_NAME)
                .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
                .withUsername(tcc.kafkaUserName())
                .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
                .build())
            .toList();

        Job[] clientJobs = clientsList.stream()
            .flatMap(clients -> Stream.of(clients.producer(), clients.consumer()))
            .toArray(Job[]::new);
        LOGGER.info("Producing and consuming {} messages per topic across {} topic(s) for group '{}'",
            Constants.MESSAGE_COUNT_HIGH, kafkaTopicNames.size(), RESET_OFFSET_CONSUMER_GROUP_NAME);
        KubeResourceManager.get().createResourceAsyncWait(clientJobs);
        WaitUtils.waitForClientsSuccess(clientsList);

        LOGGER.info("Reset-offset test bucket ready: baseline offsets initialized for topics {}", kafkaTopicNames);
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
