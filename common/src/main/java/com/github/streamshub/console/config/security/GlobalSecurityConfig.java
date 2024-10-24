package com.github.streamshub.console.config.security;

public class GlobalSecurityConfig extends SecurityConfig {

    private OidcConfig oidc;

    public OidcConfig getOidc() {
        return oidc;
    }

    public void setOidc(OidcConfig oidc) {
        this.oidc = oidc;
    }
}
