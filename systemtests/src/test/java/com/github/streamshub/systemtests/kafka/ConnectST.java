package com.github.streamshub.systemtests.kafka;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.KafkaConnectPageSelectors;
import com.github.streamshub.systemtests.locators.SingleConsumerGroupPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaConnectSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaCmdUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.connect.KafkaConnectResources;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static com.github.streamshub.systemtests.utils.resourceutils.PodUtils.getPodSnapshotBySelector;

@Tag(TestTags.REGRESSION)
public class ConnectST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConnectST.class);
    private TestCaseConfig tcc;
    private static final String CONNECT_CLUSTERS_WITH_SINK_SOURCE_CONNECTORS_BUCKET = "ConnectClustersWithSinkSourceConnectors";

    private static final String KAFKA_CONNECT_SRC_NAME = "k-cnct-source";
    private static final String KAFKA_CONNECT_SINK_NAME = "k-cnct-sink";

    private static final String SOURCE_CONNECTOR_NAME = "license-source";
    private static final String SINK_CONNECTOR_NAME = "text-sink";
    private static final String CONNECTOR_TOPIC = "my-connector-topic";
    private static final String CONNECTOR_MESSAGE = "Hello connector!";

    /**
     * Verifies the name-based filtering functionality for Kafka Connect connectors
     * and Kafka Connect clusters in the UI.
     *
     * <p>This test validates that the search filter correctly narrows displayed
     * results based on the provided name input.</p>
     *
     * <p>The test performs the following validations:</p>
     * <ul>
     *     <li>Navigates to the Kafka Connect connectors page and verifies the
     *     page header and initial connector count.</li>
     *     <li>Filters connectors by source connector name and verifies:
     *         <ul>
     *             <li>Exactly one connector is displayed</li>
     *             <li>The connector name matches the expected source connector</li>
     *             <li>The associated Kafka Connect cluster name is correct</li>
     *             <li>The connector type is correctly identified as {@code Source}</li>
     *         </ul>
     *     </li>
     *     <li>Filters connectors by sink connector name and verifies:
     *         <ul>
     *             <li>Exactly one connector is displayed</li>
     *             <li>The connector name matches the expected sink connector</li>
     *             <li>The associated Kafka Connect cluster name is correct</li>
     *             <li>The connector type is correctly identified as {@code Sink}</li>
     *         </ul>
     *     </li>
     *     <li>Navigates to the Kafka Connect clusters page and verifies the
     *     initial cluster count.</li>
     *     <li>Filters clusters by source cluster name and validates that only
     *     the matching cluster is displayed.</li>
     *     <li>Filters clusters by sink cluster name and validates that only
     *     the matching cluster is displayed.</li>
     * </ul>
     *
     * <p>This test ensures that the name filter input and search action behave
     * consistently across both connectors and cluster listing pages, and that
     * filtered results accurately reflect the expected Kafka Connect resources.</p>
     */
    @Test
    @TestBucket(CONNECT_CLUSTERS_WITH_SINK_SOURCE_CONNECTORS_BUCKET)
    void testFilterKafkaConnectClustersAndConnectors() {
        tcc.page().navigate(PwPageUrls.getKafkaConnectorPage(tcc, tcc.kafkaName()));

        LOGGER.debug("Verifying Kafka Connect page header is visible");
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, "Kafka Connect", true);

        LOGGER.debug("Waiting for connectors table and verifying initial count (expected: 2)");
        PwUtils.waitForLocatorVisible(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_INPUT);
        PwUtils.waitForLocatorCount(tcc, 2, KafkaConnectPageSelectors.KCPS_TABLE_ITEMS, true);

        // ---- SRC connector filter ----
        LOGGER.info("Filtering connectors by source connector name: {}", SOURCE_CONNECTOR_NAME);
        PwUtils.waitForLocatorAndFill(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_INPUT, SOURCE_CONNECTOR_NAME);
        PwUtils.waitForLocatorAndClick(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_SEARCH_BUTTON);

        LOGGER.debug("Verifying filtered connectors");
        PwUtils.waitForLocatorCount(tcc, 1, KafkaConnectPageSelectors.KCPS_TABLE_ITEMS, false);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 1), SOURCE_CONNECTOR_NAME, true);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 2), KAFKA_CONNECT_SRC_NAME, true);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 3), "Source", true);

        // ---- Sink connector filter ----
        LOGGER.info("Filtering connectors by sink connector name: {}", SINK_CONNECTOR_NAME);
        PwUtils.waitForLocatorAndFill(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_INPUT, SINK_CONNECTOR_NAME);
        PwUtils.waitForLocatorAndClick(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_SEARCH_BUTTON);

        LOGGER.debug("Verifying filtered connectors");
        PwUtils.waitForLocatorCount(tcc, 1, KafkaConnectPageSelectors.KCPS_TABLE_ITEMS, false);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 1), SINK_CONNECTOR_NAME, true);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 2), KAFKA_CONNECT_SINK_NAME, true);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 3), "Sink", true);

        // Filter connect clusters
        LOGGER.info("Navigating to, Kafka Connect clusters page");
        tcc.page().navigate(PwPageUrls.getKafkaConnectClusterPage(tcc, tcc.kafkaName()));

        LOGGER.debug("Verifying Kafka Connect clusters page header");
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, "Kafka Connect", true);

        LOGGER.debug("Waiting for clusters table and verifying initial count (expected: 2)");
        PwUtils.waitForLocatorVisible(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_INPUT);
        PwUtils.waitForLocatorCount(tcc, 2, KafkaConnectPageSelectors.KCPS_TABLE_ITEMS, true);

        // ---- Source cluster filter ----
        LOGGER.info("Filtering Kafka Connect clusters by source cluster name: {}", KAFKA_CONNECT_SRC_NAME);
        PwUtils.waitForLocatorAndFill(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_INPUT, KAFKA_CONNECT_SRC_NAME);
        PwUtils.waitForLocatorAndClick(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_SEARCH_BUTTON);

        LOGGER.debug("Verifying filtered cluster results");
        PwUtils.waitForLocatorCount(tcc, 1, KafkaConnectPageSelectors.KCPS_TABLE_ITEMS, false);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 1), KAFKA_CONNECT_SRC_NAME, true);

        // ---- Sink cluster filter ----
        LOGGER.info("Filtering Kafka Connect clusters by sink cluster name: {}", KAFKA_CONNECT_SINK_NAME);
        PwUtils.waitForLocatorAndFill(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_INPUT, KAFKA_CONNECT_SINK_NAME);
        PwUtils.waitForLocatorAndClick(tcc, KafkaConnectPageSelectors.KCPS_NAME_FILTER_SEARCH_BUTTON);

        LOGGER.debug("Verifying filtered cluster results");
        PwUtils.waitForLocatorCount(tcc, 1, KafkaConnectPageSelectors.KCPS_TABLE_ITEMS, false);
        PwUtils.waitForContainsText(tcc, KafkaConnectPageSelectors.getTableRowItem(1, 1), KAFKA_CONNECT_SINK_NAME, true);

        LOGGER.info("Kafka Connect filtering test finished successfully");
    }

    /**
     * Prepares two Kafka Connect clusters (source and sink) with corresponding
     * file-based connectors and test data for UI system testing.
     *
     * <p>This setup method is executed for the
     * {@code CONNECT_CLUSTERS_WITH_SINK_SOURCE_CONNECTORS_BUCKET} and ensures
     * that the Console instance is configured to display and interact with
     * multiple Kafka Connect clusters and their connectors.</p>
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *     <li>Takes a snapshot of existing Console pods to detect rollout after configuration changes.</li>
     *     <li>Deploys two Kafka Connect clusters (source and sink) with the file plugin enabled.</li>
     *     <li>Updates the Console custom resource to register both Kafka Connect clusters,
     *     including their namespace, associated Kafka cluster, and REST API URL.</li>
     *     <li>Waits for the Console deployment to roll and stabilize after the configuration update.</li>
     *     <li>Creates a dedicated Kafka topic for connector data exchange.</li>
     *     <li>Deploys:
     *         <ul>
     *             <li>A file source connector attached to the source Kafka Connect cluster.</li>
     *             <li>A file sink connector attached to the sink Kafka Connect cluster.</li>
     *         </ul>
     *     </li>
     *     <li>Produces and consumes test messages to populate the connector topic,
     *     using SCRAM-SHA authenticated Kafka clients.</li>
     *     <li>Waits until both connectors are available via the Kafka Connect REST API.</li>
     * </ul>
     *
     * <p>This setup ensures that subsequent UI tests can validate:</p>
     * <ul>
     *     <li>Listing and filtering of Kafka Connect clusters</li>
     *     <li>Listing and filtering of source and sink connectors</li>
     *     <li>Correct association between connectors and their respective clusters</li>
     *     <li>Proper integration between Console, Kafka, and Kafka Connect components</li>
     * </ul>
     *
     * @see KafkaConnectSetup
     * @see KafkaTopicUtils
     * @see KafkaClientsBuilder
     * @see WaitUtils
     * @see KafkaCmdUtils
     */
    @SetupTestBucket(CONNECT_CLUSTERS_WITH_SINK_SOURCE_CONNECTORS_BUCKET)
    public void prepareKafkaConnectClustersWithSinkSourceConnectors() {
        Map<String, String> oldSnap = getPodSnapshotBySelector(tcc.namespace(), Labels.getConsolePodSelector(tcc.consoleInstanceName()));
        // Deploy two kafka connect clusters
        KafkaConnectSetup.setupDefaultKafkaDefaultConnectWithFilePluginIfNeeded(tcc.namespace(), KAFKA_CONNECT_SRC_NAME, tcc.kafkaName(), tcc.kafkaUserName(), tcc.consoleInstanceName());
        KafkaConnectSetup.setupDefaultKafkaDefaultConnectWithFilePluginIfNeeded(tcc.namespace(), KAFKA_CONNECT_SINK_NAME, tcc.kafkaName(), tcc.kafkaUserName(), tcc.consoleInstanceName());
        // Update console
        Console console = new ConsoleBuilder(ResourceUtils.getKubeResource(Console.class, tcc.namespace(), tcc.consoleInstanceName()))
            .editSpec()
                .addNewKafkaConnectCluster()
                    .withName(KAFKA_CONNECT_SRC_NAME)
                    .withNamespace(tcc.namespace())
                    .withKafkaClusters(tcc.namespace() + "/" + tcc.kafkaName())
                    .withUrl(KafkaConnectResources.url(KAFKA_CONNECT_SRC_NAME, tcc.namespace(), Constants.CONNECT_SERVICE_PORT))
                .endKafkaConnectCluster()
                .addNewKafkaConnectCluster()
                    .withName(KAFKA_CONNECT_SINK_NAME)
                    .withNamespace(tcc.namespace())
                    .withKafkaClusters(tcc.namespace() + "/" + tcc.kafkaName())
                    .withUrl(KafkaConnectResources.url(KAFKA_CONNECT_SINK_NAME, tcc.namespace(), Constants.CONNECT_SERVICE_PORT))
                .endKafkaConnectCluster()
            .endSpec()
            .build();

        KubeResourceManager.get().createOrUpdateResourceWithWait(console);

        WaitUtils.waitForComponentPodsToRoll(tcc.namespace(), Labels.getConsolePodSelector(tcc.consoleInstanceName()), oldSnap);

        String topicName = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), CONNECTOR_TOPIC, 1, true, 1, 1, 1)
            .get(0)
            .getMetadata()
            .getName();

        KubeResourceManager.get().createResourceWithWait(KafkaConnectSetup.defaultFileSourceConnector(tcc.namespace(), SOURCE_CONNECTOR_NAME, KAFKA_CONNECT_SRC_NAME, topicName, 2).build());
        KubeResourceManager.get().createResourceWithWait(KafkaConnectSetup.defaultFileSinkConnector(tcc.namespace(), SINK_CONNECTOR_NAME, KAFKA_CONNECT_SINK_NAME, topicName, 2).build());

        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicName)
            .withMessageCount(Constants.MESSAGE_COUNT)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage(CONNECTOR_MESSAGE)
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        KafkaCmdUtils.waitForConnectorInServiceApi(tcc.namespace(), KAFKA_CONNECT_SRC_NAME, SOURCE_CONNECTOR_NAME);
        KafkaCmdUtils.waitForConnectorInServiceApi(tcc.namespace(), KAFKA_CONNECT_SINK_NAME, SINK_CONNECTOR_NAME);
    }

    @AfterEach
    void testCaseTeardown() {
        getTestCaseConfig().playwright().close();
    }

    @BeforeEach
    void testCaseSetup() {
        tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        KafkaConnectSetup.setupDefaultKafkaDefaultConnectWithFilePluginIfNeeded(tcc.namespace(), tcc.connectName(), tcc.kafkaName(), tcc.kafkaUserName(), tcc.consoleInstanceName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }
}
