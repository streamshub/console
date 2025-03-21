package com.github.streamshub.console.config.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.config.Trustable;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class OidcConfig implements Trustable {

    public static final String NAME = "auth-server";

    private String tenantId = "streamshub-console";
    @NotBlank
    private String authServerUrl;
    private String issuer;
    @NotBlank
    private String clientId;
    @NotBlank
    private String clientSecret;
    @Valid
    private TrustStoreConfig trustStore;

    @Override
    @JsonIgnore
    public String getName() {
        return NAME;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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

    public TrustStoreConfig getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStoreConfig trustStore) {
        this.trustStore = trustStore;
    }
}
