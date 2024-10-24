package com.github.streamshub.console.api.support.serdes;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaParser;
import io.apicurio.registry.resolver.data.Record;

/**
 * Schema parser that delegates to either the Avro or Protobuf schema parser.
 */
public class MultiformatSchemaParser<D> implements SchemaParser<Object, D> {

    private final Map<String, SchemaParser<Object, ?>> delegates;

    public MultiformatSchemaParser(Set<SchemaParser<Object, ?>> delegates) {
        this.delegates = delegates.stream()
                .map(p -> Map.entry(p.artifactType(), p))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String artifactType() {
        throw new UnsupportedOperationException("MultiformatSchemaParser#artifactType()");
    }

    /**
     * Parse the raw schema bytes to a format-specific schema object using the
     * {@link #delegates} known to this parser. If none of the delegates are able to
     * parse the schema, this method will return null.
     *
     * @return a parsed schema model object or null if the raw schema cannot be
     *         parsed by one of the delegates.
     */
    @Override
    public Object parseSchema(byte[] rawSchema, Map<String, ParsedSchema<Object>> resolvedReferences) {
        for (SchemaParser<Object, ?> delegate : delegates.values()) {
            try {
                return delegate.parseSchema(rawSchema, resolvedReferences);
            } catch (Exception e) {
                // Schema is not valid for the delegate parser
            }
        }

        return null;
    }

    @Override
    public boolean supportsExtractSchemaFromData() {
        return false;
    }

    @Override
    public ParsedSchema<Object> getSchemaFromData(Record<D> data) {
        throw new UnsupportedOperationException("MultiformatSchemaParser#getSchemaFromData(Record)");
    }

    @Override
    public ParsedSchema<Object> getSchemaFromData(Record<D> data, boolean dereference) {
        throw new UnsupportedOperationException("MultiformatSchemaParser#getSchemaFromData(Record,boolean)");
    }
}
