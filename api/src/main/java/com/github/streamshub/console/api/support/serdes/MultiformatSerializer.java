package com.github.streamshub.console.api.support.serdes;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.apache.avro.Schema;
import org.apache.kafka.common.header.Headers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.apicurio.registry.resolver.DefaultSchemaResolver;
import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaLookupResult;
import io.apicurio.registry.resolver.SchemaParser;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.SchemaResolverConfig;
import io.apicurio.registry.resolver.data.Record;
import io.apicurio.registry.resolver.strategy.ArtifactReference;
import io.apicurio.registry.resolver.strategy.ArtifactReferenceResolverStrategy;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.serde.AbstractKafkaSerializer;
import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.apicurio.registry.serde.config.BaseKafkaSerDeConfig;
import io.apicurio.registry.serde.data.KafkaSerdeMetadata;
import io.apicurio.registry.serde.data.KafkaSerdeRecord;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

/**
 * Serializer that supports writing Avro, Protobuf, and raw bytes.
 *
 * This serializer requires that the input data has provided a GAV
 * (groupId/artifactId/version) for the target schema, otherwise it will pass
 * through the input untouched. If the provided GAV can be found in the Apicurio
 * Registry, the schema will be used to serialize to either Avro or Protobuf
 * depending on the schema type.
 */
public class MultiformatSerializer extends AbstractKafkaSerializer<Object, RecordData> implements ArtifactReferenceResolverStrategy<Object, RecordData>, ForceCloseable {

    private static final SchemaLookupResult<Object> EMPTY_RESULT = SchemaLookupResult.builder().build();

    final ObjectMapper objectMapper;
    AvroKafkaSerializer<RecordData> avroSerializer;
    ProtobufSerializer protobufSerializer;
    SchemaParser<Object, RecordData> parser;

    public MultiformatSerializer(RegistryClient client, ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;

        if (client != null) {
            setSchemaResolver(newResolver(client));
            avroSerializer = new AvroKafkaSerializer<>();
            avroSerializer.setSchemaResolver(newResolver(client));
            protobufSerializer = new ProtobufSerializer(newResolver(client));
        }
    }

    static <S, D> SchemaResolver<S, D> newResolver(RegistryClient client) {
        var resolver = new DefaultSchemaResolver<S, D>();
        resolver.setClient(client);
        return resolver;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (getSchemaResolver() == null) {
            key = isKey;
            // Do not attempt to configure anything more if we will not be making remote calls to registry
            return;
        }

        Map<String, Object> serConfigs = new HashMap<>(configs);
        serConfigs.put(SchemaResolverConfig.ARTIFACT_RESOLVER_STRATEGY, this);

        Map<String, Object> avroConfigs = new HashMap<>(serConfigs);
        avroConfigs.put(AvroKafkaSerdeConfig.AVRO_DATUM_PROVIDER, AvroDatumProvider.class.getName());
        avroConfigs.put(SchemaResolverConfig.FIND_LATEST_ARTIFACT, Boolean.TRUE);
        avroSerializer.configure(avroConfigs, isKey);

        Map<String, Object> protobufConfigs = new HashMap<>(serConfigs);
        protobufConfigs.put(SchemaResolverConfig.FIND_LATEST_ARTIFACT, Boolean.TRUE);
        protobufConfigs.put(SerdeConfig.VALIDATION_ENABLED, Boolean.TRUE);
        protobufSerializer.configure(protobufConfigs, isKey);

        parser = new MultiformatSchemaParser<>(Set.of(
            cast(avroSerializer.schemaParser()),
            cast(protobufSerializer.schemaParser())
        ));

        super.configure(new BaseKafkaSerDeConfig(serConfigs), isKey);
    }

    @Override
    public void close() {
        // don't close - serializer will be reused
    }

    public void forceClose() {
        super.close();
    }

    @Override
    public SchemaParser<Object, RecordData> schemaParser() {
        return parser;
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
                serialized = avroSerializer.serialize(topic, headers, data);
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

            serialized = protobufSerializer.serialize(headers, msg, cast(schema));
            setSchemaMeta(data, schema, ArtifactType.PROTOBUF, descriptor.getFullName());
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

        KafkaSerdeMetadata resolverMetadata = new KafkaSerdeMetadata(topic, isKey(), headers);
        var reference = artifactReference(new KafkaSerdeRecord<>(resolverMetadata, data), null);
        SchemaLookupResult<Object> schema = null;

        if (reference != null) {
            try {
                schema = getSchemaResolver().resolveSchemaByArtifactReference(reference);
            } catch (Exception e) {
                schema = EMPTY_RESULT;
            }
        }

        return schema;
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object object) {
        return (T) object;
    }

    private void setSchemaMeta(RecordData data, SchemaLookupResult<Object> schema, String type, String name) {
        String id = ArtifactReferences.toSchemaId(schema.toArtifactReference(), objectMapper);

        data.meta.put("schema-type", type);
        data.meta.put("schema-id", id);
        data.meta.put("schema-name", name);
    }

    @Override
    public ArtifactReference artifactReference(Record<RecordData> data, ParsedSchema<Object> parsedSchema) {
        KafkaSerdeRecord<RecordData> kdata = (KafkaSerdeRecord<RecordData>) data;
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

    @Override
    protected void serializeData(ParsedSchema<Object> schema, RecordData data, OutputStream out) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void serializeData(Headers headers, ParsedSchema<Object> schema, RecordData data, OutputStream out) {
        throw new UnsupportedOperationException();
    }
}
