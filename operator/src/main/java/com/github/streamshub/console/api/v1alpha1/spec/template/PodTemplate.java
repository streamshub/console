package com.github.streamshub.console.api.v1alpha1.spec.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PodTemplate {

    @JsonPropertyDescription("""
            Labels and annotations to apply to the console pod.
            """)
    private MetadataTemplate metadata;

    @JsonPropertyDescription("""
            Spec for the console pod. Allows configuration of scheduling constraints \
            (affinity, tolerations, topology spread constraints, and node selector) \
            and the server container image, pull policy, resources, and environment \
            variables.
            """)
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
