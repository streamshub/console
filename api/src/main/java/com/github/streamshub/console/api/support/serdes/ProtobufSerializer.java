package com.github.streamshub.console.api.support.serdes;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.kafka.common.header.Headers;

import com.google.protobuf.Message;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

class ProtobufSerializer extends ProtobufKafkaSerializer<Message> {
    ProtobufSerializer(SchemaResolver<ProtobufSchema, Message> schemaResolver) {
        super();
        setSchemaResolver(schemaResolver);
    }

    @Override
    public void serializeData(ParsedSchema<ProtobufSchema> schema, Message data, OutputStream out)
            throws IOException {
        super.serializeData(schema, data, out);
    }

    @Override
    public void serializeData(Headers headers, ParsedSchema<ProtobufSchema> schema, Message data, OutputStream out)
            throws IOException {
        super.serializeData(headers, schema, data, out);
    }
}
