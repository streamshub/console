package com.github.streamshub.systemtests.system;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(FakeST.class);
    private static final String NAMESPACE_PREFIX = "fake-st";

    @Test
    void fakeTestOne() {
        // Instantiate test case config for current context
        TestCaseConfig tcc = new TestCaseConfig(KubeResourceManager.get().getTestContext(), NAMESPACE_PREFIX);
        // Create namespace, deploy kafka, deploy console instance
        tcc.defaultTestCaseSetup();

        LOGGER.info("Test starts now");
        assertTrue(true);

        // Close current testcase playwright context
        tcc.close();
    }
}
