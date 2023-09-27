package com.github.eyefloaters.console.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.eyefloaters.console.api.support.ErrorCategory;

@JsonInclude(Include.NON_NULL)
public abstract class Resource<T> {

    private final String id;
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    private final String type;
    private Map<String, Object> meta;
    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    private final T attributes;

    protected Resource(String id, String type, T attributes) {
        this.id = id;
        this.type = type;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public T getAttributes() {
        return attributes;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public Resource<T> addMeta(String key, Object value) {
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }
        meta.put(key, value);
        return this;
    }
}
