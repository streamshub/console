package com.github.streamshub.console.api.v1alpha1.spec.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PodTemplate {

    @JsonPropertyDescription("Metadata for the pod template.")
    private MetadataTemplate metadata;

    @JsonPropertyDescription("Spec for the pod template.")
    private PodSpecTemplate spec;

    public MetadataTemplate getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataTemplate metadata) {
        this.metadata = metadata;
    }

    public PodSpecTemplate getSpec() {
        return spec;
    }

    public void setSpec(PodSpecTemplate spec) {
        this.spec = spec;
    }
}
