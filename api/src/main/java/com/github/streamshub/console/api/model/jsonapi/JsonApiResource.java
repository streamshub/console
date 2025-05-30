package com.github.streamshub.console.api.model.jsonapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.api.support.ErrorCategory;

/**
 * A "resource object", as described by JSON:API.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-objects">JSON API Specification: Resource Objects</a>
 *
 * @param <A> the type of the attribute model
 * @param <R> the type of the relationships model
 */
@JsonInclude(Include.NON_NULL)
public abstract class JsonApiResource<A, R> extends JsonApiBase {

    @Schema(required = true)
    protected String id;

    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final String type;

    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    protected final A attributes;

    protected final R relationships;

    protected JsonApiResource(String id, String type, JsonApiMeta meta, A attributes, R relationships) {
        super(meta);
        this.id = id;
        this.type = type;
        this.attributes = attributes;
        this.relationships = relationships;
    }

    protected JsonApiResource(String id, String type, JsonApiMeta meta, A attributes) {
        this(id, type, meta, attributes, null);
    }

    protected JsonApiResource(String id, String type, A attributes, R relationships) {
        this.id = id;
        this.type = type;
        this.attributes = attributes;
        this.relationships = relationships;
    }

    protected JsonApiResource(String id, String type, A attributes) {
        this(id, type, attributes, null);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Identifier identifier() {
        return new Identifier(type, id);
    }

    public boolean hasIdentifier(Identifier identifier) {
        return identifier.equals(type, id);
    }

    public A getAttributes() {
        return attributes;
    }

    public R getRelationships() {
        return relationships;
    }
}
