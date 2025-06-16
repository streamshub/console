package com.github.streamshub.console.api.model.jsonapi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class for JSON API meta data attached to the document root, resources, or relationships.
 *
 * @see <a href="https://jsonapi.org/format/#document-meta">JSON API Document Structure, 7.5 Meta Information</a>
 */
@Schema(additionalProperties = Object.class)
public class JsonApiMeta {

    @JsonIgnore
    private Map<String, Object> meta;

    @JsonAnyGetter
    public Map<String, Object> get() {
        return meta;
    }

    public Object get(String key) {
        return meta != null ? meta.get(key) : null;
    }

    @JsonAnySetter
    public JsonApiMeta put(String key, Object value) {
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }
        meta.put(key, value);
        return this;
    }

}
