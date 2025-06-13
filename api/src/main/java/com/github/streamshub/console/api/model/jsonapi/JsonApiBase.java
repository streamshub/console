package com.github.streamshub.console.api.model.jsonapi;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for all JSON API object structures.
 *
 * @see <a href="https://jsonapi.org/format/#document-structure">JSON API Document Structure, 7.1 Top Level</a>
 */
@JsonInclude(value = Include.NON_NULL)
public abstract class JsonApiBase {

    @Valid
    private JsonApiMeta meta;

    private Map<String, String> links;

    protected JsonApiBase(JsonApiMeta meta) {
        this.meta = meta;
    }

    protected JsonApiBase() {
        this(null);
    }

    @JsonProperty
    public JsonApiMeta meta() {
        return meta;
    }

    public void meta(JsonApiMeta meta) {
        this.meta = meta;
    }

    public JsonApiMeta metaFactory() {
        return new JsonApiMeta();
    }

    @JsonIgnore
    public JsonApiMeta getOrCreateMeta() {
        JsonApiMeta m = meta();

        if (m == null) {
            m = metaFactory();
            meta(m);
        }

        return m;
    }

    public Object meta(String key) {
        JsonApiMeta m = meta();
        return m != null ? m.get(key) : null;
    }

    public JsonApiBase addMeta(String key, Object value) {
        JsonApiMeta m = getOrCreateMeta();
        m.put(key, value);
        return this;
    }

    @JsonProperty
    public Map<String, String> links() {
        return links;
    }

    public void links(Map<String, String> links) {
        this.links = links;
    }

    public JsonApiBase addLink(String key, String value) {
        links(addEntry(links(), key, value));
        return this;
    }

    private static <K, V> Map<K, V> addEntry(Map<K, V> map, K key, V value) {
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        map.put(key, value);
        return map;
    }
}
