package com.github.streamshub.console.api.support.serdes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.apicurio.registry.resolver.ParsedSchema;
import io.apicurio.registry.resolver.SchemaParser;
import io.apicurio.registry.resolver.data.Record;
import io.apicurio.registry.types.ArtifactType;

/**
 * Schema parser that delegates to either the Avro or Protobuf schema parser.
 * This class attempts to sniff the schema's type using a primitive check for a
 * Protobuf message header.
 *
 * This class can likely be improved with more a advanced/accurate detection
 * process.
 */
public class MultiformatSchemaParser<D> implements SchemaParser<Object, D> {

    private static final int CACHE_LIMIT = 20;
    private static final char[] PROTOBUF_INTRO = "message ".toCharArray();

    private final Map<String, SchemaParser<?, ?>> delegates;
    private final Map<byte[], Object> schemaCache = new LinkedHashMap<>(CACHE_LIMIT);

    public MultiformatSchemaParser(Set<SchemaParser<?, ?>> delegates) {
        this.delegates = delegates.stream()
                .map(p -> Map.entry(p.artifactType(), p))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String artifactType() {
        throw new UnsupportedOperationException("MultiformatSchemaParser#artifactType()");
    }

    @Override
    public Object parseSchema(byte[] rawSchema, Map<String, ParsedSchema<Object>> resolvedReferences) {
        if (schemaCache.containsKey(rawSchema)) {
            return schemaCache.get(rawSchema);
        }

        Object parsedSchema = loadSchema(rawSchema, resolvedReferences);

        if (schemaCache.size() == CACHE_LIMIT) {
            // Remove the oldest entry
            var iter = schemaCache.entrySet().iterator();
            iter.next();
            iter.remove();
        }

        schemaCache.put(rawSchema, parsedSchema);

        return parsedSchema;
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

    @SuppressWarnings("unchecked")
    Object loadSchema(byte[] rawSchema, Map<String, ParsedSchema<Object>> resolvedReferences) {
        Object parsedSchema = null;
        SchemaParser<Object, RecordData> delegate = null;

        if (delegates.containsKey(ArtifactType.PROTOBUF) && looksLikeProtobuf(rawSchema)) {
            delegate = (SchemaParser<Object, RecordData>) delegates.get(ArtifactType.PROTOBUF);
        } else if (delegates.containsKey(ArtifactType.AVRO)) {
            delegate = (SchemaParser<Object, RecordData>) delegates.get(ArtifactType.AVRO);
        }

        if (delegate != null) {
            try {
                parsedSchema = delegate.parseSchema(rawSchema, resolvedReferences);
            } catch (Exception e) {
                // Schema is not valid, will be cached as null
            }
        }

        return parsedSchema;
    }

    boolean looksLikeProtobuf(byte[] rawSchema) {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(rawSchema))) {
            int input;

            while ((input = reader.read()) != -1) {
                if (Character.isWhitespace(input)) {
                    continue;
                }
                char[] buffer = new char[8];
                buffer[0] = (char) input;

                if (reader.read(buffer, 1, 7) == 7 && Arrays.equals(PROTOBUF_INTRO, buffer)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        return false;
    }
}
