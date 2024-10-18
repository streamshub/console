package com.github.streamshub.console.api.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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
