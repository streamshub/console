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
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.apicurio.ApicurioUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaClientsUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaTopicUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaUtils;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.client.common.RegistryClientOptions;
import io.apicurio.registry.rest.client.RegistryClient;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class ApicurioST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioST.class);

    protected TestCaseConfig tcc;
    protected ApicurioOperatorSetup apicurioOperatorSetup;

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
        """.formatted(SERIALIZED_SCHEMA_NAME);

    private static final String XML_MESSAGE = """
        <TestMessage>
          <id>1</id>
          <message>verify-xml</message>
        </TestMessage>
        """;

    private static final String MESSAGE = "{\"id\": 1, \"message\": \"verify-schema-%s\"}";

    private static Stream<Arguments> schemasWithExtraSchemaPageScenarios() {
        return Stream.of(
            Arguments.of(
                Constants.SERIALIZER_AVRO,
                Constants.ARTIFACT_TYPE_AVRO,
                SERIALIZED_SCHEMA_NAME,
                AVRO_SCHEMA,
                Constants.ARTIFACT_TYPE_AVRO.toUpperCase(Locale.ROOT),
                MESSAGE.formatted(Constants.ARTIFACT_TYPE_AVRO),
                Constants.CONTENT_TYPE_JSON),
            Arguments.of(
                Constants.SERIALIZER_PROTOBUF,
                Constants.ARTIFACT_TYPE_PROTOBUF,
                SERIALIZED_SCHEMA_NAME,
                PROTOBUF_SCHEMA,
                Constants.ARTIFACT_TYPE_PROTOBUF.toUpperCase(Locale.ROOT),
                MESSAGE.formatted(Constants.ARTIFACT_TYPE_PROTOBUF),
                Constants.CONTENT_TYPE_PROTOBUF)
        );
    }

    /**
     * Verifies that serialized messages produced with Apicurio Registry schemas
     * are correctly displayed and linked in the Console UI.
     *
     * <p>This parameterized test covers multiple serialization scenarios
     * (Avro, Protobuf) using different serializers and schema types.</p>
     *
     * <p>The test performs the following steps:
     * <ul>
     *     <li>Creates a Kafka topic.</li>
     *     <li>Registers a schema artifact in Apicurio Registry.</li>
     *     <li>Produces a message using the configured serializer and registry settings.</li>
     *     <li>Verifies in the Topic Messages UI:
     *         <ul>
     *             <li>Schema name is displayed correctly.</li>
     *             <li>Value format matches the artifact type.</li>
     *             <li>Schema link points to the correct registry location.</li>
     *             <li>Schema definition is rendered correctly.</li>
     *         </ul>
     *     </li>
     *     <li>Navigates to the Schema page and verifies its content.</li>
     *     <li>Cleans up by deleting the created artifact.</li>
     * </ul>
     *
     * @param serializer   the value serializer class used by the producer
     * @param schemaType   the logical schema type (used for topic naming)
     * @param schemaName   the expected schema name displayed in the UI
     * @param schema       the schema definition content
     * @param artifactType the Apicurio artifact type (e.g. AVRO, JSON, PROTOBUF)
     * @param message      the message payload to produce
     * @param contentType  the registry content type for the artifact
     */
    @ParameterizedTest
    @MethodSource("schemasWithExtraSchemaPageScenarios")
    void testApicurioSchemasWithExtraSchemaPage(String serializer, String schemaType, String schemaName, String schema, String artifactType, String message, String contentType) {
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
        LOGGER.info("Producing test message to topic {} using serializer {}", topicName, serializer);
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

        LOGGER.info("Verifying message and schema in topic page for {}", topicName);
        String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);

        tcc.page().navigate(PwPageUrls.getSingleTopicPage(tcc, tcc.kafkaName(), topicId));

        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.getTableRowItem(1, 1));
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_NAME, schemaName, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_VALUE_FORMAT, artifactType, true);
        String schemaLink = tcc.page().locator(MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_LINK).getAttribute(CssBuilder.HREF);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_CODE, PwUtils.getTrimmedText(schema), true);

        // Content Id depends on how much artifacts are present in the registry - just like globalId
        LOGGER.info("Navigating to schema page to verify artifact content for {}", artifactId);
        tcc.page().navigate(PwPageUrls.getConsoleUrl(tcc) + schemaLink);

        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_SCHEMA_PAGE_HEADER, schemaName, true);
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_SCHEMA_PAGE_CODE, PwUtils.getTrimmedText(schema), true);

        // Delete artifact
        LOGGER.info("Deleting artifact: {} from group: {}", artifactId, Constants.APICURIO_DEFAULT_GROUP);
        client.groups().byGroupId(Constants.APICURIO_DEFAULT_GROUP).artifacts().byArtifactId(artifactId).delete();
    }

    private static Stream<Arguments> schemasWithoutExtraSchemaPageScenarios() {
        return Stream.of(
            Arguments.of(
                Constants.SERIALIZER_JSON,
                Constants.ARTIFACT_TYPE_JSON,
                JSON_SCHEMA,
                Constants.PLAIN_VALUE_TYPE,
                Constants.ARTIFACT_TYPE_JSON.toUpperCase(Locale.ROOT),
                MESSAGE.formatted(Constants.ARTIFACT_TYPE_JSON),
                Constants.CONTENT_TYPE_JSON),
            Arguments.of(
                null,
                Constants.ARTIFACT_TYPE_XML,
                XML_SCHEMA,
                Constants.PLAIN_VALUE_TYPE,
                Constants.ARTIFACT_TYPE_XML.toUpperCase(Locale.ROOT),
                XML_MESSAGE,
                Constants.CONTENT_TYPE_XML)
        );
    }

    /**
     * Verifies that messages produced with raw schema types in Apicurio Registry
     * are correctly displayed in the Console UI for Kafka topics.
     *
     * <p>This parameterized test covers multiple raw schema scenarios (e.g., plain JSON, strings, integers)
     * and ensures that the message value and schema type are correctly rendered in the topic messages sidebar.</p>
     *
     * <p>The test performs the following steps:
     * <ul>
     *     <li>Creates a Kafka topic for the given schema type.</li>
     *     <li>Registers a raw schema artifact in Apicurio Registry.</li>
     *     <li>Produces a single message using the registered artifact.</li>
     *     <li>Verifies in the Topic Messages UI:
     *         <ul>
     *             <li>The value format matches the expected schema type.</li>
     *             <li>The message content matches the raw schema value.</li>
     *         </ul>
     *     </li>
     *     <li>Cleans up by deleting the created artifact from the registry.</li>
     * </ul>
     *
     * @param schemaType   the logical schema type used to create the topic
     * @param schema       the schema definition or content
     * @param valueFormat  the expected format displayed in the UI (e.g., JSON, STRING)
     * @param artifactType the Apicurio artifact type (e.g., AVRO, JSON, PROTOBUF)
     * @param message      the raw message payload to produce
     * @param contentType  the registry content type for the artifact
     */
    @ParameterizedTest
    @MethodSource("schemasWithoutExtraSchemaPageScenarios")
    void testApicurioSchemasWithoutExtraSchemaPage(String serializer, String schemaType, String schema, String valueFormat, String artifactType, String message, String contentType) {
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
        LOGGER.info("Producing test message to topic {}", topicName);
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

        LOGGER.info("Verifying message and schema in topic page for {}", topicName);
        String topicId = WaitUtils.waitForKafkaTopicToHaveIdAndReturn(tcc.namespace(), topicName);
        tcc.page().navigate(PwPageUrls.getSingleTopicPage(tcc, tcc.kafkaName(), topicId));

        PwUtils.waitForLocatorAndClick(tcc, MessagesPageSelectors.getTableRowItem(1, 1));
        PwUtils.waitForContainsText(tcc, MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_VALUE_FORMAT, valueFormat, true);

        String currentMessage = tcc.page().locator(MessagesPageSelectors.MPS_MESSAGE_SIDEBAR_SCHEMA_FORMATED_PLAIN)
            .getAttribute(Constants.VALUE_ATTRIBUTE)
            .replaceAll("\\s", "")
            .toLowerCase(Locale.ROOT);

        String expectedMessage = PwUtils.getTrimmedText(message)
            .replaceAll("\\s", "")
            .toLowerCase(Locale.ROOT);

        assertTrue(currentMessage.contains(expectedMessage));

        // Delete artifact
        LOGGER.info("Deleting artifact: {} from group: {}", artifactId, Constants.APICURIO_DEFAULT_GROUP);
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
                        .withUrl(ApicurioUtils.getApicurioRegistryUrl(tcc.namespace(), tcc.apicurioRegistry3Name()))
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
    }
}
