package com.github.eyefloaters.console.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.eyefloaters.console.api.support.ErrorCategory;

/**
 * A "resource object", as described by JSON:API.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-objects">JSON API Specification: Resource Objects</a>
 *
 * @param <T> the type of the attribute model
 */
@JsonInclude(Include.NON_NULL)
public abstract class RelatableResource<A, R> {

    protected final String id;
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final String type;
    protected Map<String, Object> meta;

    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final A attributes;

    protected final R relationships;

    protected RelatableResource(String id, String type, A attributes, R relationships) {
        this.id = id;
        this.type = type;
        this.attributes = attributes;
        this.relationships = relationships;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public A getAttributes() {
        return attributes;
    }

    public R getRelationships() {
        return relationships;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public RelatableResource<A, R> addMeta(String key, Object value) {
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }
        meta.put(key, value);
        return this;
    }
}
