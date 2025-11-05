package com.github.streamshub.systemtests.upgrade;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.console.YamlConfig;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.testchecks.TopicChecks;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.YAML_UPGRADE)
public class YamlUpgradeST extends AbstractUpgradeST {

    private static final Logger LOGGER = LogWrapper.getLogger(YamlUpgradeST.class);
    private TestCaseConfig tcc;
    private ConsoleOperatorSetup consoleOperatorSetup = new ConsoleOperatorSetup(Constants.CO_NAMESPACE);
    YamlVersionModificationData yamlVersionData = new VersionModificationDataLoader(VersionModificationDataLoader.InstallType.YAML).getYamlUpgradeData();

    // Topics
    private static final int REPLICATED_TOPICS_COUNT = 7;
    private static final int UNMANAGED_REPLICATED_TOPICS_COUNT = 5;
    private static final int TOTAL_REPLICATED_TOPICS_COUNT = REPLICATED_TOPICS_COUNT + UNMANAGED_REPLICATED_TOPICS_COUNT;
    //
    private static final int UNDER_REPLICATED_TOPICS_COUNT = 3;
    private static final int UNAVAILABLE_TOPICS_COUNT = 2;
    private static final int TOTAL_TOPICS_COUNT = TOTAL_REPLICATED_TOPICS_COUNT + UNDER_REPLICATED_TOPICS_COUNT + UNAVAILABLE_TOPICS_COUNT;

    /**
     * Verifies that upgrading the Console Operator deployed from YAML manifests
     * correctly transitions to the new version while preserving the existing Console instance
     * and maintaining full UI functionality.
     *
     * <p>This test performs the following steps:</p>
     * <ul>
     *   <li>Installs the Console Operator from the <b>old YAML manifest URL</b>.</li>
     *   <li>Deploys a default Console instance linked to a Kafka cluster.</li>
     *   <li>Validates that the deployed operator version matches the expected old version.</li>
     *   <li>Captures the Console deployment UID as a snapshot for later comparison.</li>
     *   <li>Performs UI verification to ensure the Console is functioning correctly before the upgrade.</li>
     *   <li>Upgrades the Console Operator using the <b>new YAML manifest URL</b> without deleting existing resources.</li>
     *   <li>Waits for the Console instance to roll and redeploy under the new operator version.</li>
     *   <li>Confirms that the operator has upgraded to the expected new version.</li>
     *   <li>Repeats UI checks to validate that the Console remains fully operational post-upgrade.</li>
     * </ul>
     */
    @Test
    void testUpgragdeYamlOperator() {
        // Setup
        LOGGER.info("Setup console operator from specified URL: {}", yamlVersionData.getOldOperatorCrdsUrl());
        YamlConfig yamlConfig = new YamlConfig(Constants.CO_NAMESPACE, yamlVersionData.getOldOperatorCrdsUrl());

        consoleOperatorSetup.setInstallConfig(yamlConfig);
        consoleOperatorSetup.install();

        LOGGER.info("Setup console instance");
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));

        LOGGER.info("Verify console operator version");
        String currentOperatorVersion = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, Constants.CO_NAMESPACE, Environment.CONSOLE_DEPLOYMENT_NAME)
            .get(0)
            .getMetadata()
            .getLabels()
            .get("app.kubernetes.io/version");

        assertEquals(yamlVersionData.getOldOperatorVersion(), currentOperatorVersion);
        // Take snapshot for future assertion
        String oldInstanceSnapshot = ResourceUtils.getKubeResource(Deployment.class, tcc.namespace(), ConsoleUtils.getConsoleDeploymentName(tcc.consoleInstanceName()))
            .getMetadata().getUid();

        LOGGER.info("Perform basic checks to validate UI is working");
        PwUtils.login(tcc);

        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        // Upgrade
        LOGGER.info("Perform console operator upgrade from URL: {}", yamlVersionData.getNewOperatorCrdsUrl());
        yamlConfig = new YamlConfig(Constants.CO_NAMESPACE, yamlVersionData.getNewOperatorCrdsUrl());
        consoleOperatorSetup.setInstallConfig(yamlConfig);
        // Do not delete resources to verify instance upgrades
        consoleOperatorSetup.install(false);

        LOGGER.info("Wait for console instance to roll");
        WaitUtils.waitForConsoleInstanceToRoll(tcc.namespace(), ConsoleUtils.getConsoleDeploymentName(tcc.consoleInstanceName()), oldInstanceSnapshot);

        LOGGER.info("Verify upgraded console operator version is: {}", yamlVersionData.getNewOperatorVersion());

        currentOperatorVersion = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, Constants.CO_NAMESPACE, Environment.CONSOLE_DEPLOYMENT_NAME)
            .get(0)
            .getMetadata()
            .getLabels()
            .get("app.kubernetes.io/version");

        assertEquals(yamlVersionData.getNewOperatorVersion(), currentOperatorVersion);

        LOGGER.info("Perform basic checks after upgrade to validate UI is still working");
        PwUtils.login(tcc);
        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
    }

    @AfterEach
    void testCaseTeardown() {
        // This is the only place a teardown like this is required, when operator YAML is applied inside the test context,
        // after each will remove CRDs first, but they will get stuck on finalizers so this method prevents it.
        ConsoleUtils.removeConsoleInstancesFinalizers(tcc.namespace());
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
