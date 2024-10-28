package com.github.streamshub.console.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

interface HasMeta<T> {

    JsonApiMeta meta();

    void meta(JsonApiMeta meta);

    default JsonApiMeta metaFactory() {
        return new JsonApiMeta();
    }

    @JsonIgnore
    default JsonApiMeta getOrCreateMeta() {
        JsonApiMeta meta = meta();

        if (meta == null) {
            meta = metaFactory();
            meta(meta);
        }

        return meta;
    }

    default Object meta(String key) {
        JsonApiMeta meta = meta();
        return meta != null ? meta.get(key) : null;
    }

    default T addMeta(String key, Object value) {
        JsonApiMeta meta = getOrCreateMeta();
        meta.put(key, value);
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return t;
    }
}
