package com.github.streamshub.systemtests.upgrade;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.console.YamlConfig;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
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
     * Verifies that upgrading the Console Operator installed from plain YAML manifests
     * (as opposed to OLM) correctly transitions from the old to the new operator version,
     * defined in {@code src/test/resources/upgrade/YamlUpgrade.yaml} (currently version
     * {@code 0.11.0} to {@code 0.12.3}), while preserving the existing Console instance
     * and keeping the UI fully functional throughout.
     *
     * <p>This test performs the following steps:</p>
     * <ul>
     *   <li>Installs the Console Operator into the {@code co-namespace} namespace from the
     *       <b>old operator YAML manifest URL</b> ({@code yamlVersionData.getOldOperatorCrdsUrl()}).</li>
     *   <li>Deploys a default Console instance linked to the test Kafka cluster.</li>
     *   <li>Validates that the deployed operator's {@code app.kubernetes.io/version} label matches
     *       the expected old operator version.</li>
     *   <li>Performs UI verification (overview and topics pages) to ensure the Console correctly
     *       reports all 17 pre-created topics (12 replicated, 3 under-replicated, 2 unavailable)
     *       before the upgrade.</li>
     *   <li>Upgrades the Console Operator in place using the <b>new operator YAML manifest URL</b>
     *       ({@code yamlVersionData.getNewOperatorCrdsUrl()}), without deleting existing resources.</li>
     *   <li>Waits for the Console operator deployment to roll and report the new operator version.</li>
     *   <li>Repeats the same UI checks to confirm the Console remains fully operational, still
     *       reporting all 17 topics correctly, after the upgrade.</li>
     * </ul>
     *
     * <p>This ensures that Console Operator upgrades performed via raw YAML manifests do not
     * disrupt an already-running Console instance or its topic-reporting UI.</p>
     */
    @Test
    void testUpgradeYamlOperator() {
        // Setup
        LOGGER.info("Installing old Console Operator version '{}' from YAML manifest URL: {}", yamlVersionData.getOldOperatorVersion(), yamlVersionData.getOldOperatorCrdsUrl());
        YamlConfig yamlConfig = new YamlConfig(Constants.CO_NAMESPACE, yamlVersionData.getOldOperatorCrdsUrl());

        consoleOperatorSetup.setInstallConfig(yamlConfig);
        consoleOperatorSetup.install(false);

        LOGGER.info("Setting up Console instance '{}' in namespace '{}' for Kafka cluster '{}'", tcc.consoleInstanceName(), tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());

        LOGGER.debug("Reading '{}' label from Console operator Deployment '{}' in namespace '{}'", Labels.K8S_VERSION_LABEL, Environment.CONSOLE_DEPLOYMENT_NAME, Constants.CO_NAMESPACE);
        String currentOperatorVersion = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, Constants.CO_NAMESPACE, Environment.CONSOLE_DEPLOYMENT_NAME)
            .getFirst().getMetadata().getLabels().get(Labels.K8S_VERSION_LABEL);

        LOGGER.info("Verifying Console operator version label reports old version, expected '{}', actual '{}'", yamlVersionData.getOldOperatorVersion(), currentOperatorVersion);
        assertEquals(yamlVersionData.getOldOperatorVersion(), currentOperatorVersion);

        LOGGER.info("Performing pre-upgrade UI checks, expecting {} topics in total ({} replicated, {} under-replicated, {} unavailable)",
            TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        PwUtils.login(tcc);

        TopicChecks.checkOverviewPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        TopicChecks.checkTopicsPageTopicState(tcc, tcc.kafkaName(), TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);

        // Upgrade
        LOGGER.info("Triggering in-place Console Operator upgrade from version '{}' to '{}' using YAML manifest URL: {}",
            yamlVersionData.getOldOperatorVersion(), yamlVersionData.getNewOperatorVersion(), yamlVersionData.getNewOperatorCrdsUrl());
        yamlConfig = new YamlConfig(Constants.CO_NAMESPACE, yamlVersionData.getNewOperatorCrdsUrl());
        consoleOperatorSetup.setInstallConfig(yamlConfig);
        // Do not delete resources to verify instance upgrades
        LOGGER.debug("Re-applying YAML manifests without deleting existing resources, to verify the Console instance survives the upgrade");
        consoleOperatorSetup.install(false);

        LOGGER.info("Waiting for Console operator Deployment '{}' to roll and report new version label '{}'", Environment.CONSOLE_DEPLOYMENT_NAME, yamlVersionData.getNewOperatorVersion());
        WaitUtils.waitForConsoleDeploymentToReachVersion(Constants.CO_NAMESPACE, Environment.CONSOLE_DEPLOYMENT_NAME, yamlVersionData.getNewOperatorVersion(),
            deployment -> deployment.getMetadata().getLabels().get(Labels.K8S_VERSION_LABEL));

        LOGGER.info("Performing post-upgrade UI checks, verifying all {} topics are still correctly reported after upgrading to version '{}'",
            TOTAL_TOPICS_COUNT, yamlVersionData.getNewOperatorVersion());
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
