package com.github.streamshub.console.api.v1alpha1.spec.security;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class GlobalSecurity extends Security {

    private Oidc oidc;

    public Oidc getOidc() {
        return oidc;
    }

    public void setOidc(Oidc oidc) {
        this.oidc = oidc;
    }
}
