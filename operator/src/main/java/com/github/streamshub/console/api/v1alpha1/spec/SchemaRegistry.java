package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaRegistry {

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

    @JsonPropertyDescription("""
            Trust store configuration for when the schema registry uses \
            TLS certificates signed by an unknown CA.
            """)
    private TrustStore trustStore;

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

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }
}
