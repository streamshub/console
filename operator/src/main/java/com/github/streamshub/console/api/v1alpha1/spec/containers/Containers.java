package com.github.streamshub.console.api.v1alpha1.spec.containers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Containers {

    @JsonPropertyDescription("Template for the Console API server container. " +
            "The template allows users to specify how the Kubernetes resources are generated.")
    ContainerTemplateSpec api;

    /**
     * @deprecated the UI container is no longer used
     */
    @Deprecated(forRemoval = true, since = "0.13.0")
    @JsonPropertyDescription("""
            DEPRECATED: The Console UI no longer uses a separate container. This configuration \
            is ignored and will be removed in the next version of the Console custom resource API.
            """)
    ContainerTemplateSpec ui; // NOSONAR

    public ContainerTemplateSpec getApi() {
        return api;
    }

    public void setApi(ContainerTemplateSpec api) {
        this.api = api;
    }

    /**
     * @deprecated the UI container is no longer used
     */
    @Deprecated(forRemoval = true, since = "0.13.0")
    public ContainerTemplateSpec getUi() { // NOSONAR
        return ui;
    }

    /**
     * @deprecated the UI container is no longer used
     */
    @Deprecated(forRemoval = true, since = "0.13.0")
    public void setUi(ContainerTemplateSpec ui) { // NOSONAR
        this.ui = ui;
    }
}
