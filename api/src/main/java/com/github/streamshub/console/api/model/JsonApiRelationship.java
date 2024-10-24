package com.github.streamshub.console.api.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of a JSON API resource relationship linking a resource to another or
 * providing meta information about the relationship.
 *
 * @see <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON API Document Structure, 7.2.2.2 Relationships</a>
 */
@JsonInclude(value = Include.NON_NULL)
public class JsonApiRelationship implements HasLinks<JsonApiRelationship>, HasMeta<JsonApiRelationship> {

    private JsonApiMeta meta;
    private Identifier data;
    private Map<String, String> links;

    @JsonProperty
    public JsonApiMeta meta() {
        return meta;
    }

    public void meta(JsonApiMeta meta) {
        this.meta = meta;
    }

    @JsonProperty
    public Map<String, String> links() {
        return links;
    }

    public void links(Map<String, String> links) {
        this.links = links;
    }

    @JsonProperty
    public Identifier data() {
        return data;
    }

    public void data(Identifier data) {
        this.data = data;
    }

}
