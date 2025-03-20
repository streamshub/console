package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigVar {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
