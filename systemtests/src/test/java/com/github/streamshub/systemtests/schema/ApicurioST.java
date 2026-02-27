package com.github.streamshub.systemtests.schema;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.apicurio.ApicurioOperatorSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaUtils;
import com.github.streamshub.systemtests.utils.resourceutils.apicurio.ApicurioUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class ApicurioST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioST.class);

    protected TestCaseConfig tcc;
    protected ApicurioOperatorSetup apicurioOperatorSetup;

    @Test
    void testApicurioSchemaRegistry() throws InvalidProtocolBufferException {
        LOGGER.info("Test");
        String topicName = "my-schema-topic";

        //
        // KubeResourceManager.get().createResourceWithWait(
        //     KafkaTopicUtils.defaultTopic(tcc.namespace(), tcc.kafkaName(),  topicName, 1, 1, 1).build());

        KafkaClients proto = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicName)
            .withMessageCount(10)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage("{\"id\": 1, \"message\": \"verify-protobuf\"}")
            .withAdditionalConfig(
                """
                %s
                value.serializer=io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer
                apicurio.registry.url=%s
                apicurio.registry.group-id=default
                apicurio.registry.artifact-id=%s
                apicurio.registry.artifact-version=1
                apicurio.registry.headers.enabled=true
                apicurio.registry.find-latest=true
                apicurio.registry.auto-register=false
                """
                .formatted(
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(),tcc.kafkaUserName(),SecurityProtocol.SASL_PLAINTEXT),
                    ApicurioUtils.getApicurioRegistryApiUrl(tcc.namespace(),tcc.apicurioRegistry3Name()),
                    topicName + "-value"
                )
            )
            .build();

        KubeResourceManager.get().createResourceWithWait(proto.producer());
        WaitUtils.waitForClientSuccess(tcc.namespace(), proto.getProducerName(), proto.getMessageCount(), true);


        // Avro
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicName)
            .withMessageCount(10)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage("{\"id\": 1, \"message\": \"verify-avro\"}")
            .withHeaders(Constants.APICURIO_VALUE_GROUP_ID + "=default, " + Constants.APICURIO_VALUE_ARTIFACT_ID + "=test-avro-schema")

            .withAdditionalConfig(KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT))
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(tcc.namespace(), clients.getProducerName(), clients.getMessageCount(), true);


        // Protobuf
        clients = new KafkaClientsBuilder(clients)
            .withHeaders(Constants.APICURIO_VALUE_GROUP_ID + "=default, " + Constants.APICURIO_VALUE_ARTIFACT_ID + "=test-proto-schema")
            .withMessage("{\"id\": 1, \"message\": \"verify-protobuf\"}")
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(tcc.namespace(), clients.getProducerName(), clients.getMessageCount(), true);

        // JSON
        clients = new KafkaClientsBuilder(clients)
            .withHeaders(Constants.APICURIO_VALUE_GROUP_ID + "=default, " + Constants.APICURIO_VALUE_ARTIFACT_ID + "=test-json-schema")
            .withMessage("{\"id\": 1, \"message\": \"verify-json\"}")
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(tcc.namespace(), clients.getProducerName(), clients.getMessageCount(), true);

        LOGGER.info("STOP");
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
        // // Prepare test environment
        // NamespaceUtils.prepareNamespace(tcc.namespace());
        // apicurioOperatorSetup = new ApicurioOperatorSetup(Constants.CO_NAMESPACE, tcc.namespace());
        // apicurioOperatorSetup.setup();
        //
        // ApicurioRegistry3Setup.setupMemIfNeeded(tcc.namespace(), tcc.apicurioRegistry3Name());
        //
        // KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        // ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName())
        //     .editSpec()
        //         .addNewSchemaRegistry()
        //             .withName(tcc.apicurioRegistry3Name())
        //                 .withUrl(ApicurioUtils.getApicurioRegistryApiUrl(tcc.namespace(), tcc.apicurioRegistry3Name()))
        //             .endSchemaRegistry()
        //     .endSpec()
        //     .build());

        // PwUtils.login(tcc);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
        apicurioOperatorSetup.teardown();
    }
}
