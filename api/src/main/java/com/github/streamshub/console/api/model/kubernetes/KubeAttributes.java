package com.github.streamshub.console.api.model.kubernetes;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KubeAttributes {

    @JsonProperty
    @Schema(readOnly = true)
    private String name;

    @JsonProperty
    @Schema(readOnly = true)
    private String namespace;

    @JsonProperty
    @Schema(readOnly = true)
    private String creationTimestamp;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }
}
