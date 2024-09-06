package com.github.streamshub.console.api.support.serdes;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.kafka.common.header.Headers;

import com.google.protobuf.Message;

import io.apicurio.registry.resolver.DefaultSchemaResolver;
import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaLookupResult;
import io.apicurio.registry.resolver.SchemaParser;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.strategy.ArtifactReference;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.serde.AbstractKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.config.BaseKafkaSerDeConfig;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

public class MultiformatDeserializer extends AbstractKafkaDeserializer<Object, RecordData> {

    AvroDeserializer avroDeserializer;
    ProtobufDeserializer protobufDeserializer;
    SchemaParser<Object, RecordData> parser;

    public MultiformatDeserializer(RegistryClient client) {
        super();
        setSchemaResolver(newResolver(client));
        this.avroDeserializer = new AvroDeserializer(newResolver(client));
        this.protobufDeserializer = new ProtobufDeserializer(newResolver(client));
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
        avroDeserializer.configure(avroConfigs, isKey);

        protobufDeserializer.configure(configs, isKey);

        parser = new MultiformatSchemaParser<>(Set.of(
            avroDeserializer.schemaParser(),
            protobufDeserializer.schemaParser()
        ));

        super.configure(new BaseKafkaSerDeConfig(configs), isKey);
    }

    @Override
    public void close() {
        // don't close - deserializer will be reused
    }

    @Override
    public SchemaParser<Object, RecordData> schemaParser() {
        return parser;
    }

    @Override
    protected RecordData readData(ParsedSchema<Object> schema, ByteBuffer buffer, int start, int length) {
        Object parsedSchema = schema != null ? schema.getParsedSchema() : null;
        RecordData result;

        if (parsedSchema instanceof Schema) {
            try {
                result = avroDeserializer.readData(cast(schema), buffer, start, length);
                result.schema = schema;
            } catch (Exception e) {
                result = new RecordData(null, null, schema);
                result.meta.put("error", e.getMessage());
            }
        } else if (parsedSchema instanceof ProtobufSchema) {
            try {
                Message msg = protobufDeserializer.readData(cast(schema), buffer, start, length);
                byte[] data = com.google.protobuf.util.JsonFormat.printer().print(msg).getBytes();
                result = new RecordData(ArtifactType.PROTOBUF, data, schema);
                result.schema = schema;
            } catch (Exception e) {
                result = new RecordData(null, null, schema);
                result.meta.put("error", e.getMessage());
            }
        } else {
            byte[] bytes = new byte[length];
            System.arraycopy(buffer.array(), start, bytes, 0, length);
            result = new RecordData(null, bytes, null);
        }

        return result;
    }

    @Override
    protected RecordData readData(Headers headers, ParsedSchema<Object> schema, ByteBuffer buffer, int start, int length) {
        Object parsedSchema = schema != null ? schema.getParsedSchema() : null;
        RecordData result;

        if (parsedSchema instanceof Schema) {
            try {
                result = avroDeserializer.readData(headers, cast(schema), buffer, start, length);
            } catch (Exception e) {
                result = new RecordData(null, null, schema);
                result.meta.put("error", e.getMessage());
            }
        } else if (parsedSchema instanceof ProtobufSchema) {
            try {
                Message msg = protobufDeserializer.readData(headers, cast(schema), buffer, start, length);
                byte[] data = com.google.protobuf.util.JsonFormat.printer().print(msg).getBytes();
                result = new RecordData(ArtifactType.PROTOBUF, data, schema);
            } catch (Exception e) {
                result = new RecordData(null, null, schema);
                result.meta.put("error", e.getMessage());
            }
        } else {
            byte[] bytes = new byte[length];
            System.arraycopy(buffer.array(), start, bytes, 0, length);
            result = new RecordData(null, bytes, null);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object object) {
        return (T) object;
    }

    @Override
    public RecordData deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        ByteBuffer buffer;
        SchemaLookupResult<Object> schema;
        int length;

        if (data[0] == MAGIC_BYTE) {
            buffer = getByteBuffer(data);
            ArtifactReference artifactReference = getIdHandler().readId(buffer);
            schema = resolve(artifactReference);
            length = buffer.limit() - 1 - getIdHandler().idSize();
        } else {
            buffer = ByteBuffer.wrap(data);
            // Empty schema
            schema = SchemaLookupResult.builder().build();
            length = buffer.limit() - 1;
        }

        int start = buffer.position() + buffer.arrayOffset();

        return readData(schema.getParsedSchema(), buffer, start, length);
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
                return readData(headers, data, artifactReference);
            }
        }

        if (headers == null) {
            return deserialize(topic, data);
        } else {
            //try to read data even if artifactReference has no value, maybe there is a fallbackArtifactProvider configured
            return readData(headers, data, artifactReference);
        }
    }

    private RecordData readData(Headers headers, byte[] data, ArtifactReference artifactReference) {
        SchemaLookupResult<Object> schema = resolve(artifactReference);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int length = buffer.limit();
        int start = buffer.position();

        return readData(headers, schema.getParsedSchema(), buffer, start, length);
    }

    private SchemaLookupResult<Object> resolve(ArtifactReference artifactReference) {
        try {
            return getSchemaResolver().resolveSchemaByArtifactReference(artifactReference);
        } catch (RuntimeException e) {
            // Empty result
            return SchemaLookupResult.builder().build();
        }
    }
}
