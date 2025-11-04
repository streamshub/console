package com.github.streamshub.console.api.support.serdes;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.apache.avro.Schema;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.support.RootCause;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaLookupResult;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.client.RegistryClientFacade;
import io.apicurio.registry.resolver.config.SchemaResolverConfig;
import io.apicurio.registry.resolver.data.Record;
import io.apicurio.registry.resolver.strategy.ArtifactReference;
import io.apicurio.registry.resolver.strategy.ArtifactReferenceResolverStrategy;
import io.apicurio.registry.serde.avro.AvroSerdeConfig;
import io.apicurio.registry.serde.avro.AvroSerializer;
import io.apicurio.registry.serde.config.BaseKafkaSerDeConfig;
import io.apicurio.registry.serde.config.SerdeConfig;
import io.apicurio.registry.serde.data.KafkaSerdeMetadata;
import io.apicurio.registry.serde.data.SerdeRecord;
import io.apicurio.registry.serde.protobuf.ProtobufSerializer;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

import static io.apicurio.registry.serde.BaseSerde.MAGIC_BYTE;

/**
 * Serializer that supports writing Avro, Protobuf, and raw bytes.
 *
 * This serializer requires that the input data has provided a GAV
 * (groupId/artifactId/version) for the target schema, otherwise it will pass
 * through the input untouched. If the provided GAV can be found in the Apicurio
 * Registry, the schema will be used to serialize to either Avro or Protobuf
 * depending on the schema type.
 */
public class MultiformatSerializer extends MultiformatSerdeBase
        implements Serializer<RecordData>, ArtifactReferenceResolverStrategy<Object, RecordData> {

    private static final Logger LOGGER = Logger.getLogger(MultiformatSerializer.class);
    private static final SchemaLookupResult<Object> EMPTY_RESULT = SchemaLookupResult.builder().build();

    AvroSerializer<RecordData> avroSerializer;
    ProtobufSerializer<Message> protobufSerializer;

    public MultiformatSerializer(RegistryClientFacade client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override
    protected AutoCloseable createAvroSerde(SchemaResolver<Schema, RecordData> resolver) {
        this.avroSerializer = new AvroSerializer<>(resolver);
        return avroSerializer;
    }

    @Override
    protected AutoCloseable createProtobufSerde(SchemaResolver<ProtobufSchema, Message> resolver) {
        this.protobufSerializer = new ProtobufSerializer<>(resolver);
        return protobufSerializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (getSchemaResolver() == null) {
            super.key = isKey;
            // Do not attempt to configure anything more if we will not be making remote calls to registry
            return;
        }

        Map<String, Object> serConfigs = new HashMap<>(configs);
        serConfigs.put(SchemaResolverConfig.ARTIFACT_RESOLVER_STRATEGY, this);

        Map<String, Object> avroConfigs = new HashMap<>(serConfigs);
        avroConfigs.put(AvroSerdeConfig.AVRO_DATUM_PROVIDER, AvroDatumProvider.class);
        avroConfigs.put(SchemaResolverConfig.FIND_LATEST_ARTIFACT, Boolean.TRUE);
        avroSerializer.configure(new SerdeConfig(avroConfigs), isKey);

        Map<String, Object> protobufConfigs = new HashMap<>(serConfigs);
        protobufConfigs.put(SchemaResolverConfig.FIND_LATEST_ARTIFACT, Boolean.TRUE);
        protobufConfigs.put(SerdeConfig.VALIDATION_ENABLED, Boolean.TRUE);
        protobufSerializer.configure(new SerdeConfig(protobufConfigs), isKey);

        parser = new MultiformatSchemaParser<>(Set.of(
            cast(avroSerializer.schemaParser()),
            cast(protobufSerializer.schemaParser())
        ));

        baseSerde.configure(new SerdeConfig(configs), isKey, parser);

        super.configure(new BaseKafkaSerDeConfig(serConfigs), isKey);
    }

    @Override
    public byte[] serialize(String topic, RecordData data) {
        return serialize(topic, null, data);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, RecordData data) {
        // just return null
        if (data == null) {
            return null; // NOSONAR - we want to return null and not an empty array
        }

        SchemaLookupResult<Object> schema = resolveSchema(topic, headers, data);
        Object parsedSchema = null;

        if (schema != null && schema.getParsedSchema() != null) {
            parsedSchema = schema.getParsedSchema().getParsedSchema();
        }

        byte[] serialized;

        if (parsedSchema instanceof Schema avroSchema) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(MAGIC_BYTE);
                baseSerde.getIdHandler().writeId(schema.toArtifactReference(), out);
                avroSerializer.serializeData(cast(schema.getParsedSchema()), data, out);
                serialized = out.toByteArray();
                setSchemaMeta(data, schema, ArtifactType.AVRO, avroSchema.getFullName());
            } catch (Exception e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        } else if (parsedSchema instanceof ProtobufSchema protobufSchema) {
            Message msg;
            String schemaRef = schemaMeta(data, "schema-gav").orElseThrow(); // we know it's non-null because we have a schema
            String messageType = schemaMeta(data, "message-type").orElse(null);
            Descriptor descriptor;

            if (messageType != null) {
                descriptor = protobufSchema.getFileDescriptor().findMessageTypeByName(messageType);
                if (descriptor == null) {
                    throw new BadRequestException("No such message type %s for schema %s"
                            .formatted(messageType, schemaRef));
                }
            } else if (protobufSchema.getFileDescriptor().getMessageTypes().size() == 1) {
                descriptor = protobufSchema.getFileDescriptor().getMessageTypes().get(0);
            } else {
                throw new BadRequestException("Unable to determine message type to use from schema %s"
                        .formatted(schemaRef));
            }

            try {
                var builder = DynamicMessage.newBuilder(descriptor);
                com.google.protobuf.util.JsonFormat.parser()
                    .ignoringUnknownFields()
                    .merge(data.dataString(null), builder);
                msg = builder.build();
            } catch (InvalidProtocolBufferException e) {
                throw new BadRequestException(e.getMessage(), e);
            }

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(MAGIC_BYTE);
                baseSerde.getIdHandler().writeId(schema.toArtifactReference(), out);
                protobufSerializer.serializeData(cast(schema.getParsedSchema()), msg, out);
                serialized = out.toByteArray();
                setSchemaMeta(data, schema, ArtifactType.PROTOBUF, descriptor.getFullName());
            } catch (Exception e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        } else {
            data.meta.remove("schema"); // Remove schema meta so it is not returned with 201 response
            serialized = data.data;
        }

        return serialized;
    }

    SchemaLookupResult<Object> resolveSchema(String topic, Headers headers, RecordData data) {
        if (getSchemaResolver() == null) {
            return EMPTY_RESULT;
        }

        KafkaSerdeMetadata resolverMetadata = new KafkaSerdeMetadata(topic, key, headers);
        var reference = artifactReference(new SerdeRecord<>(resolverMetadata, data), null);
        SchemaLookupResult<Object> schema = null;

        if (reference != null) {
            try {
                schema = getSchemaResolver().resolveSchemaByArtifactReference(reference);
            } catch (Exception e) {
                LOGGER.warnf("Exception retrieving schema: %s", RootCause.of(e)
                        .map(Throwable::getMessage)
                        .orElseGet(() -> String.valueOf(e)));
                schema = EMPTY_RESULT;
            }
        }

        return schema;
    }

    private void setSchemaMeta(RecordData data, SchemaLookupResult<Object> schema, String type, String name) {
        String id = ArtifactReferences.toSchemaId(schema.toArtifactReference(), objectMapper);

        data.meta.put("schema-type", type);
        data.meta.put("schema-id", id);
        data.meta.put("schema-name", name);
    }

    @Override
    public ArtifactReference artifactReference(Record<RecordData> data, ParsedSchema<Object> parsedSchema) {
        SerdeRecord<RecordData> kdata = (SerdeRecord<RecordData>) data;
        RecordData rData = kdata.payload();

        return schemaMeta(rData, "schema-gav")
            .map(schemaRef -> {
                String[] gav = schemaRef.split(":");
                return ArtifactReference.builder()
                        .groupId(gav[0])
                        .artifactId(gav[1])
                        .version(gav.length > 2 ? gav[2] : null)
                        .build();
            })
            .orElse(null);
    }

    private Optional<String> schemaMeta(RecordData data, String metaProperty) {
        return Optional.ofNullable(data.meta.get(metaProperty));
    }

    @Override
    public boolean loadSchema() {
        return false;
    }
}
