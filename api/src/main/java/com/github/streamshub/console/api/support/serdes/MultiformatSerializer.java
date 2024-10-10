package com.github.streamshub.console.api.support.serdes;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;

import io.apicurio.registry.resolver.DefaultSchemaResolver;
import io.apicurio.registry.resolver.ParsedSchema;
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
import io.apicurio.registry.serde.config.BaseKafkaSerDeConfig;
import io.apicurio.registry.serde.data.KafkaSerdeMetadata;
import io.apicurio.registry.serde.data.KafkaSerdeRecord;
import io.apicurio.registry.types.ArtifactType;

public class MultiformatSerializer extends AbstractKafkaSerializer<Object, RecordData> implements ArtifactReferenceResolverStrategy<Object, RecordData> {

    final ObjectMapper objectMapper;
    AvroSerializer avroSerializer;
    ProtobufSerializer protobufSerializer;
    SchemaParser<Object, RecordData> parser;

    public MultiformatSerializer(RegistryClient client, ObjectMapper objectMapper) {
        super();
        setSchemaResolver(newResolver(client));
        this.objectMapper = objectMapper;
        this.avroSerializer = new AvroSerializer(newResolver(client));
        this.protobufSerializer = new ProtobufSerializer(newResolver(client));
    }

    static <S, D> SchemaResolver<S, D> newResolver(RegistryClient client) {
        var resolver = new DefaultSchemaResolver<S, D>();
        resolver.setClient(client);
        return resolver;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Map<String, Object> avroConfigs = new HashMap<>(configs);
        avroConfigs.put(AvroKafkaSerdeConfig.AVRO_DATUM_PROVIDER, AvroDatumProvider.class.getName());
        avroConfigs.put(SchemaResolverConfig.ARTIFACT_RESOLVER_STRATEGY, this);
        avroConfigs.put(SchemaResolverConfig.FIND_LATEST_ARTIFACT, true);
        avroSerializer.configure(avroConfigs, isKey);

        Map<String, Object> protobufConfigs = new HashMap<>(configs);
        protobufConfigs.put(SchemaResolverConfig.ARTIFACT_RESOLVER_STRATEGY, this);
        protobufConfigs.put(SchemaResolverConfig.FIND_LATEST_ARTIFACT, true);
        protobufConfigs.put(SerdeConfig.AUTO_REGISTER_ARTIFACT, Boolean.TRUE);
        protobufSerializer.configure(protobufConfigs, isKey);

        parser = new MultiformatSchemaParser<>(Set.of(
            avroSerializer.schemaParser(),
            protobufSerializer.schemaParser()
        ));

        super.configure(new BaseKafkaSerDeConfig(configs), isKey);
    }

    @Override
    public void close() {
        // don't close - serializer will be reused
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

        String formatHeaderName = "com.github.streamshub.console.message-format" + (isKey() ? "-key" : "-value");
        Header formatHeader = headers.lastHeader(formatHeaderName);
        String format = Optional.ofNullable(formatHeader)
                .map(Header::value)
                .map(String::new)
                .orElse("");
        headers.remove(formatHeaderName);

        if (ArtifactType.AVRO.equals(format)) {
            try {
                return avroSerializer.serialize(topic, headers, data);
            } catch (Exception e) {
                throw new BadRequestException(e.getMessage());
            }
        } else if (ArtifactType.PROTOBUF.equals(format)) {
            var structBuilder = Struct.newBuilder();
            try {
                com.google.protobuf.util.JsonFormat.parser()
                        .ignoringUnknownFields()
                        .merge(data.dataString(null), structBuilder);
            } catch (InvalidProtocolBufferException e) {
                throw new BadRequestException(e.getMessage());
            }
            return protobufSerializer.serialize(topic, headers, structBuilder.build());
        } else {
            return data.data;
        }
    }

    @Override
    public ArtifactReference artifactReference(Record<RecordData> data, ParsedSchema<Object> parsedSchema) {
        KafkaSerdeRecord<RecordData> kdata = (KafkaSerdeRecord<RecordData>) data;
        KafkaSerdeMetadata metadata = kdata.metadata();

        return ArtifactReference.builder()
                .groupId("default")
                .artifactId(String.format("%s-%s", metadata.getTopic(), metadata.isKey() ? "key" : "value"))
                .build();
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
