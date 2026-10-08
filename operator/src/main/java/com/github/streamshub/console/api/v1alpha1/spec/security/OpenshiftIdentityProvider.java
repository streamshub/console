package com.github.streamshub.console.api.v1alpha1.spec.security;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenshiftIdentityProvider {

    @JsonPropertyDescription("Restrict logins to members of these groups. Empty means all groups are allowed.")
    private List<String> groups;

    @JsonPropertyDescription("If true, TLS certificate verification is skipped when contacting the OpenShift API server.")
    private Boolean insecureCA;

    @JsonPropertyDescription("""
            Trust store configuration for when the OpenShift API server uses \
            TLS certificates signed by an unknown CA.
            """)
    private TrustStore trustStore;

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public Boolean isInsecureCA() {
        return insecureCA;
    }

    public void setInsecureCA(Boolean insecureCA) {
        this.insecureCA = insecureCA;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }
}
