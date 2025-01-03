package com.github.streamshub.console.api.v1alpha1.spec.security;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class Oidc {

    @Required
    private String authServerUrl;
    private String issuer;
    @Required
    private String clientId;
    @Required
    private String clientSecret;

    @JsonPropertyDescription("""
            Trust store configuration for when the OIDC provider uses \
            TLS certificates signed by an unknown CA.
            """)
    private TrustStore trustStore;

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }
}
