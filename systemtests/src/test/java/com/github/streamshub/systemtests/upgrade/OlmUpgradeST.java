package com.github.streamshub.systemtests.upgrade;


import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.console.OlmConfig;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.OLM_UPGRADE)
public class OlmUpgradeST extends AbstractUpgradeST {
    private static final Logger LOGGER = LogWrapper.getLogger(OlmUpgradeST.class);
    private TestCaseConfig tcc;
    private ConsoleOperatorSetup consoleOperatorSetup = new ConsoleOperatorSetup(Constants.CO_NAMESPACE);
    OlmVersionModificationData olmVersionData = new VersionModificationDataLoader(VersionModificationDataLoader.InstallType.OLM).getOlmUpgradeData();

    // Topics
    private static final int REPLICATED_TOPICS_COUNT = 7;
    private static final int UNMANAGED_REPLICATED_TOPICS_COUNT = 5;
    private static final int TOTAL_REPLICATED_TOPICS_COUNT = REPLICATED_TOPICS_COUNT + UNMANAGED_REPLICATED_TOPICS_COUNT;
    //
    private static final int UNDER_REPLICATED_TOPICS_COUNT = 3;
    private static final int UNAVAILABLE_TOPICS_COUNT = 2;
    private static final int TOTAL_TOPICS_COUNT = TOTAL_REPLICATED_TOPICS_COUNT + UNDER_REPLICATED_TOPICS_COUNT + UNAVAILABLE_TOPICS_COUNT;


    /**
     * Verifies that upgrading the Console Operator through OLM (Operator Lifecycle Manager)
     * successfully transitions to the new version without disrupting the Console instance or UI functionality.
     *
     * <p>The old and new channel/version values are loaded from
     * {@code src/test/resources/upgrade/OlmUpgrade.yaml}; at the time of writing this test upgrades
     * from OLM channel <b>0.12.x</b> (operator version <b>0.12.3</b>) to channel <b>0.13.x</b>
     * (operator version <b>0.13.0-snapshot</b>).</p>
     *
     * <p>This test performs the following steps:</p>
     * <ul>
     *   <li>Installs the Console Operator via OLM into the {@code co-namespace} namespace, subscribed to the <b>old OLM channel</b>.</li>
     *   <li>Deploys a default Console instance connected to an existing Kafka cluster.</li>
     *   <li>Validates that the deployed operator name reports the expected old version.</li>
     *   <li>Performs basic UI checks (overview and topics pages) confirming the 17 pre-created topics
     *       (12 fully replicated, 3 under-replicated, 2 unavailable) are displayed correctly, verifying the
     *       Console is functional before the upgrade.</li>
     *   <li>Upgrades the Console Operator subscription to the <b>new OLM channel</b>.</li>
     *   <li>Waits for the Console Operator deployment to roll and report the new operator version.</li>
     *   <li>Repeats the same overview/topics page checks to confirm the Console and its topic data
     *       remain correctly displayed after the upgrade.</li>
     * </ul>
     *
     * <p>This ensures that an OLM-based upgrade of the Console Operator completes cleanly and that the
     * Console UI keeps reflecting the underlying Kafka topic state both before and after the upgrade.</p>
     */
    @Test
    void testUpgradeOlmOperator() {
        // Setup
        LOGGER.info("Setting up Console Operator subscription on old OLM channel '{}' (expected version '{}')", olmVersionData.getOldOlmChannel(), olmVersionData.getOldOperatorVersion());
        OlmConfig olmConfig = new OlmConfig(Constants.CO_NAMESPACE);

        olmConfig.setChannelName(olmVersionData.getOldOlmChannel());
        consoleOperatorSetup.setInstallConfig(olmConfig);
        LOGGER.info("Installing Console Operator via OLM into namespace '{}'", Constants.CO_NAMESPACE);
        consoleOperatorSetup.install(false);

        LOGGER.info("Setting up Console instance '{}' in namespace '{}' for Kafka cluster '{}'", tcc.consoleInstanceName(), tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());

        LOGGER.info("Verifying Console Operator deployment reports the expected old version '{}'", olmVersionData.getOldOperatorVersion());

        String currentOperatorVersion = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, Constants.CO_NAMESPACE, Environment.CONSOLE_OLM_PACKAGE_NAME)
            .getFirst().getMetadata().getName().replace(Environment.CONSOLE_OLM_PACKAGE_NAME + "-v", "");

        LOGGER.debug("Detected deployed Console Operator version: '{}'", currentOperatorVersion);
        assertEquals(olmVersionData.getOldOperatorVersion(), currentOperatorVersion);

        LOGGER.info("Performing basic UI checks before upgrade to confirm Console displays all {} pre-created topics correctly", TOTAL_TOPICS_COUNT);
        PwUtils.login(tcc);

        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        // Upgrade
        LOGGER.info("Triggering Console Operator OLM upgrade: switching subscription from channel '{}' to channel '{}'", olmVersionData.getOldOlmChannel(), olmVersionData.getNewOlmChannel());
        olmConfig.setChannelName(olmVersionData.getNewOlmChannel());
        consoleOperatorSetup.setInstallConfig(olmConfig);
        consoleOperatorSetup.install(false);

        LOGGER.info("Waiting for Console Operator deployment to roll and report the new version '{}'", olmVersionData.getNewOperatorVersion());
        WaitUtils.waitForConsoleDeploymentToReachVersion(Constants.CO_NAMESPACE, Environment.CONSOLE_OLM_PACKAGE_NAME, olmVersionData.getNewOperatorVersion(),
            deployment -> deployment.getMetadata().getName().replace(Environment.CONSOLE_OLM_PACKAGE_NAME + "-v", ""));

        LOGGER.info("Performing basic UI checks after upgrade to confirm all {} topics survived and Console UI is still functional", TOTAL_TOPICS_COUNT);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());

        // Setup topics
        final int scaledUpBrokerReplicas = Constants.REGULAR_BROKER_REPLICAS + 1;
        KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, REPLICATED_TOPICS_COUNT, 1, 1, 1);
        KafkaTopicUtils.setupUnmanagedTopicsAndReturnNames(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX, UNMANAGED_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1);
        KafkaTopicUtils.setupUnderReplicatedTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNDER_REPLICATED_TOPICS_PREFIX, UNDER_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas);
        KafkaTopicUtils.setupUnavailableTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNAVAILABLE_TOPICS_PREFIX, UNAVAILABLE_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }

}
