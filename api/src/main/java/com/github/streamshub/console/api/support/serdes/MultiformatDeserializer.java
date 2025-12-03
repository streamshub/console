package com.github.streamshub.console.api.support.serdes;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaLookupResult;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.client.RegistryClientFacade;
import io.apicurio.registry.resolver.strategy.ArtifactReference;
import io.apicurio.registry.serde.BaseSerde;
import io.apicurio.registry.serde.config.BaseKafkaSerDeConfig;
import io.apicurio.registry.serde.config.SerdeConfig;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

/**
 * Deserializer that supports reading Avro, Protobuf, and raw bytes.
 *
 * If an Apicurio Registry client is provided, the deserializer attempts to find
 * a schema using an identifier found in the message or its headers. If an
 * identifier is found and a schema is also found, either Avro or Protobuf
 * deserialization will take place by delegating to the Apicurio deserializer
 * for each type.
 *
 * Otherwise, the data will be returned as-is to the client of the Consumer
 * using this deserializer. Warning information will be provided for the reason
 * a raw message is returned if the deserializer detects the presence of a
 * schema identifier.
 */
public class MultiformatDeserializer extends MultiformatSerdeBase implements Deserializer<RecordData> {

    private static final Logger LOG = Logger.getLogger(MultiformatDeserializer.class);

    private static final SchemaLookupResult<Object> NO_SCHEMA_ID = SchemaLookupResult.builder().build();
    private static final SchemaLookupResult<Object> RESOLVER_MISSING = SchemaLookupResult.builder().build();
    private static final SchemaLookupResult<Object> LOOKUP_FAILURE = SchemaLookupResult.builder().build();

    AvroDeserializer avroDeserializer;
    ProtobufDeserializer protobufDeserializer;

    public MultiformatDeserializer(RegistryClientFacade client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override
    protected AutoCloseable createAvroSerde(SchemaResolver<Schema, RecordData> resolver) {
        this.avroDeserializer = new AvroDeserializer(resolver);
        return avroDeserializer;
    }

    @Override
    protected AutoCloseable createProtobufSerde(SchemaResolver<ProtobufSchema, Message> resolver) {
        this.protobufDeserializer = new ProtobufDeserializer(resolver);
        return protobufDeserializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (getSchemaResolver() == null) {
            super.key = isKey;
            // Do not attempt to configure anything more if we will not be making remote calls to registry
            return;
        }

        Map<String, Object> avroConfigs = new HashMap<>(configs);
        avroDeserializer.configure(new SerdeConfig(avroConfigs), isKey);
        avroDeserializer.setAvroDatumProvider(new AvroDatumProvider());

        Map<String, Object> protobufConfigs = new HashMap<>(configs);
        protobufConfigs.put(SerdeConfig.DESERIALIZER_SPECIFIC_KEY_RETURN_CLASS, DynamicMessage.class.getName());
        protobufConfigs.put(SerdeConfig.DESERIALIZER_SPECIFIC_VALUE_RETURN_CLASS, DynamicMessage.class.getName());
        protobufDeserializer.configure(new SerdeConfig(protobufConfigs), isKey);

        parser = new MultiformatSchemaParser<>(Set.of(
            cast(avroDeserializer.schemaParser()),
            cast(protobufDeserializer.schemaParser())
        ));

        baseSerde.configure(new SerdeConfig(configs), isKey, parser);

        super.configure(new BaseKafkaSerDeConfig(configs), isKey);
    }

    private RecordData readData(SchemaLookupResult<Object> schemaResult, ByteBuffer buffer, int start, int length) {
        Object parsedSchema = null;

        if (schemaResult != null && schemaResult.getParsedSchema() != null) {
            parsedSchema = schemaResult.getParsedSchema().getParsedSchema();
        }

        RecordData result;

        if (parsedSchema instanceof Schema) {
            result = readAvroData(schemaResult, buffer, start, length);
        } else if (parsedSchema instanceof ProtobufSchema) {
            result = readProtobufData(schemaResult, buffer, start, length);
        } else {
            result = readRawData(schemaResult, buffer, start, length);
        }

        return result;
    }

    private RecordData readAvroData(SchemaLookupResult<Object> schemaResult, ByteBuffer buffer, int start, int length) {
        ParsedSchema<Object> schema = schemaResult.getParsedSchema();
        Schema avroSchema = (Schema) schema.getParsedSchema();
        RecordData result;

        try {
            result = avroDeserializer.readData(cast(schema), buffer, start, length);
            result.meta.put("schema-type", ArtifactType.AVRO);
            result.meta.put("schema-id", ArtifactReferences.toSchemaId(schemaResult.toArtifactReference(), objectMapper));
            result.meta.put("schema-name", avroSchema.getFullName());
        } catch (Exception e) {
            result = new RecordData((byte[]) null);
            result.error = com.github.streamshub.console.api.model.jsonapi.JsonApiError.forThrowable(e, "Error deserializing Avro data");
        }

        return result;
    }

    private RecordData readProtobufData(SchemaLookupResult<Object> schemaResult, ByteBuffer buffer, int start, int length) {
        ParsedSchema<Object> schema = schemaResult.getParsedSchema();
        RecordData result;

        try {
            Message msg = protobufDeserializer.readData(cast(schema), buffer, start, length);
            byte[] data = com.google.protobuf.util.JsonFormat.printer()
                    .omittingInsignificantWhitespace()
                    .print(msg)
                    .getBytes();
            result = new RecordData(data);
            result.meta.put("schema-type", ArtifactType.PROTOBUF);
            result.meta.put("schema-id", ArtifactReferences.toSchemaId(schemaResult.toArtifactReference(), objectMapper));
            result.meta.put("schema-name", msg.getDescriptorForType().getFullName());
        } catch (Exception e) {
            result = new RecordData((byte[]) null);
            result.error = com.github.streamshub.console.api.model.jsonapi.JsonApiError.forThrowable(e, "Error deserializing Protobuf data");
        }

        return result;
    }

    private RecordData readRawData(SchemaLookupResult<Object> schemaResult, ByteBuffer buffer, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(buffer.array(), start, bytes, 0, length);
        RecordData result = new RecordData(bytes);

        if (schemaResult == RESOLVER_MISSING) {
            result.error = new com.github.streamshub.console.api.model.jsonapi.JsonApiError(
                    "Schema resolution error",
                    "%s encoded, but no schema registry is configured"
                        .formatted(isKey() ? "Key" : "Value"),
                    null);
        } else if (schemaResult == LOOKUP_FAILURE) {
            result.error = new com.github.streamshub.console.api.model.jsonapi.JsonApiError(
                    "Schema resolution error",
                    "Schema could not be retrieved from registry to decode %s"
                        .formatted(isKey() ? "Key" : "Value"),
                    null);
        }

        return result;
    }

    @Override
    public RecordData deserialize(String topic, byte[] data) {
        return deserialize(topic, null, data);
    }

    @Override
    public RecordData deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) {
            return null;
        }

        ArtifactReference artifactReference = null;

        if (headersHandler != null && headers != null) {
            artifactReference = headersHandler.readHeaders(headers);

            if (artifactReference.hasValue()) {
                return readData(data, artifactReference);
            }
        }

        ByteBuffer buffer;
        SchemaLookupResult<Object> schema;
        int length;

        if (data.length > 0 && data[0] == BaseSerde.MAGIC_BYTE) {
            buffer = BaseSerde.getByteBuffer(data);
            var idHandler = baseSerde.getIdHandler();
            artifactReference = idHandler.readId(buffer);
            schema = resolve(artifactReference);
            length = buffer.limit() - idHandler.idSize() - 1;
        } else {
            buffer = ByteBuffer.wrap(data);
            // Empty schema
            schema = NO_SCHEMA_ID;
            length = buffer.limit();
        }

        int start = buffer.position() + buffer.arrayOffset();

        return readData(schema, buffer, start, length);
    }

    private RecordData readData(byte[] data, ArtifactReference artifactReference) {
        SchemaLookupResult<Object> schema = resolve(artifactReference);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int length = buffer.limit();
        int start = buffer.position();

        return readData(schema, buffer, start, length);
    }

    private SchemaLookupResult<Object> resolve(ArtifactReference artifactReference) {
        if (artifactReference == null || !artifactReference.hasValue()) {
            return NO_SCHEMA_ID;
        }

        var schemaResolver = getSchemaResolver();

        if (schemaResolver == null) {
            return RESOLVER_MISSING;
        }

        try {
            return schemaResolver.resolveSchemaByArtifactReference(artifactReference);
        } catch (io.apicurio.registry.rest.client.models.ProblemDetails e) {
            LOG.infof("Schema could not be resolved: %s. Message: %s", artifactReference, e.getMessage());
            return LOOKUP_FAILURE;
        } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                /*
                 * Only log the stack trace at debug level. Schema resolution will be attempted
                 * for every message consumed and will lead to excessive logging in case of a
                 * problem.
                 */
                LOG.debugf(e, "Exception resolving schema reference: %s", artifactReference);
            } else {
                LOG.warnf("Exception resolving schema reference: %s ; %s", artifactReference, e.getMessage());
            }
            return LOOKUP_FAILURE;
        }
    }

    /**
     * Simple subclass of {@link io.apicurio.registry.serde.avro.AvroDeserializer}
     * to make the {@code readData} methods public.
     */
    private static class AvroDeserializer extends io.apicurio.registry.serde.avro.AvroDeserializer<RecordData> {
        AvroDeserializer(SchemaResolver<Schema, RecordData> schemaResolver) {
            super(schemaResolver);
        }

        @Override
        public RecordData readData(ParsedSchema<Schema> schema, ByteBuffer buffer, int start, int length) {
            return super.readData(schema, buffer, start, length);
        }
    }

    /**
     * Simple subclass of {@link io.apicurio.registry.serde.protobuf.ProtobufDeserializer} to make the
     * {@code readData} methods public.
     */
    private static class ProtobufDeserializer extends io.apicurio.registry.serde.protobuf.ProtobufDeserializer<Message> {
        ProtobufDeserializer(SchemaResolver<ProtobufSchema, Message> schemaResolver) {
            super(schemaResolver);
        }

        @Override
        public Message readData(ParsedSchema<ProtobufSchema> schema, ByteBuffer buffer, int start, int length) {
            return super.readData(schema, buffer, start, length);
        }
    }

}
