package com.github.streamshub.console.config.security;

import jakarta.validation.Valid;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class GlobalSecurityConfig extends SecurityConfig {

    @Valid
    private OidcConfig oidc;

    public OidcConfig getOidc() {
        return oidc;
    }

    public void setOidc(OidcConfig oidc) {
        this.oidc = oidc;
    }
}
