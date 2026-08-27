package com.github.streamshub.console.api;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.Uuid;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.serdes.MultiformatDeserializer;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.LogCapture;
import com.github.streamshub.console.test.RecordHelper;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;

import io.apicurio.registry.resolver.client.RegistryClientFacade;
import io.apicurio.registry.types.ArtifactType;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.ValidatableResponse;
import io.strimzi.api.kafka.model.kafka.Kafka;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestHTTPEndpoint(RecordsResource.class)
@TestProfile(TestPlainProfile.class)
class RecordsResourceIT {

    static LogCapture logCapture = LogCapture.with(logRecord -> logRecord
            .getLoggerName()
            .equals(MultiformatDeserializer.class.getName()),
            Level.FINEST);

    @Inject
    Config config;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> kafkaContexts;

    @Inject
    KubernetesClient client;

    TestHelper utils;
    TopicHelper topicUtils;
    RecordHelper recordUtils;
    String clusterId1;
    String clusterId3;
    String clusterIdY = "test-kafkaY";

    @BeforeAll
    static void initialize() {
        logCapture.register();
    }

    @AfterAll
    static void cleanup() {
        logCapture.deregister();
    }

    @AfterEach
    void cleanupAfterEach() {
        if (recordUtils != null) {
            recordUtils.close();
        }
    }

    @BeforeEach
    void setup() {
        logCapture.records().clear();
        URI bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));

        topicUtils = new TopicHelper(bootstrapServers, config);
        topicUtils.deleteAllTopics();

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        recordUtils = new RecordHelper(bootstrapServers, config);

        client.resources(Kafka.class).inAnyNamespace().delete();

        utils.apply(client, utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers));
        // test-kafka3 simply proxies to test-kafka1, but we can associate a different schema registry with it
        clusterId3 = UUID.randomUUID().toString();
        utils.apply(client, utils.buildKafkaResource("test-kafka3", clusterId3, bootstrapServers));

        // Wait for the context map to be populated with all Kafka configurations
        await().atMost(10, TimeUnit.SECONDS).until(() -> kafkaContexts.values()
                .stream()
                .map(KafkaContext::clusterConfig)
                .map(KafkaClusterConfig::getName)
                .toList()
                .containsAll(List.of("test-kafka1", "test-kafka3")));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
    }

    private String getClusterId(String kafkaName) {
        return switch (kafkaName) {
            case "test-kafka1" -> clusterId1;
            case "test-kafka3" -> clusterId3;
            default -> throw new IllegalArgumentException(kafkaName);
        };
    }

    private RegistryClientFacade getRegistryClient(String kafkaName) {
        return kafkaContexts.get(getClusterId(kafkaName))
                .schemaRegistryContext().registryClient();
    }

    private ValidatableResponse getRecord(String topicId, int partition, long offset) {
        return whenRequesting(req -> req.get("{partition}/{offset}", clusterId1, topicId, partition, offset));
    }

    private void assertNotFound(ValidatableResponse response) {
        response.assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

    @Test
    void testGetRecord() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        for (int i = 0; i < 3; i++) {
            recordUtils.produceRecord(topicName, 1, null, Map.of("h1", "h1-value-" + i), "the-key-" + i, "the-value-" + i);
        }

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 3);

        getRecord(topicIds.get(topicName), 1, 1)
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .header(HttpHeaders.CACHE_CONTROL, is("no-store"))
            .body("data.type", is("records"))
            .body("data.attributes.partition", is(1))
            .body("data.attributes.offset", is(1))
            .body("data.attributes.key", is("the-key-1"))
            .body("data.attributes.value", is("the-value-1"))
            .body("data.attributes.headers.h1", is("h1-value-1"));
    }

    @Test
    void testGetRecordFromInvalidTopic() {
        assertNotFound(getRecord(Uuid.randomUuid().toString(), 0, 0));
    }

    @Test
    void testGetRecordFromNonexistentPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);
        recordUtils.produceRecord(topicName, 0, null, null, "the-key", "the-value");

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        assertNotFound(getRecord(topicIds.get(topicName), 2, 0));
    }

    @Test
    void testGetRecordFromEmptyPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        assertNotFound(getRecord(topicIds.get(topicName), 0, 0));
    }

    @ParameterizedTest
    @CsvSource({
        "-1,  0",
        " 0, -1",
        " 0,  1", // beyond the end of the partition
        " 0, 10",
    })
    void testGetRecordWithMissingPartitionOrOffset(int partition, long offset) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        recordUtils.produceRecord(topicName, null, null, "the-key", "the-value");

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        assertNotFound(getRecord(topicIds.get(topicName), partition, offset));
    }

    @Test
    void testGetRecordIncludeOnlyHeaders() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        recordUtils.produceRecord(topicName, null, Map.of("h1", "h1-value"), "the-key", "the-value");

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        whenRequesting(req -> req
                .queryParam("fields[records]", "headers")
                .get("{partition}/{offset}", clusterId1, topicIds.get(topicName), 0, 0))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", allOf(
                    is(aMapWithSize(3)),
                    hasEntry("type", "records"),
                    hasKey("attributes"),
                    hasKey("relationships")))
            .body("data.attributes", is(aMapWithSize(1)))
            .body("data.attributes.headers.h1", is("h1-value"));
    }

    @Test
    void testGetRecordWithEmptyValue() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        recordUtils.produceRecord(topicName, null, null, null, "");

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        getRecord(topicIds.get(topicName), 0, 0)
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.value", is(equalTo("")));
    }

    @Test
    void testGetRecordWithBinaryValue() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        // Large binary values are never omitted or truncated when fetching a single record
        final byte[] data = new byte[512];
        SecureRandom.getInstanceStrong().nextBytes(data);
        data[511] = (byte) 0xFF; // ensure at least one byte invalid UTF-8

        recordUtils.produceRecord(topicName, null, null, null, data);

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        getRecord(topicIds.get(topicName), 0, 0)
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.value", is(equalTo(Base64.getEncoder().encodeToString(data))))
            .body("data.meta.content.value.type", is(equalTo("application/octet-stream")))
            .body("data.meta.content.value.encoding", is(equalTo("base64")));
    }

    @Test
    void testGetRecordWithBinaryHeader() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        final byte[] binaryHeaderValue = new byte[8];
        SecureRandom.getInstanceStrong().nextBytes(binaryHeaderValue);
        binaryHeaderValue[7] = (byte) 0xFF; // ensure at least one byte invalid UTF-8

        recordUtils.produceRecord(topicName, null, Map.<String, Object>of("binHeader", binaryHeaderValue), null, (byte[]) null);

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        getRecord(topicIds.get(topicName), 0, 0)
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.headers.binHeader", is(equalTo(Base64.getEncoder().encodeToString(binaryHeaderValue))))
            .body("data.meta.content.headers.binHeader.type", is(equalTo("application/octet-stream")))
            .body("data.meta.content.headers.binHeader.encoding", is(equalTo("base64")));
    }

    @Test
    void testGetRecordWithLongValuesNotTruncated() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        String h1Value = "h".repeat(5000);
        String key = "k".repeat(5000);
        String value = "v".repeat(5000);
        recordUtils.produceRecord(topicName, null, Map.of("h1", h1Value), key, value);

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= 1);

        getRecord(topicIds.get(topicName), 0, 0)
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.headers.h1", is(equalTo(h1Value)))
            .body("data.attributes.key", is(equalTo(key)))
            .body("data.attributes.value", is(equalTo(value)))
            .body("data.meta", is(nullValue()));
    }

    @Test
    void testGetRecordDeletedBeforeBeginning() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        Stream.of("first", "second", "third", "fourth")
            .forEach(msg -> recordUtils.produceRecord(topicName, null, null, null, msg));

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 4);

        topicUtils.deleteRecords(topicName, 0, 3);

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 1);

        // The deleted records are not substituted with the first available record
        assertNotFound(getRecord(topicIds.get(topicName), 0, 0));

        getRecord(topicIds.get(topicName), 0, 3)
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.offset", is(equalTo(3)))
            .body("data.attributes.value", is(equalTo("fourth")));
    }

    @Test
    void testGetRecordMagicByteWithoutRegistry() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        var value = "\u0000rest of value";
        recordUtils.produceRecord(topicName, null, null, null, value);

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 1);

        whenRequesting(req -> req
                // Kafka Y has no associated registry
                .get("{partition}/{offset}", clusterIdY, topicIds.get(topicName), 0, 0))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.offset", is(equalTo(0)))
            // The value is treated as binary data
            .body("data.attributes.value", is(equalTo(Base64.getEncoder().encodeToString(value.getBytes()))));
    }

    @Test
    void testGetRecordWithSchemaNotFound() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        var value = "\u0000rest of value";
        recordUtils.produceRecord(topicName, null, null, null, value);

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 1);

        getRecord(topicIds.get(topicName), 0, 0)
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.offset", is(equalTo(0)))
            // The value is treated as binary data
            .body("data.attributes.value", is(equalTo(Base64.getEncoder().encodeToString(value.getBytes()))));

        var logs = logCapture.records();

        assertThat(logs, hasItem(hasProperty("message", containsString(
                // 1919251316 = 64-bit long integer from bytes of `rest` string
                "Message: No content with id/hash 'contentId-1919251316' was found"
        ))));
    }

    @Test
    void testProduceRecordSimple() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("value", "TEST")))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .header(HttpHeaders.LOCATION, endsWith("/topics/" + topicIds.get(topicName) + "/records/0/0"))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.attributes.value", is("TEST"));
    }

    @Test
    void testProduceRecordSimpleWithPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("partition", 1)
                                        .add("value", "TEST")))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(1))
            .body("data.attributes.offset", is(0))
            .body("data.attributes.value", is("TEST"));
    }

    @ParameterizedTest
    @CsvSource({
        "'hdr1', ", // null value
        "'hdr2', 'hdr2Value'",
        "'hdr3', ''",
    })
    void testProduceRecordWithHeaders(String headerName, String headerValue) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("value", "TEST")
                                        .add("headers", Json.createObjectBuilder()
                                                .add(headerName,
                                                     headerValue == null
                                                         ? JsonValue.NULL
                                                         : Json.createValue(headerValue)))))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.attributes.value", is("TEST"))
            .body("data.attributes.headers." + headerName, is(headerValue));
    }

    @Test
    void testProduceRecordWithInvalidPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "records")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("partition", 1) // only partition 0 is valid
                                        .add("value", "TEST")))
                        .build()
                        .toString())
                .post("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("400"))
            .body("errors[0].code", is("4003"))
            .body("errors[0].source.pointer", is("/data/attributes/partition"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "test-kafka1", "test-kafka3" })
    void testProduceRecordWithAvroFormat(String kafkaName) {
        String clusterId = getClusterId(kafkaName);
        var registryClient = getRegistryClient(kafkaName);

        final String keyArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String keySchema = """
              {
                "namespace": "console.avro",
                "type": "record",
                "name": "name_%s",
                "fields": [
                  {
                    "name": "key1",
                    "type": "string"
                  }
                ]
              }
              """
              .formatted(keyArtifactId);

        final String valueArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String valueSchema = """
              {
                "namespace": "console.avro",
                "type": "record",
                "name": "name_%s",
                "fields": [
                  {
                    "name": "value1",
                    "type": "string"
                  }
                ]
              }
              """
              .formatted(valueArtifactId);

        registryClient.createSchema(
                ArtifactType.AVRO,
                "default",
                keyArtifactId,
                "1.0.0",
                "FAIL",
                true,
                keySchema,
                Collections.emptySet()
        );

        registryClient.createSchema(
                ArtifactType.AVRO,
                "default",
                valueArtifactId,
                "1.0.0",
                "FAIL",
                true,
                valueSchema,
                Collections.emptySet()
        );

        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        JsonObject requestBody = Json.createObjectBuilder()
                .add("data", Json.createObjectBuilder()
                        .add("type", "records")
                        .add("relationships", Json.createObjectBuilder()
                                .add("keySchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                .add("coordinates", "default:" + keyArtifactId + ":1.0.0")))
                                .add("valueSchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                .add("coordinates", "default:" + valueArtifactId + ":1.0.0"))))
                        .add("attributes", Json.createObjectBuilder()
                                .add("key", """
                                        {
                                          "key1": "value-of-key1",
                                          "field2": "field-not-in-the-schema"
                                        }""")
                                .add("value", """
                                        {
                                          "value1": "value-of-value1",
                                          "field2": "field-not-in-the-schema"
                                        }""")))
                .build();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(requestBody.toString())
                .post("", clusterId, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.relationships.keySchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data.relationships.keySchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + keyArtifactId))
            .body("data.relationships.valueSchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data.relationships.valueSchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + valueArtifactId));

        var recordsResponse = whenRequesting(req -> req.get("{partition}/{offset}", clusterId, topicIds.get(topicName), 0, 0))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.relationships.keySchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data.relationships.keySchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + keyArtifactId))
            .body("data.relationships.valueSchema.meta.artifactType", is(ArtifactType.AVRO))
            .body("data.relationships.valueSchema.meta.name",
                    // fully qualified
                    is("console.avro.name_" + valueArtifactId))
            .body("data.attributes.key", is("""
                    {"key1":"value-of-key1"}"""))
            .body("data.attributes.value", is("""
                    {"value1":"value-of-value1"}"""))
            .extract();

        assertSchemaContent(keySchema, valueSchema, recordsResponse, (exp, act) -> {
            JSONAssert.assertEquals(exp, act, true);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "test-kafka1", "test-kafka3" })
    void testProduceRecordWithProtobufFormat(String kafkaName) {
        String clusterId = getClusterId(kafkaName);
        var registryClient = getRegistryClient(kafkaName);

        final String keyArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String keySchema = """
                message name_%s {
                optional string key1 = 1;
              }
              """
              .formatted(keyArtifactId);

        final String valueArtifactId = UUID.randomUUID().toString().replace("-", "");
        final String valueSchema = """
                message some_other_name_%s {
                optional string field1 = 1;
              }
              message name_%s {
                optional string value1 = 1;
              }
              """
              .formatted(valueArtifactId, valueArtifactId);

        registryClient.createSchema(
                ArtifactType.PROTOBUF,
                "default",
                keyArtifactId,
                "1.0.0",
                "FAIL",
                true,
                keySchema,
                Collections.emptySet()
        );

        registryClient.createSchema(
                ArtifactType.PROTOBUF,
                "default",
                valueArtifactId,
                "1.0.0",
                "FAIL",
                true,
                valueSchema,
                Collections.emptySet()
        );

        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        JsonObject requestBody = Json.createObjectBuilder()
                .add("data", Json.createObjectBuilder()
                        .add("type", "records")
                        .add("relationships", Json.createObjectBuilder()
                                .add("keySchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                // messageType omitted since there is only 1 in the key schema
                                                .add("coordinates", "default:" + keyArtifactId + ":1.0.0")))
                                .add("valueSchema", Json.createObjectBuilder()
                                        .add("meta", Json.createObjectBuilder()
                                                .add("coordinates", "default:" + valueArtifactId + ":1.0.0")
                                                .add("messageType", "name_" + valueArtifactId))))
                        .add("attributes", Json.createObjectBuilder()
                                .add("key", """
                                        {
                                          "key1": "value-of-key1",
                                          "field2": "field-not-in-the-schema"
                                        }""")
                                .add("value", """
                                        {
                                          "value1": "value-of-value1",
                                          "field2": "field-not-in-the-schema"
                                        }""")))
                .build();

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(requestBody.toString())
                .post("", clusterId, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.CREATED.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.relationships.keySchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data.relationships.keySchema.meta.name", is("name_" + keyArtifactId))
            .body("data.relationships.valueSchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data.relationships.valueSchema.meta.name", is("name_" + valueArtifactId));

        var recordsResponse = whenRequesting(req -> req.get("{partition}/{offset}", clusterId, topicIds.get(topicName), 0, 0))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.partition", is(0))
            .body("data.attributes.offset", is(0))
            .body("data.relationships.keySchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data.relationships.keySchema.meta.name", is("name_" + keyArtifactId))
            .body("data.attributes.key", is("""
                    {"key1":"value-of-key1"}"""))
            .body("data.relationships.valueSchema.meta.artifactType", is(ArtifactType.PROTOBUF))
            .body("data.relationships.valueSchema.meta.name", is("name_" + valueArtifactId))
            .body("data.attributes.value", is("""
                    {"value1":"value-of-value1"}"""))
            .extract();

        assertSchemaContent(keySchema, valueSchema, recordsResponse, Assertions::assertEquals);
    }

    private void assertSchemaContent(
            String expectedKeySchema,
            String expectedValueSchema,
            ExtractableResponse<?> response,
            BiConsumer<String, String> assertion) {
        String keySchemaLink = response.jsonPath().getString("data.relationships.keySchema.links.content");

        String actualKeySchema = RestAssured.given()
            .basePath(keySchemaLink)
            .when().get()
            .then()
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .extract()
            .asString();

        assertion.accept(expectedKeySchema, actualKeySchema);

        String valueSchemaLink = response.jsonPath().getString("data.relationships.valueSchema.links.content");

        String actualValueSchema = RestAssured.given()
            .basePath(valueSchemaLink)
            .when().get()
            .then()
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .extract()
            .asString();

        assertion.accept(expectedValueSchema, actualValueSchema);
    }
}
