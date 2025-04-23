package com.github.streamshub.systemtests.system;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.KafkaNamingUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(FakeST.class);

    @Test
    void fakeTestOne() {
        TestCaseConfig tcc = getTestCaseConfig();
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), KafkaNamingUtils.kafkaClusterName(tcc.namespace()));
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace()));
        LOGGER.info("Test starts now");
        assertTrue(true);
    }
}
