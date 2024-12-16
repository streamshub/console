package com.github.streamshub.console.config.security;

import jakarta.validation.constraints.NotBlank;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class OidcConfig {

    private String tenantId = "streamshub-console";
    @NotBlank
    private String authServerUrl;
    private String issuer;
    @NotBlank
    private String clientId;
    @NotBlank
    private String clientSecret;

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

}
