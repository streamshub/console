package com.github.streamshub.console.api.support.serdes;

import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.kafka.common.header.Headers;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;

class AvroDeserializer extends AvroKafkaDeserializer<RecordData> {
    AvroDeserializer(SchemaResolver<Schema, RecordData> schemaResolver) {
        super();
        setSchemaResolver(schemaResolver);
    }

    @Override
    public RecordData readData(ParsedSchema<Schema> schema, ByteBuffer buffer, int start, int length) {
        return super.readData(schema, buffer, start, length);
    }

    @Override
    public RecordData readData(Headers headers,
            ParsedSchema<Schema> schema,
            ByteBuffer buffer,
            int start,
            int length) {
        return super.readData(headers, schema, buffer, start, length);
    }
}
