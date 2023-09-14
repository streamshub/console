package com.github.eyefloaters.console.api.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class DataListResponse<T> {

    private Map<String, Object> meta;
    private final List<T> data;

    protected DataListResponse(List<T> data) {
        this.data = data;
    }

    public List<T> getData() {
        return data;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public DataListResponse<T> addMeta(String key, Object value) {
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }
        meta.put(key, value);
        return this;
    }
}
