package com.github.streamshub.console.api.v1alpha1.spec.containers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Containers {

    @JsonPropertyDescription("Template for the Console API server container. " +
            "The template allows users to specify how the Kubernetes resources are generated.")
    ContainerTemplateSpec api;

    @JsonPropertyDescription("Template for the Console UI server container. " +
            "The template allows users to specify how the Kubernetes resources are generated.")
    ContainerTemplateSpec ui;

    public ContainerTemplateSpec getApi() {
        return api;
    }

    public void setApi(ContainerTemplateSpec api) {
        this.api = api;
    }

    public ContainerTemplateSpec getUi() {
        return ui;
    }

    public void setUi(ContainerTemplateSpec ui) {
        this.ui = ui;
    }
}
