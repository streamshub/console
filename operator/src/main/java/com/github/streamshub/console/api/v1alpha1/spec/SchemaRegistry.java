package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.config.Named;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaRegistry implements Named {

    @Required
    @JsonPropertyDescription("""
            Name of the Apicurio Registry. The name may be referenced by Kafka clusters \
            configured in the console to indicate that a particular registry is to be \
            used for message deserialization when browsing topics within that cluster.
            """)
    private String name;

    @Required
    @JsonPropertyDescription("URL of the Apicurio Registry server API.")
    private String url;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
