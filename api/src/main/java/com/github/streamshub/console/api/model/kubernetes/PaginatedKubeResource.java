package com.github.streamshub.console.api.model.kubernetes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Common interface to parse and generate pagination cursors for Kube resources
 * having the common fields id, name, namespace, and creationTimestamp.
 */
public interface PaginatedKubeResource {

    static class Fields {
        static final String NAME = "name";
        static final String NAMESPACE = "namespace";
        static final String CREATION_TIMESTAMP = "creationTimestamp";

        private Fields() {
            // Prevent instances
        }
    }

    // Use getId to match Resource method.
    // May rename later, see https://github.com/streamshub/console/issues/43
    String getId();

    String name();

    void name(String name);

    String namespace();

    void namespace(String namespace);

    String creationTimestamp();

    void creationTimestamp(String creationTimestamp);

    static <T extends PaginatedKubeResource> T fromCursor(JsonObject cursor, Function<String, T> resourceFactory) {
        if (cursor == null) {
            return null;
        }

        JsonObject attr = cursor.getJsonObject("attributes");

        T resource = resourceFactory.apply(cursor.getString("id"));
        resource.name(attr.getString(Fields.NAME, null));
        resource.namespace(attr.getString(Fields.NAMESPACE, null));
        resource.creationTimestamp(attr.getString(Fields.CREATION_TIMESTAMP, null));

        return resource;
    }

    default String toCursor(List<String> sortFields) {
        String id = getId();

        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", id == null ? Json.createValue("") : Json.createValue(id));

        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();
        maybeAddAttribute(attrBuilder, sortFields, Fields.NAME, name());
        maybeAddAttribute(attrBuilder, sortFields, Fields.NAMESPACE, namespace());
        maybeAddAttribute(attrBuilder, sortFields, Fields.CREATION_TIMESTAMP, creationTimestamp());
        cursor.add("attributes", attrBuilder.build());

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }

    static void maybeAddAttribute(JsonObjectBuilder attrBuilder, List<String> sortFields, String key, String value) {
        if (sortFields.contains(key)) {
            attrBuilder.add(key, value != null ? Json.createValue(value) : JsonValue.NULL);
        }
    }

}
