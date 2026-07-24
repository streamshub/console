package com.github.streamshub.console.api.v1alpha1.spec.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentTemplate {

    @JsonPropertyDescription("Metadata for the deployment template.")
    private MetadataTemplate metadata;

    @JsonPropertyDescription("Spec for the deployment template.")
    private DeploymentSpecTemplate spec;

    public MetadataTemplate getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataTemplate metadata) {
        this.metadata = metadata;
    }

    public DeploymentSpecTemplate getSpec() {
        return spec;
    }

    public void setSpec(DeploymentSpecTemplate spec) {
        this.spec = spec;
    }

}
