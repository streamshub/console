package com.github.streamshub.console.api.support.serdes;

import java.io.Closeable;
import java.util.List;

import org.apache.avro.Schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;

import io.apicurio.registry.resolver.DefaultSchemaResolver;
import io.apicurio.registry.resolver.SchemaParser;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.client.RegistryClientFacade;
import io.apicurio.registry.resolver.utils.Utils;
import io.apicurio.registry.serde.BaseSerde;
import io.apicurio.registry.serde.config.BaseKafkaSerDeConfig;
import io.apicurio.registry.serde.headers.HeadersHandler;
import io.apicurio.registry.utils.protobuf.schema.ProtobufSchema;

abstract class MultiformatSerdeBase implements Closeable, ForceCloseable {

    protected final ObjectMapper objectMapper;
    protected final BaseSerde<Object, RecordData> baseSerde;
    protected HeadersHandler headersHandler;
    protected boolean key;
    protected SchemaParser<Object, RecordData> parser;

    private final SchemaResolver<Object, RecordData> schemaResolver;
    private final AutoCloseable avroSerde;
    private final AutoCloseable protobufSerde;

    protected MultiformatSerdeBase(RegistryClientFacade client, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        if (client != null) {
            schemaResolver = newResolver(client);
            avroSerde = createAvroSerde(cast(schemaResolver));
            protobufSerde = createProtobufSerde(cast(schemaResolver));
            baseSerde = new BaseSerde<>(schemaResolver);
        } else {
            schemaResolver = null;
            avroSerde = null;
            protobufSerde = null;
            baseSerde = new BaseSerde<>();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object object) {
        return (T) object;
    }

    static <S, D> SchemaResolver<S, D> newResolver(RegistryClientFacade client) {
        var resolver = new DefaultSchemaResolver<S, D>();
        resolver.setClientFacade(client);
        return resolver;
    }

    protected void configure(BaseKafkaSerDeConfig config, boolean isKey) {
        key = isKey;

        if (config.enableHeaders()) {
            Object headersHandlerConf = config.getHeadersHandler();
            Utils.instantiate(HeadersHandler.class, headersHandlerConf, this::setHeadersHandler);
            this.headersHandler.configure(config.originals(), isKey);
        }
    }

    private void setHeadersHandler(HeadersHandler headersHandler) {
        this.headersHandler = headersHandler;
    }

    @Override
    public void close() {
        // don't close - serializer will be reused
    }

    @Override
    public void forceClose() {
        for (var serde : List.of(avroSerde, protobufSerde)) {
            if (serde != null) {
                try {
                    serde.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    protected boolean isKey() {
        return key;
    }

    protected SchemaResolver<Object, RecordData> getSchemaResolver() {
        return schemaResolver;
    }

    protected abstract AutoCloseable createAvroSerde(SchemaResolver<Schema, RecordData> resolver);

    protected abstract AutoCloseable createProtobufSerde(SchemaResolver<ProtobufSchema, Message> resolver);
}
