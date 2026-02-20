package com.github.streamshub.systemtests.kafka;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
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
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.connect.KafkaConnectResources;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;

@Tag(TestTags.REGRESSION)
public class ConnectST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ConnectST.class);

    private static final String KAFKA_CONNECT_SRC_NAME = "k-cnct-source";
    private static final String KAFKA_CONNECT_SINK_NAME = "k-cnct-sink";

    @Test
    void testFilterKafkaConnect() {
        LOGGER.info("START");
        TestCaseConfig tcc = getTestCaseConfig();
        String sourceConnectorName = "license-source";
        String sinkConnectorName = "text-sink";
        String topicPrefix = "my-connector-topic";
        String connectorMessage = "Hello connector!";

        String topicName = KafkaTopicUtils.setupTopicsAndReturn(tcc.namespace(), tcc.kafkaName(), topicPrefix, 1, true, 1, 1, 1)
            .get(0)
            .getMetadata()
            .getName();

        KubeResourceManager.get().createResourceWithWait(KafkaConnectSetup.defaultFileSourceConnector(tcc.namespace(), sourceConnectorName, KAFKA_CONNECT_SRC_NAME, topicName, 2).build());
        KubeResourceManager.get().createResourceWithWait(KafkaConnectSetup.defaultFileSinkConnector(tcc.namespace(), sinkConnectorName, KAFKA_CONNECT_SINK_NAME, topicName, 2).build());

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
            .withMessage(connectorMessage)
            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer(), clients.consumer());
        WaitUtils.waitForClientsSuccess(clients);

        KafkaCmdUtils.waitForConnectorInServiceApi(tcc.namespace(), KAFKA_CONNECT_SRC_NAME, sourceConnectorName);
        KafkaCmdUtils.waitForConnectorInServiceApi(tcc.namespace(), KAFKA_CONNECT_SINK_NAME, sinkConnectorName);

        // Filter connectors
        tcc.page().navigate(PwPageUrls.getKafkaConnectorPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, "Kafka Connect", true);

        // Filter connect
        tcc.page().navigate(PwPageUrls.getKafkaConnectPage(tcc, tcc.kafkaName()));
        PwUtils.waitForContainsText(tcc, SingleConsumerGroupPageSelectors.SCGPS_PAGE_HEADER_NAME, "Kafka Connect", true);
        LOGGER.info("STOP");
    }

    @AfterEach
    void testCaseTeardown() {
        getTestCaseConfig().playwright().close();
    }

    @BeforeEach
    void testCaseSetup() {
        final TestCaseConfig tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        // Deploy two kafka connect clusters
        KafkaConnectSetup.setupDefaultKafkaDefaultConnectWithFilePluginIfNeeded(tcc.namespace(), KAFKA_CONNECT_SRC_NAME, tcc.kafkaName(), tcc.kafkaUserName(), tcc.consoleInstanceName());
        KafkaConnectSetup.setupDefaultKafkaDefaultConnectWithFilePluginIfNeeded(tcc.namespace(), KAFKA_CONNECT_SINK_NAME, tcc.kafkaName(), tcc.kafkaUserName(), tcc.consoleInstanceName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName())
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
            .build());
        PwUtils.login(tcc);
    }
}
