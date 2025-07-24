package com.github.streamshub.console.api.v1alpha1.spec.security;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.Value;

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
    private Value clientSecret;
    private List<String> roleClaimPath;

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

    public Value getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(Value clientSecret) {
        this.clientSecret = clientSecret;
    }

    public List<String> getRoleClaimPath() {
        return roleClaimPath;
    }

    public void setRoleClaimPath(List<String> roleClaimPath) {
        this.roleClaimPath = roleClaimPath;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }
}
