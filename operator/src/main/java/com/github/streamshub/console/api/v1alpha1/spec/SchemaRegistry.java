package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaRegistry {

    @Required
    @JsonPropertyDescription("URL of the Apicurio Registry server API.")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
