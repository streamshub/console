package com.github.streamshub.console.api.v1alpha1.spec.containers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContainerTemplateSpec {

    @JsonPropertyDescription("Specification to be applied to the resulting container.")
    ContainerSpec spec;

    public ContainerSpec getSpec() {
        return spec;
    }

    public void setSpec(ContainerSpec spec) {
        this.spec = spec;
    }
}
