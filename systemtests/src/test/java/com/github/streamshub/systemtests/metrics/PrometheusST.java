package com.github.streamshub.systemtests.metrics;

import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.prometheus.PrometheusInstanceSetup;
import com.github.streamshub.systemtests.setup.prometheus.PrometheusOperatorSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.REGRESSION)
public class PrometheusST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(PrometheusST.class);
    protected TestCaseConfig tcc;
    protected PrometheusOperatorSetup prometheusOperator;
    protected PrometheusInstanceSetup prometheusInstance;


    @Test
    void testCustomPrometheus() {
        LOGGER.info("Verify that the default prometheus is not deployed");
        assertTrue(ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), Constants.CONSOLE_INSTANCE + "-" + Utils.hashStub(tcc.namespace()) + "-prometheus-deployment").isEmpty());

        tcc.page().navigate(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));
        LOGGER.info("Verify charts are present and contain data");
        // Disk
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_DISK_SPACE_CHART_NODE_TEXT_ITEMS).nth(1).build(), "Node 0", true);
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_DISK_SPACE_CHART_NODE_TEXT_ITEMS).nth(6).build(), "Node 5", true);

        // CPU
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_CPU_USAGE_CHART_NODE_TEXT_ITEMS).nth(1).build(), "Node 0", true);
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_CPU_USAGE_CHART_NODE_TEXT_ITEMS).nth(6).build(), "Node 5", true);

        // Memory
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_MEMORY_USAGE_CHART_NODE_TEXT_ITEMS).nth(1).build(), "Node 0", true);
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_MEMORY_USAGE_CHART_NODE_TEXT_ITEMS).nth(6).build(), "Node 5", true);

        // Topics
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_TOPIC_BYTES_CHART_NODE_TEXT_ITEMS).nth(1).build(), "Incoming bytes", true);
        PwUtils.waitForContainsText(tcc,
            new CssBuilder(ClusterOverviewPageSelectors.COPS_TOPIC_BYTES_CHART_NODE_TEXT_ITEMS).nth(2).build(), "Outgoing bytes", true);
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

        KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, 1, true, 1, 1, 1);
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(Constants.REPLICATED_TOPICS_PREFIX)
            .withMessageCount(Constants.MESSAGE_COUNT_HIGH)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(Constants.REPLICATED_TOPICS_PREFIX))
            .withConsumerName(KafkaNamingUtils.consumerName(Constants.REPLICATED_TOPICS_PREFIX))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(Constants.REPLICATED_TOPICS_PREFIX))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
        prometheusOperator.teardown();
    }
}

