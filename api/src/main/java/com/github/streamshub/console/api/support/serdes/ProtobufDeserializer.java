package com.github.streamshub.console.api.support.serdes;

import java.nio.ByteBuffer;

import com.google.protobuf.Message;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.strategy.ArtifactReference;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

/**
 * Simple subclass of {@link io.apicurio.registry.serde.protobuf.ProtobufDeserializer} to make the
 * {@code readData} methods public.
 */
class ProtobufDeserializer extends io.apicurio.registry.serde.protobuf.ProtobufDeserializer<Message> {
    ProtobufDeserializer(SchemaResolver<ProtobufSchema, Message> schemaResolver) {
        super(schemaResolver);
    }

    @Override
    public Message readData(ParsedSchema<ProtobufSchema> schema, ByteBuffer buffer, int start, int length) {
        return super.readData(schema, buffer, start, length);
    }

    @Override
    public Message readData(String topic, byte[] data, ArtifactReference artifactReference) {
        throw new UnsupportedOperationException();
    }
}
