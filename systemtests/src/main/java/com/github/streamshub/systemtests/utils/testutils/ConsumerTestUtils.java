package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.ResetOffsetDateTimeType;
import com.github.streamshub.systemtests.enums.ResetOffsetType;
import com.github.streamshub.systemtests.locators.ConsumerGroupsPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.microsoft.playwright.Locator;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ConsumerTestUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsumerTestUtils.class);

    public static void prepareConsumerOffsetScenario(TestCaseConfig tcc, String topicPrefix, String consumerGroupName, int topicCount, int partitions, int replicas, int minIsr) {
        LOGGER.info("Prepare consumer offset scenario by creating topic(s) and then producing and consuming messages");

        List<String> kafkaTopicNames = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), topicPrefix, topicCount, true, partitions, replicas, minIsr)
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
                .withConsumerGroup(consumerGroupName)
                .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
                .withUsername(tcc.kafkaUserName())
                .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
                .build();

            KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
            WaitUtils.waitForClientsSuccess(clients);
        }

        LOGGER.info("Reset consumer offset scenario ready");
    }

    public static void selectResetOffsetType(TestCaseConfig tcc, ResetOffsetType offsetType, String value) {
        LOGGER.debug("Select reset offset type");
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_DROPDOWN_BUTTON);

        switch (offsetType) {
            case EARLIEST:
                PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_EARLIEST_OFFSET);
                break;
            case LATEST:
                PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_LATEST_OFFSET);
                break;
            case DATE_TIME:
                Locator specificPartitionRadio = tcc.page().locator(ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_SELECTED_PARTITION_RADIO);
                if (specificPartitionRadio.isVisible() && specificPartitionRadio.getAttribute(Constants.CHECKED_ATTRIBUTE) != null) {
                    PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_SPECIFIC_PARTITION_SPECIFIC_DATETIME_OFFSET);
                } else {
                    PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_ALL_PARTITIONS_SPECIFIC_DATETIME_OFFSET);
                }
                break;
            case CUSTOM_OFFSET:
                PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET);
                PwUtils.waitForLocatorAndFill(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET_INPUT, value);
                break;
        }
    }

    public static void selectResetOffsetDatetimeTypeAndFill(TestCaseConfig tcc, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.debug("Select reset offset datetime type and fill value");
        switch (dateTimeType) {
            case UNIX_EPOCH:
                PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_EPOCH_FORMAT_RADIO);
                break;
            case ISO_8601:
                PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_ISO_FORMAT_RADIO);
                break;
        }
        PwUtils.waitForLocatorAndFill(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_SPECIFIC_DATETIME_INPUT, value);
    }

    public static void selectResetOffsetParameters(TestCaseConfig tcc, ResetOffsetType offsetType, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.debug("Select reset offset parameters");
        selectResetOffsetType(tcc, offsetType, value);

        if (offsetType.equals(ResetOffsetType.DATE_TIME)) {
            selectResetOffsetDatetimeTypeAndFill(tcc, dateTimeType, value);
        }

        if (offsetType.equals(ResetOffsetType.CUSTOM_OFFSET)) {
            PwUtils.waitForLocatorAndFill(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_CUSTOM_OFFSET_INPUT, value);
        }
    }

    public static void execDryRunAndReset(TestCaseConfig tcc, ResetOffsetType offsetType, ResetOffsetDateTimeType dateTimeType, String value) {
        LOGGER.debug("DryRun reset consumer group with offset {}", offsetType);
        selectResetOffsetParameters(tcc, offsetType, dateTimeType, value);

        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_DRY_RUN_BUTTON);
        PwUtils.waitForContainsAttribute(tcc, ConsumerGroupsPageSelectors.CGPS_DRY_RUN_COMMAND, "--to-" + offsetType.getCommand(), Constants.VALUE_ATTRIBUTE, true);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_BACK_TO_EDIT_OFFSET_BUTTON);

        // Reselect offset since UI resets previously selected
        LOGGER.debug("Execute reset consumer group offsets with offset {}", offsetType);
        selectResetOffsetParameters(tcc, offsetType, dateTimeType, value);
        PwUtils.waitForLocatorAndClick(tcc, ConsumerGroupsPageSelectors.CGPS_RESET_PAGE_OFFSET_RESET_BUTTON);
    }
}
