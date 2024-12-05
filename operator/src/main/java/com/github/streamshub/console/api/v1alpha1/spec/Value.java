package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Value {

    @JsonProperty("value")
    @JsonPropertyDescription("Literal string to be used for this value")
    private String value; // NOSONAR

    @JsonProperty("valueFrom")
    @JsonPropertyDescription("Reference to an external source to use for this value")
    private ValueReference valueFrom;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ValueReference getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(ValueReference valueFrom) {
        this.valueFrom = valueFrom;
    }

}
