package com.github.streamshub.console.api.model.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Representation of a JSON API resource relationship linking a resource to another or
 * providing meta information about the relationship.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON API Document Structure, 7.2.2.2 Relationships</a>
 */
@JsonInclude(value = Include.NON_NULL)
public class JsonApiRelationshipToOne extends JsonApiData<Identifier> {

    public JsonApiRelationshipToOne(Identifier data) {
        super(data);
    }
}
