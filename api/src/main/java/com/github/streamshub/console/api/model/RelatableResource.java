package com.github.streamshub.console.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A "resource object", as described by JSON:API.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-objects">JSON API Specification: Resource Objects</a>
 *
 * @param <A> the type of the attribute model
 * @param <R> the type of the relationships model
 */
@JsonInclude(Include.NON_NULL)
public abstract class RelatableResource<A, R> extends Resource<A> {

    protected final R relationships;

    protected RelatableResource(String id, String type, A attributes, R relationships) {
        super(id, type, attributes);
        this.relationships = relationships;
    }

    protected RelatableResource(String id, String type, JsonApiMeta meta, A attributes, R relationships) {
        super(id, type, meta, attributes);
        this.relationships = relationships;
    }

    public R getRelationships() {
        return relationships;
    }
}
