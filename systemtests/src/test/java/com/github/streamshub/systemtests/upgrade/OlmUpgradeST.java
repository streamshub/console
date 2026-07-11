package com.github.streamshub.systemtests.upgrade;


import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.console.OlmConfig;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
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



    // 0.12.0 CSS
    private static final String COPS_TOPICS_CARD_TOTAL_TOPICS = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div > div.pf-v6-l-flex > div:nth-of-type(1) > div.pf-v6-c-content";
    private static final String COPS_TOPICS_CARD_TOTAL_PARTITIONS = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-content";
    private static final String COPS_TOPICS_CARD_FULLY_REPLICATED = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(2) > div:nth-of-type(1)";
    private static final String COPS_TOPICS_CARD_UNDER_REPLICATED = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(2) > div:nth-of-type(2)";
    private static final String COPS_TOPICS_CARD_UNAVAILABLE = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > section.pf-v6-c-page__main-section > div.pf-v6-c-page__main-body > div.pf-v6-l-grid > div.pf-v6-l-grid__item:nth-of-type(2) > div.pf-v6-l-flex > div:nth-of-type(2) > div.pf-v6-c-card > div.pf-v6-c-card__body:nth-of-type(2) > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(2) > div:nth-of-type(3)";
    //
    private static final String TPS_HEADER_TOTAL_TOPICS_BADGE = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(2) > span.pf-v6-c-label > span.pf-v6-c-label__content > span";
    private static final String TPS_HEADER_BADGE_STATUS_SUCCESS = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(3) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";
    private static final String TPS_HEADER_BADGE_STATUS_WARNING = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(4) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";
    private static final String TPS_HEADER_BADGE_STATUS_ERROR = "body > div:nth-of-type(2) > div.pf-v6-c-page > div.pf-v6-c-page__main-container:nth-of-type(2) > main.pf-v6-c-page__main > div.pf-v6-c-drawer > div.pf-v6-c-drawer__main > div.pf-v6-c-drawer__content:nth-of-type(1) > div.pf-v6-c-drawer__body > div.pf-v6-c-page__main-group > section.pf-v6-c-page__main-section:nth-of-type(2) > div.pf-v6-c-page__main-body > div.pf-v6-l-flex > div.pf-v6-l-flex:nth-of-type(1) > div:nth-of-type(1) > h1.pf-v6-c-title > div.pf-v6-l-split > div.pf-v6-l-split__item:nth-of-type(5) > div > span.pf-v6-c-label > span.pf-v6-c-label__content > span.pf-v6-c-label__text:nth-of-type(2)";

    /**
     * Verifies that upgrading the Console Operator through OLM (Operator Lifecycle Manager)
     * successfully transitions to the new version without disrupting the Console instance or UI functionality.
     *
     * <p>The old and new channel/version values are loaded from
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
        // 0.12.0 console had sign in with credentials button
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), false, false);
        PwUtils.waitForLocatorAndClick(tcc, "body > div > div > div > div > form > button");

        // Status
        // Overview
        LOGGER.info("Checking overview page topic status: {} total topics, {} partitions, {} fully replicated, {} under-replicated, {} unavailable", TOTAL_TOPICS_COUNT, TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_TOTAL_TOPICS, TOTAL_TOPICS_COUNT + " topics", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_TOTAL_PARTITIONS, TOTAL_TOPICS_COUNT + " partitions", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_FULLY_REPLICATED, TOTAL_REPLICATED_TOPICS_COUNT + " Fully replicated", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_UNDER_REPLICATED, UNDER_REPLICATED_TOPICS_COUNT + " Under replicated", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, COPS_TOPICS_CARD_UNAVAILABLE, UNAVAILABLE_TOPICS_COUNT + " Unavailable", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        // Topics page
        LOGGER.info("Checking topics page topic status: {} total topics, {} fully replicated, {} under-replicated, {} unavailable", TOTAL_TOPICS_COUNT, TOTAL_REPLICATED_TOPICS_COUNT, UNDER_REPLICATED_TOPICS_COUNT, UNAVAILABLE_TOPICS_COUNT);
        PwUtils.navigate(tcc, PwPageUrls.getTopicsPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, TPS_HEADER_TOTAL_TOPICS_BADGE, TOTAL_TOPICS_COUNT + " total", false, true, TimeConstants.ACTION_WAIT_MEDIUM, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, TPS_HEADER_BADGE_STATUS_SUCCESS, Integer.toString(TOTAL_REPLICATED_TOPICS_COUNT), false, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, TPS_HEADER_BADGE_STATUS_WARNING, Integer.toString(UNDER_REPLICATED_TOPICS_COUNT), false, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);
        PwUtils.waitForContainsText(tcc, TPS_HEADER_BADGE_STATUS_ERROR, Integer.toString(UNAVAILABLE_TOPICS_COUNT), false, true, TimeConstants.ACTION_WAIT_SHORT, Constants.SELECTOR_RETRIES);

        // Upgrade
        LOGGER.info("Triggering Console Operator OLM upgrade: switching subscription from channel '{}' to channel '{}'", olmVersionData.getOldOlmChannel(), olmVersionData.getNewOlmChannel());
        olmConfig.updateChannel(olmVersionData.getNewOlmChannel());

        LOGGER.info("Waiting for Console Operator deployment to roll and report the new version '{}'", olmVersionData.getNewOperatorVersion());
        WaitUtils.waitForConsoleDeploymentToReachVersion(Constants.CO_NAMESPACE, Environment.CONSOLE_OLM_PACKAGE_NAME, olmVersionData.getNewOperatorVersion(),
            deployment -> deployment.getMetadata().getName().replace(Environment.CONSOLE_OLM_PACKAGE_NAME + "-v", ""));

        Utils.sleepWait(TimeConstants.COMPONENT_LOAD_TIMEOUT);
        PwUtils.login(tcc);

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

        // KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, REPLICATED_TOPICS_COUNT, 1, 1, 1);
        // KafkaTopicUtils.setupUnmanagedUnderReplicatedAndUnavailableTopics(tcc.namespace(), tcc.kafkaName(), KafkaNamingUtils.kafkaUserName(tcc.kafkaName()),
        //     new KafkaTopicUtils.TopicTypeSpec(Constants.UNMANAGED_REPLICATED_TOPICS_PREFIX, UNMANAGED_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1),
        //     new KafkaTopicUtils.TopicTypeSpec(Constants.UNDER_REPLICATED_TOPICS_PREFIX, UNDER_REPLICATED_TOPICS_COUNT, tcc.defaultMessageCount(), 1, scaledUpBrokerReplicas, scaledUpBrokerReplicas),
        //     new KafkaTopicUtils.TopicTypeSpec(Constants.UNAVAILABLE_TOPICS_PREFIX, UNAVAILABLE_TOPICS_COUNT, tcc.defaultMessageCount(), 1, 1, 1));
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }

}
