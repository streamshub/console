package com.github.streamshub.console.api.support.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.kafka.common.header.Headers;

import com.google.protobuf.Message;

import io.apicurio.registry.resolver.SchemaLookupResult;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.serde.AbstractKafkaSerializer;
import io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

/**
 * This serializer is required to provide a {@code serialize} method to be used
 * instead of {@link AbstractKafkaSerializer#serialize(String, Headers, Object)}
 * where the schema is provided, rather than resolved. We have already resolved
 * the schema when this serializer is invoked in order to determine that the
 * message type should be Protobuf.
 */
class ProtobufSerializer extends ProtobufKafkaSerializer<Message> {
    ProtobufSerializer(SchemaResolver<ProtobufSchema, Message> schemaResolver) {
        super();
        setSchemaResolver(schemaResolver);
    }

    public byte[] serialize(Headers headers, Message data, SchemaLookupResult<ProtobufSchema> schema) {
        // just return null
        if (data == null) {
            return null; // NOSONAR
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (headersHandler != null && headers != null) {
                headersHandler.writeHeaders(headers, schema.toArtifactReference());
                serializeData(headers, schema.getParsedSchema(), data, out);
            } else {
                out.write(MAGIC_BYTE);
                getIdHandler().writeId(schema.toArtifactReference(), out);
                serializeData(schema.getParsedSchema(), data, out);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
