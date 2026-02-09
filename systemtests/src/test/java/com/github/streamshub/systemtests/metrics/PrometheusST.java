package com.github.streamshub.systemtests.metrics;

import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.prometheus.PrometheusInstanceSetup;
import com.github.streamshub.systemtests.setup.prometheus.PrometheusOperatorSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.REGRESSION)
public class PrometheusST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(PrometheusST.class);
    protected TestCaseConfig tcc;
    protected PrometheusOperatorSetup prometheusOperator;
    protected PrometheusInstanceSetup prometheusInstance;


    @Test
    void testCustomPrometheus() {
        LOGGER.info("Start");
        LOGGER.info("Start");
        LOGGER.info("Start");
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        prometheusOperator = new PrometheusOperatorSetup(tcc.namespace());
        prometheusInstance = new PrometheusInstanceSetup(tcc.namespace());

        prometheusOperator.setup();
        prometheusInstance.setup();


        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.
            getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName())
                .editSpec()
                    .addNewMetricsSource()
                        .withName(prometheusInstance.getName())
                        .withType(MetricsSource.Type.STANDALONE)
                        .withUrl(prometheusInstance.getPrometheusServerUrl())
                    .endMetricsSource()
                    .editFirstKafkaCluster()
                        .withMetricsSource(prometheusInstance.getName())
                    .endKafkaCluster()
                .endSpec()
            .build());
        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
        prometheusOperator.teardown();
    }
}

