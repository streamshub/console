package com.github.streamshub.console.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Schema(additionalProperties = Object.class)
public class JsonApiMeta {

    public static JsonApiMeta put(JsonApiMeta meta, String key, Object value) {
        if (meta == null) {
            meta = new JsonApiMeta();
        }
        meta.put(key, value);
        return meta;
    }

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
