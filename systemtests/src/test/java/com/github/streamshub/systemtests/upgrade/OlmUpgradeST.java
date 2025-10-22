package com.github.streamshub.systemtests.upgrade;


import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.console.OlmConfig;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;

@Tag(TestTags.OLM_UPGRADE)
public class OlmUpgradeST extends AbstractUpgradeST {
    private static final Logger LOGGER = LogWrapper.getLogger(OlmUpgradeST.class);
    private TestCaseConfig tcc;
    private ConsoleOperatorSetup consoleOperatorSetup = new ConsoleOperatorSetup(Constants.CO_NAMESPACE);
    OlmVersionModificationData olmVersionData = new VersionModificationDataLoader(VersionModificationDataLoader.InstallType.OLM).getOlmUpgradeData();

    @Test
    void testUpgradeOlmOperator() {
        // First setup console operator with specific channel
        OlmConfig olmConfig = new OlmConfig(Constants.CO_NAMESPACE);
        //
        olmConfig.setChannelName(olmVersionData.getOldOlmChannel());
        consoleOperatorSetup.setInstallConfig(olmConfig);
        consoleOperatorSetup.install();
        // Second setup console instance
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()));
        PwUtils.login(tcc);
        // Check basic stuff
        LOGGER.info("START TEST");
        // Upgrade
        olmConfig.setChannelName(olmVersionData.getNewOlmChannel());
        consoleOperatorSetup.setInstallConfig(olmConfig);
        consoleOperatorSetup.install();
        // Check stuff still works
        LOGGER.info("START SECOND TEST");
    }


    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }

}
