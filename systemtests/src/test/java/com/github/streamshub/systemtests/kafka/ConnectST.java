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
import com.github.streamshub.systemtests.locators.SingleConsumerGroupPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaConnectSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaCmdUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
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

    @Test
    @TestBucket(CONNECT_CLUSTERS_WITH_SINK_SOURCE_CONNECTORS_BUCKET)
    void testFilterKafkaConnect() {
        LOGGER.info("START");

        // Filter connectors
        tcc.page().navigate(PwPageUrls.getKafkaConnectorPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, "Kafka Connect", true);

        // Filter connect
        tcc.page().navigate(PwPageUrls.getKafkaConnectPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, "Kafka Connect", true);
        LOGGER.info("STOP");
    }

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
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName()).build());
        PwUtils.login(tcc);
    }
}
