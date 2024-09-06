package com.github.streamshub.console.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.support.ErrorCategory;

/**
 * A "resource object", as described by JSON:API.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-objects">JSON API Specification: Resource Objects</a>
 *
 * @param <T> the type of the attribute model
 */
@JsonInclude(Include.NON_NULL)
public abstract class Resource<T> implements HasMeta<T> {

    protected String id;

    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final String type;

    @Valid
    protected JsonApiMeta meta;

    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final T attributes;

    protected Resource(String id, String type, JsonApiMeta meta, T attributes) {
        this.id = id;
        this.type = type;
        this.meta = meta;
        this.attributes = attributes;
    }

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

    @JsonProperty
    public JsonApiMeta meta() {
        return meta;
    }

    public void meta(JsonApiMeta meta) {
        this.meta = meta;
    }
}
