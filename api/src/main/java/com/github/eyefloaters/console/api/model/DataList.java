package com.github.eyefloaters.console.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataList<T> extends JsonApiDocument {

    private final List<T> data;

    public DataList() {
        this(new ArrayList<>());
    }

    protected DataList(List<T> data) {
        this.data = data;
    }

    @JsonProperty
    public List<T> data() {
        return data;
    }

}
