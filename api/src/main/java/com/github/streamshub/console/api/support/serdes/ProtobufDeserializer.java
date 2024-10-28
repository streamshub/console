package com.github.streamshub.console.api.support.serdes;

import java.nio.ByteBuffer;

import org.apache.kafka.common.header.Headers;

import com.google.protobuf.Message;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.serde.protobuf.ProtobufKafkaDeserializer;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

/**
 * Simple subclass of {@link ProtobufKafkaDeserializer} to make the
 * {@code readData} methods public.
 */
class ProtobufDeserializer extends ProtobufKafkaDeserializer<Message> {
    ProtobufDeserializer(SchemaResolver<ProtobufSchema, Message> schemaResolver) {
        super();
        setSchemaResolver(schemaResolver);
    }

    @Override
    public Message readData(ParsedSchema<ProtobufSchema> schema, ByteBuffer buffer, int start, int length) {
        return super.readData(schema, buffer, start, length);
    }

    @Override
    public Message readData(Headers headers,
            ParsedSchema<ProtobufSchema> schema,
            ByteBuffer buffer,
            int start,
            int length) {
        return super.readData(headers, schema, buffer, start, length);
    }
}
