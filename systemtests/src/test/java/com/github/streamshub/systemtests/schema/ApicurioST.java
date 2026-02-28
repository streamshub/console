package com.github.streamshub.systemtests.schema;

import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.locators.CssBuilder;
import com.github.streamshub.systemtests.locators.MessagesPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.apicurio.ApicurioOperatorSetup;
import com.github.streamshub.systemtests.setup.apicurio.ApicurioRegistry3Setup;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
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
import com.github.streamshub.systemtests.utils.resourceutils.apicurio.ApicurioUtils;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.client.common.RegistryClientOptions;
import io.apicurio.registry.rest.client.RegistryClient;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;


public class ApicurioST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioST.class);

    protected TestCaseConfig tcc;
    protected ApicurioOperatorSetup apicurioOperatorSetup;

    // For test purposes we add only one artifact version, so we'll be using default id 1
    private static final String DEFAULT_ARTIFACT_VERSION = "1";

    private static final String SERIALIZED_SCHEMA_NAME = "TestMessage";

    private static final String PROTOBUF_SCHEMA = """
        syntax = "proto3";
        message %s {
          int32 id = 1;
          string message = 2;
        }
        """.formatted(SERIALIZED_SCHEMA_NAME);

    private static final String AVRO_SCHEMA = """
        {
          "type": "record",
          "name": "%s",
          "fields": [
            {"name": "id", "type": "int"},
            {"name": "message", "type": "string"}
          ]
        }
        """.formatted(SERIALIZED_SCHEMA_NAME);

    private static final String JSON_SCHEMA = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "id": {"type": "integer"},
            "message": {"type": "string"}
          },
          "required": ["id", "message"]
        }
        """;

    private static final String XML_SCHEMA = """
        <?xml version="1.0" encoding="UTF-8"?>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <xs:element name="%s">
            <xs:complexType>
              <xs:sequence>
                <xs:element name="id" type="xs:integer"/>
                <xs:element name="message" type="xs:string"/>
              </xs:sequence>
            </xs:complexType>
          </xs:element>
        </xs:schema>
        """.formatted(SERIALIZED_SCHEMA_NAME);;

    private static final String XML_MESSAGE = """
        <TestMessage>
          <id>1</id>
          <message>verify-xml</message>
        </TestMessage>
        """;

    private static final String MESSAGE = "{\"id\": 1, \"message\": \"verify-schema-%s\"}";

    private static Stream<Arguments> serializedSchemaScenarios() {
        return Stream.of(
            Arguments.of(Constants.SERIALIZER_PROTOBUF, Constants.ARTIFACT_TYPE_PROTOBUF, SERIALIZED_SCHEMA_NAME, PROTOBUF_SCHEMA,
                Constants.ARTIFACT_TYPE_PROTOBUF.toUpperCase(Locale.ROOT), MESSAGE.formatted(Constants.ARTIFACT_TYPE_PROTOBUF), Constants.CONTENT_TYPE_PROTOBUF),

            Arguments.of(null, Constants.ARTIFACT_TYPE_AVRO, AVRO_SCHEMA, SERIALIZED_SCHEMA_NAME,
                Constants.ARTIFACT_TYPE_AVRO.toUpperCase(Locale.ROOT), MESSAGE.formatted(Constants.ARTIFACT_TYPE_AVRO), Constants.CONTENT_TYPE_JSON)
            );
    }

    @ParameterizedTest
    @MethodSource("serializedSchemaScenarios")
    void testApicurioSerializedSchema(String serializer, String schemaType, String schemaName, String schema, String artifactType, String message, String contentType) {
        String topicName = "schema-topic-" + schemaType;
        String artifactId = topicName + "-value";
        String registryUrl = ApicurioUtils.getApicurioRegistryUrl(tcc.namespace(), tcc.apicurioRegistry3Name());

        RegistryClientOptions options = RegistryClientOptions.create(registryUrl);
        RegistryClient client = RegistryClientFactory.create(options);

        // Create topic
        KubeResourceManager.get().createResourceWithWait(
            KafkaTopicUtils.defaultTopic(tcc.namespace(), tcc.kafkaName(), topicName, 1, 1, 1).build());

        // Register schema
        ApicurioUtils.createArtifact(client, Constants.APICURIO_DEFAULT_GROUP, artifactId, artifactType, schema, contentType);

        // Produce messages
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicName)
            .withMessageCount(1)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage(message)
            .withHeaders(
                Constants.APICURIO_VALUE_GROUP_ID + "=" + Constants.APICURIO_DEFAULT_GROUP
                + ", " +
                Constants.APICURIO_VALUE_ARTIFACT_ID + "=" + artifactId
            )
            .withAdditionalConfig(
                ApicurioUtils.getApicurioProducerConfig(registryUrl, serializer, artifactId,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)
                )
            )
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(tcc.namespace(), clients.getProducerName(), clients.getMessageCount(), true);

        String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
        String contentId = ApicurioUtils.getArtifactContentId(client, Constants.APICURIO_DEFAULT_GROUP, artifactId, DEFAULT_ARTIFACT_VERSION);
        String schemaLink = PwPageUrls.getSchemaLink(tcc.apicurioRegistry3Name(), contentId, Constants.APICURIO_DEFAULT_GROUP, schemaName);

        tcc.page().navigate(PwPageUrls.getSingleTopicPage(tcc, tcc.kafkaName(), topicId));

        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.getTableRowItem(1, 1));
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_NAME, schemaName, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_VALUE_FORMAT, artifactType, true);
        PwUtils.waitForAttributeContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_LINK, schemaLink, CssBuilder.HREF, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_CODE, PwUtils.getTrimmedText(schema), true);

        // Content Id depends on how much artifacts are present in the registry - just like globalId
        tcc.page().navigate(PwPageUrls.getSchemaPage(tcc, tcc.apicurioRegistry3Name(), contentId, Constants.APICURIO_DEFAULT_GROUP, schemaName));

        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_SCHEMA_PAGE_HEADER, schemaName, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_SCHEMA_PAGE_CODE, PwUtils.getTrimmedText(schema), true);

        // Delete artifact
        client.groups().byGroupId(Constants.APICURIO_DEFAULT_GROUP).artifacts().byArtifactId(artifactId).delete();
    }


    private static Stream<Arguments> schemaRawTypes() {
        return Stream.of(
            Arguments.of(Constants.ARTIFACT_TYPE_JSON, JSON_SCHEMA, Constants.PLAIN_VALUE_TYPE,
                Constants.ARTIFACT_TYPE_JSON.toUpperCase(Locale.ROOT), MESSAGE.formatted(Constants.ARTIFACT_TYPE_JSON), Constants.CONTENT_TYPE_JSON),

            Arguments.of(Constants.ARTIFACT_TYPE_XML, XML_SCHEMA, Constants.PLAIN_VALUE_TYPE,
                 Constants.ARTIFACT_TYPE_XML.toUpperCase(Locale.ROOT), XML_MESSAGE, Constants.CONTENT_TYPE_XML)
        );
    }
    @ParameterizedTest
    @MethodSource("schemaRawTypes")
    void testApicurioSchemaRegistryRawTypes(String schemaType, String schema, String valueFormat, String artifactType, String message, String contentType) {
        String topicName = "schema-topic-" + schemaType;
        String artifactId = topicName + "-value";
        String registryUrl = ApicurioUtils.getApicurioRegistryUrl(tcc.namespace(), tcc.apicurioRegistry3Name());

        RegistryClientOptions options = RegistryClientOptions.create(registryUrl);
        RegistryClient client = RegistryClientFactory.create(options);

        // Create topic
        KubeResourceManager.get().createResourceWithWait(
            KafkaTopicUtils.defaultTopic(tcc.namespace(), tcc.kafkaName(), topicName, 1, 1, 1).build());

        // Register schema
        ApicurioUtils.createArtifact(client, Constants.APICURIO_DEFAULT_GROUP, artifactId, artifactType, schema, contentType);

        // Produce messages
        KafkaClients clients = new KafkaClientsBuilder()
            .withNamespaceName(tcc.namespace())
            .withTopicName(topicName)
            .withMessageCount(1)
            .withDelayMs(0)
            .withProducerName(KafkaNamingUtils.producerName(topicName))
            .withConsumerName(KafkaNamingUtils.consumerName(topicName))
            .withConsumerGroup(KafkaNamingUtils.consumerGroupName(topicName))
            .withBootstrapAddress(KafkaUtils.getPlainScramShaBootstrapAddress(tcc.kafkaName()))
            .withUsername(tcc.kafkaUserName())
            .withMessage(message)
            .withHeaders(
                Constants.APICURIO_VALUE_GROUP_ID + "=" + Constants.APICURIO_DEFAULT_GROUP
                + ", " +
                Constants.APICURIO_VALUE_ARTIFACT_ID + "=" + artifactId
            )
            .withAdditionalConfig(
                ApicurioUtils.getApicurioProducerConfig(registryUrl, null, artifactId,
                    KafkaClientsUtils.getScramShaConfig(tcc.namespace(), tcc.kafkaUserName(), SecurityProtocol.SASL_PLAINTEXT)
                )
            )
            .build();

        KubeResourceManager.get().createResourceWithWait(clients.producer());
        WaitUtils.waitForClientSuccess(tcc.namespace(), clients.getProducerName(), clients.getMessageCount(), true);

        String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
        tcc.page().navigate(PwPageUrls.getSingleTopicPage(tcc, tcc.kafkaName(), topicId));

        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.getTableRowItem(1, 1));
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_VALUE_FORMAT, valueFormat, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_PLAIN_CODE, PwUtils.getTrimmedText(message), true);

        // Delete artifact
        client.groups().byGroupId(Constants.APICURIO_DEFAULT_GROUP).artifacts().byArtifactId(artifactId).delete();
    }

    @BeforeAll
    void testClassSetup() {
        // Init test case config based on the test context
        tcc = Utils.getTestCaseConfig();
        // Prepare test environment
        NamespaceUtils.prepareNamespace(tcc.namespace());
        apicurioOperatorSetup = new ApicurioOperatorSetup(Constants.CO_NAMESPACE, tcc.namespace());
        apicurioOperatorSetup.setup();

        ApicurioRegistry3Setup.setupMemIfNeeded(tcc.namespace(), tcc.apicurioRegistry3Name());

        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
        ConsoleInstanceSetup.setupIfNeeded(ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), tcc.consoleInstanceName(), tcc.kafkaName(), tcc.kafkaUserName())
            .editSpec()
                .addNewSchemaRegistry()
                    .withName(tcc.apicurioRegistry3Name())
                        .withUrl(ApicurioUtils.getApicurioRegistryUrlWithApi(tcc.namespace(), tcc.apicurioRegistry3Name()))
                .endSchemaRegistry()
                .editFirstKafkaCluster()
                    .withSchemaRegistry(tcc.apicurioRegistry3Name())
                .endKafkaCluster()
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
