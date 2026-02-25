package com.github.streamshub.systemtests.kroxylicious;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.kroxylicious.KroxyliciousOperatorSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.REGRESSION)
public class KroxyST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(KroxyST.class);

    protected TestCaseConfig tcc;
    protected KroxyliciousOperatorSetup kroxyOperatorSetup;

    @Test
    void testDisplayVirtualCluster() {
        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        LOGGER.info("Test");
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        kroxyOperatorSetup = new KroxyliciousOperatorSetup(tcc.namespace());
        kroxyOperatorSetup.setup();

        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());

        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
        kroxyOperatorSetup.teardown();
    }
}
