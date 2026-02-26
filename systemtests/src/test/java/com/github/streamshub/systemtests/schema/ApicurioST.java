package com.github.streamshub.systemtests.schema;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.apicurio.ApicurioOperatorSetup;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.apicurio.ApicurioUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApicurioST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioST.class);

    protected TestCaseConfig tcc;
    protected ApicurioOperatorSetup apicurioOperatorSetup;

    @Test
    void testApicurioSchemaRegistry() {
        LOGGER.info("Test");
        LOGGER.info("STOP");
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        apicurioOperatorSetup = new ApicurioOperatorSetup(Constants.CO_NAMESPACE, tcc.namespace());
        apicurioOperatorSetup.setup();

        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName())
                .editSpec()
                    .addNewSchemaRegistry()
                        .withName(tcc.apicurioRegistryName())
                        .withUrl("http://" + ApicurioUtils.getApicurioServiceUrl(tcc.namespace(), tcc.apicurioRegistryName()) + "/apis/registry/v2")
                    .endSchemaRegistry()
                .endSpec()
            .build());

        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
        apicurioOperatorSetup.teardown();
    }
}
