package com.github.streamshub.systemtests.metrics;

import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.OpenShiftOnly;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.ClusterOverviewPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.prometheus.PrometheusInstanceSetup;
import com.github.streamshub.systemtests.setup.prometheus.PrometheusOperatorSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@OpenShiftOnly
@Tag(TestTags.REGRESSION)
public class PrometheusST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(PrometheusST.class);
    protected TestCaseConfig tcc;
    protected PrometheusOperatorSetup prometheusOperator;
    protected PrometheusInstanceSetup prometheusInstance;

    /**
     * Tests that the console uses an externally provided (standalone) Prometheus instance for metrics
     * instead of deploying its own embedded one.
     *
     * <p>The console instance under test is configured, as part of the class setup, with a
     * {@code STANDALONE} metrics source pointing at a Prometheus instance that was deployed ahead of time
     * via the Prometheus Operator ({@link PrometheusOperatorSetup} and {@link PrometheusInstanceSetup}).
     * The test first verifies that no embedded console Prometheus pod (matching the prefix
     * {@code <CONSOLE_INSTANCE>-<namespace-hash>-prometheus-deployment}) has been deployed, confirming the
     * console did not fall back to bundling its own metrics stack.</p>
     *
     * <p>It then navigates to the Kafka Cluster Overview page and verifies that the metrics charts are
     * populated with real data sourced from the standalone Prometheus instance:</p>
     * <ul>
     *   <li>The disk space usage chart contains per-node series for the cluster's 6 nodes (3 broker
     *       replicas plus 3 controller replicas), checked via the presence of "Node 0" and "Node 5" labels.</li>
     *   <li>The CPU usage chart likewise contains "Node 0" and "Node 5" series.</li>
     *   <li>The memory usage chart likewise contains "Node 0" and "Node 5" series.</li>
     *   <li>The topic bytes chart contains both "Incoming bytes" and "Outgoing bytes" series, populated from
     *       traffic previously produced and consumed against the {@code replicated} topic.</li>
     * </ul>
     *
     * <p>This ensures the console correctly integrates with an externally managed Prometheus instance as a
     * metrics source, rather than silently deploying a redundant embedded Prometheus.</p>
     */
    @Test
    void testCustomPrometheus() {
        LOGGER.info("Verifying no embedded console Prometheus pod exists with prefix '{}'", Constants.CONSOLE_INSTANCE + "-" + Utils.hashStub(tcc.namespace()) + "-prometheus-deployment");
        assertTrue(ResourceUtils.listKubeResourcesByPrefix(Pod.class, tcc.namespace(), Constants.CONSOLE_INSTANCE + "-" + Utils.hashStub(tcc.namespace()) + "-prometheus-deployment").isEmpty());
        LOGGER.debug("Confirmed no embedded Prometheus pod is deployed in namespace '{}'", tcc.namespace());

        LOGGER.info("Navigating to Kafka Cluster Overview page for cluster '{}'", tcc.kafkaName());
        PwUtils.navigate(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()));

        LOGGER.info("Verifying disk space usage chart contains per-node series for 'Node 0' and 'Node 5'");
        // Disk
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_DISK_SPACE_CHART_NODE_TEXT_ITEMS, "Node 0", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_DISK_SPACE_CHART_NODE_TEXT_ITEMS, "Node 5", true);

        LOGGER.info("Verifying CPU usage chart contains per-node series for 'Node 0' and 'Node 5'");
        // CPU
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CPU_USAGE_CHART_NODE_TEXT_ITEMS, "Node 0", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_CPU_USAGE_CHART_NODE_TEXT_ITEMS, "Node 5", true);

        LOGGER.info("Verifying memory usage chart contains per-node series for 'Node 0' and 'Node 5'");
        // Memory
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_MEMORY_USAGE_CHART_NODE_TEXT_ITEMS, "Node 0", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_MEMORY_USAGE_CHART_NODE_TEXT_ITEMS, "Node 5", true);

        LOGGER.info("Verifying topic bytes chart contains incoming and outgoing bytes series for topic '{}'", Constants.REPLICATED_TOPICS_PREFIX);
        // Topics
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPIC_BYTES_CHART_NODE_TEXT_ITEMS, "Incoming bytes", true);
        PwUtils.waitForContainsText(tcc, ClusterOverviewPageSelectors.COPS_TOPIC_BYTES_CHART_NODE_TEXT_ITEMS, "Outgoing bytes", true);
    }

    @BeforeAll
    void testClassSetup() {
        // // Init test case config based on the test context
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

        KafkaTopicUtils.setupTopicsIfNeededAndReturn(tcc.namespace(), tcc.kafkaName(), Constants.REPLICATED_TOPICS_PREFIX, 1, 1, 1, 1);
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
    }
}

