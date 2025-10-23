package com.github.streamshub.console.api.support.serdes;

import java.nio.ByteBuffer;

import org.apache.avro.Schema;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.strategy.ArtifactReference;

/**
 * Simple subclass of {@link io.apicurio.registry.serde.avro.AvroDeserializer}
 * to make the {@code readData} methods public.
 */
class AvroDeserializer extends io.apicurio.registry.serde.avro.AvroDeserializer<RecordData> {
    AvroDeserializer(SchemaResolver<Schema, RecordData> schemaResolver) {
        super(schemaResolver);
    }

    @Override
    public RecordData readData(ParsedSchema<Schema> schema, ByteBuffer buffer, int start, int length) {
        return super.readData(schema, buffer, start, length);
    }

    @Override
    public RecordData readData(String topic, byte[] data, ArtifactReference artifactReference) {
        throw new UnsupportedOperationException();
    }
}
