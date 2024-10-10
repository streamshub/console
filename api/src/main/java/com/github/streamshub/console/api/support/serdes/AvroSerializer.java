package com.github.streamshub.console.api.support.serdes;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.kafka.common.header.Headers;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;

class AvroSerializer extends AvroKafkaSerializer<RecordData> {
    AvroSerializer(SchemaResolver<Schema, RecordData> schemaResolver) {
        super();
        setSchemaResolver(schemaResolver);
    }

    @Override
    public void serializeData(ParsedSchema<Schema> schema, RecordData data, OutputStream out) throws IOException {
        super.serializeData(schema, data, out);
    }

    @Override
    public void serializeData(Headers headers, ParsedSchema<Schema> schema, RecordData data, OutputStream out)
            throws IOException {
        super.serializeData(headers, schema, data, out);
    }
}
