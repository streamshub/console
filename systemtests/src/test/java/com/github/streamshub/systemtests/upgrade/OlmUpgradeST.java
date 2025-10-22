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
import com.github.streamshub.systemtests.utils.resourceutils.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import io.fabric8.kubernetes.api.model.Pod;
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
    // Note for pagination scenario it's best to have total of 150 topics
    private static final int REPLICATED_TOPICS_COUNT = 7;
    private static final int UNMANAGED_REPLICATED_TOPICS_COUNT = 5;
    private static final int TOTAL_REPLICATED_TOPICS_COUNT = REPLICATED_TOPICS_COUNT + UNMANAGED_REPLICATED_TOPICS_COUNT;
    //
    private static final int UNDER_REPLICATED_TOPICS_COUNT = 3;
    private static final int UNAVAILABLE_TOPICS_COUNT = 2;
    private static final int TOTAL_TOPICS_COUNT = TOTAL_REPLICATED_TOPICS_COUNT + UNDER_REPLICATED_TOPICS_COUNT + UNAVAILABLE_TOPICS_COUNT;

    @Test
    void testUpgradeOlmOperator() {
        // Setup
        LOGGER.info("Setup console operator with specified channel: {}", olmVersionData.getOldOlmChannel());
        OlmConfig olmConfig = new OlmConfig(Constants.CO_NAMESPACE);

        olmConfig.setChannelName(olmVersionData.getOldOlmChannel());
        consoleOperatorSetup.setInstallConfig(olmConfig);
        consoleOperatorSetup.install();

        LOGGER.info("Setup console instance");
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));

        LOGGER.info("Verify console operator version");

        String currentOperatorVersion = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, Constants.CO_NAMESPACE, Environment.CONSOLE_OLM_PACKAGE_NAME)
            .get(0)
            .getMetadata()
            .getName()
            .replace(Environment.CONSOLE_OLM_PACKAGE_NAME + "-v", "");

        assertEquals(olmVersionData.getOldOperatorVersion(), currentOperatorVersion);
        // Take snapshot for future assertion
        String oldInstanceSnapshot = ResourceUtils.getKubeResource(Deployment.class, tcc.namespace(), ConsoleUtils.getConsoleDeploymentName(tcc.consoleInstanceName()))
            .getMetadata().getUid();

        LOGGER.info("Perform basic checks to validate UI is working");
        PwUtils.login(tcc);

        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        // Upgrade
        LOGGER.info("Perform console operator upgrade to channel {}", olmVersionData.getNewOlmChannel());
        olmConfig.setChannelName(olmVersionData.getNewOlmChannel());
        consoleOperatorSetup.setInstallConfig(olmConfig);
        consoleOperatorSetup.install();

        String consoleInstancePodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), ConsoleUtils.getConsoleDeploymentName(tcc.consoleInstanceName()))
            .get(0)
            .getMetadata()
            .getName();

        WaitUtils.waitForPodRoll(tcc.namespace(), consoleInstancePodName, oldInstanceSnapshot);

        LOGGER.info("Verify upgraded console operator version is: {}", olmVersionData.getNewOperatorVersion());

        currentOperatorVersion = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, Constants.CO_NAMESPACE, Environment.CONSOLE_OLM_PACKAGE_NAME)
            .get(0)
            .getMetadata()
            .getName()
            .replace(Environment.CONSOLE_OLM_PACKAGE_NAME + "-v", "");

        assertEquals(olmVersionData.getNewOperatorVersion(), currentOperatorVersion);

        LOGGER.info("Perform basic checks after upgrade to validate UI is still working");
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
        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, REPLICATED_TOPICS_COUNT, true, 1, 1, 1);
        KafkaTopicUtils.setupUnmanagedTopicsAndReturnNames(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX, UNMANAGED_REPLICATED_TOPICS_COUNT, tcc.messageCount(), 1, 1, 1);
        KafkaTopicUtils.setupUnderReplicatedTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNDER_REPLICATED_TOPICS_PREFIX, UNDER_REPLICATED_TOPICS_COUNT, tcc.messageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas);
        KafkaTopicUtils.setupUnavailableTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()), Constants.UNAVAILABLE_TOPICS_PREFIX, UNAVAILABLE_TOPICS_COUNT, tcc.messageCount(), 1, 1, 1);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }

}
