package com.github.streamshub.console.api.support.serdes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;

import jakarta.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.resolver.strategy.ArtifactReference;

public class ArtifactReferences {

    private static final Logger LOGGER = Logger.getLogger(ArtifactReferences.class);
    private static final String GLOBAL_ID = "globalId";
    private static final String CONTENT_ID = "contentId";
    private static final String GROUP_ID = "groupId";
    private static final String ARTIFACT_ID = "artifactId";
    private static final String VERSION = "version";

    private ArtifactReferences() {
    }

    public static String toSchemaId(ArtifactReference reference, ObjectMapper objectMapper) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (var generator = objectMapper.createGenerator(out)) {
            generator.writeStartObject();
            putEntry(generator, GLOBAL_ID, reference.getGlobalId());
            putEntry(generator, CONTENT_ID, reference.getContentId());
            putEntry(generator, GROUP_ID, reference.getGroupId());
            putEntry(generator, ARTIFACT_ID, reference.getArtifactId());
            putEntry(generator, VERSION, reference.getVersion());
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Base64.getUrlEncoder().encodeToString(out.toByteArray());
    }

    public static ArtifactReference fromSchemaId(String schemaId, ObjectMapper objectMapper) {
        JsonNode id;

        try {
            InputStream in = new ByteArrayInputStream(Base64.getUrlDecoder().decode(schemaId));
            id = objectMapper.readTree(in);
        } catch (IOException e) {
            LOGGER.debugf("Failed to decode or parse schemaId '%s': %s", schemaId, e.getMessage());
            throw new NotFoundException("No such schema");
        }

        var builder = ArtifactReference.builder();

        if (id.has(GLOBAL_ID)) {
            builder.globalId(id.get(GLOBAL_ID).asLong());
        }
        if (id.has(CONTENT_ID)) {
            builder.contentId(id.get(CONTENT_ID).asLong());
        }
        if (id.has(GROUP_ID)) {
            builder.groupId(id.get(GROUP_ID).asText());
        }
        if (id.has(ARTIFACT_ID)) {
            builder.artifactId(id.get(ARTIFACT_ID).asText());
        }
        if (id.has(VERSION)) {
            builder.version(id.get(VERSION).asText());
        }

        return builder.build();
    }

    static void putEntry(JsonGenerator generator, String key, Object value) throws IOException {
        if (value instanceof String str) {
            generator.writeStringField(key, str);
        } else if (value instanceof Long nbr && nbr != 0) {
            generator.writeNumberField(key, nbr);
        }
    }

}
