package com.github.streamshub.systemtests.system;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class FakeST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(FakeST.class);
    private static final String NAMESPACE_PREFIX = "fake-st";

    @Test
    void fakeTestOne() {
        // Deploy default Kafka
        TestCaseConfig tcc = new TestCaseConfig(KubeResourceManager.get().getTestContext(), NAMESPACE_PREFIX);
        tcc.defaultTestCaseSetup();
        // Deploy console instance
        ConsoleInstanceSetup consoleInstanceSetup = new ConsoleInstanceSetup(tcc.getNamespaceName(), tcc.getDefaultKafkaCluster().getClusterName());
        consoleInstanceSetup.deploy();

        LOGGER.info("Test starts now");
    }
}
