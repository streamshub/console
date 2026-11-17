package com.github.streamshub.console.api.v1alpha1.spec.networking;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.api.v1alpha1.spec.template.MetadataTemplate;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteTemplate {

    @JsonPropertyDescription("Metadata to apply to the generated Route.")
    private MetadataTemplate metadata;

    public MetadataTemplate getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataTemplate metadata) {
        this.metadata = metadata;
    }
}
