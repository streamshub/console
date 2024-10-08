package com.github.streamshub.console.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Base class for all JSON API request and response bodies.
 *
 * @see <a href="https://jsonapi.org/format/#document-structure">JSON API Document Structure, 7.1 Top Level</a>
 */
@JsonInclude(value = Include.NON_NULL)
public abstract class JsonApiDocument {

    private JsonApiMeta meta;
    private Map<String, String> links;

    static <K, V> Map<K, V> addEntry(Map<K, V> map, K key, V value) {
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        map.put(key, value);
        return map;
    }

    @JsonProperty
    public JsonApiMeta meta() {
        return meta;
    }

    public Object meta(String key) {
        return meta != null ? meta.get(key) : null;
    }

    public JsonApiDocument addMeta(String key, Object value) {
        meta = JsonApiMeta.put(meta, key, value);
        return this;
    }

    @JsonProperty
    public Map<String, String> links() {
        return links;
    }

    public JsonApiDocument addLink(String key, String value) {
        links = addEntry(links, key, value);
        return this;
    }

}
