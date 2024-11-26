package com.github.streamshub.console.api.v1alpha1.spec.containers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Containers {

    @JsonPropertyDescription("Template for the Console API server container. " +
            "The template allows users to specify how the Kubernetes resources are generated.")
    ContainerTemplate api;

    @JsonPropertyDescription("Template for the Console UI server container. " +
            "The template allows users to specify how the Kubernetes resources are generated.")
    ContainerTemplate ui;

    public ContainerTemplate getApi() {
        return api;
    }

    public void setApi(ContainerTemplate api) {
        this.api = api;
    }

    public ContainerTemplate getUi() {
        return ui;
    }

    public void setUi(ContainerTemplate ui) {
        this.ui = ui;
    }
}
