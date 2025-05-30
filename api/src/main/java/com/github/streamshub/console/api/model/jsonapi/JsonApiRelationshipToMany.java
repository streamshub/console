package com.github.streamshub.console.api.model.jsonapi;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Representation of a JSON API resource relationship linking a resource to another or
 * providing meta information about the relationship.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON API Document Structure, 7.2.2.2 Relationships</a>
 */
@JsonInclude(value = Include.NON_NULL)
public class JsonApiRelationshipToMany extends JsonApiData<List<Identifier>> {

    public JsonApiRelationshipToMany() {
        this(new ArrayList<>());
    }

    public JsonApiRelationshipToMany(List<Identifier> data) {
        super(data);
    }
}
