package com.github.streamshub.console.api.v1alpha1.spec;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.api.v1alpha1.spec.authentication.Authentication;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaConnect {

    @Required
    @JsonPropertyDescription("""
            Name of the Kafka Connect cluster. When the console is connected to
            Kubernetes, a KafkaConnect custom resource may be discovered using
            this property together with the namespace property.
            """)
    private String name;

    @JsonPropertyDescription("""
            The namespace of the Kafka Connect cluster. When the console is connected to
            Kubernetes, a Strimzi KafkaConnect custom resource may be discovered using
            this property together with the name property.
            """)
    private String namespace;

    @Required
    @JsonPropertyDescription("URL of the Kafka Connect REST API endpoint.")
    private String url;

    @JsonPropertyDescription("""
            Indicates whether this Kafka Connect cluster is a MirrorMaker 2 deployment.
            This affects how certain resources are displayed and managed by the console.
            """)
    private Boolean mirrorMaker;

    @JsonPropertyDescription("Authentication configuration to use when connecting to the Kafka Connect REST API.")
    private Authentication authentication;

    @JsonPropertyDescription("""
            Trust store configuration used to verify TLS connections to the Kafka Connect
            REST API when certificates are signed by an unknown CA.
            """)
    private TrustStore trustStore;

    @JsonPropertyDescription("""
            Names of Kafka clusters configured in the console that this Kafka Connect
            cluster is associated with. Must contain at least one entry.
            """)
    private List<String> kafkaClusters = new ArrayList<>();

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getMirrorMaker() {
        return mirrorMaker;
    }

    public void setMirrorMaker(Boolean mirrorMaker) {
        this.mirrorMaker = mirrorMaker;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }

    public List<String> getKafkaClusters() {
        return kafkaClusters;
    }

    public void setKafkaClusters(List<String> kafkaClusters) {
        this.kafkaClusters = kafkaClusters;
    }
}
