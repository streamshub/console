package com.github.streamshub.console.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.api.support.ErrorCategory;

/**
 * A "resource object", as described by JSON:API.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-objects">JSON API Specification: Resource Objects</a>
 *
 * @param <T> the type of the attribute model
 */
@JsonInclude(Include.NON_NULL)
public abstract class Resource<T> {

    protected String id;
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final String type;
    protected Map<String, Object> meta;
    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final T attributes;

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

    public Object getMeta(String key) {
        return meta != null ? meta.get(key) : null;
    }

    public Resource<T> addMeta(String key, Object value) {
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }
        meta.put(key, value);
        return this;
    }
}
