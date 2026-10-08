package com.github.streamshub.console.api.v1alpha1.spec.security;

import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@ValidationRule(
    value = "[has(self.oidc), has(self.openshift), has(self.ldap)].filter(x, x).size() <= 1",
    message = "At most one of `oidc`, `openshift`, or `ldap` may be specified")
public class GlobalSecurity extends Security {

    private Oidc oidc;
    private OpenshiftIdentityProvider openshift;
    private LdapIdentityProvider ldap;

    public Oidc getOidc() {
        return oidc;
    }

    public void setOidc(Oidc oidc) {
        this.oidc = oidc;
    }

    public OpenshiftIdentityProvider getOpenshift() {
        return openshift;
    }

    public void setOpenshift(OpenshiftIdentityProvider openshift) {
        this.openshift = openshift;
    }

    public LdapIdentityProvider getLdap() {
        return ldap;
    }

    public void setLdap(LdapIdentityProvider ldap) {
        this.ldap = ldap;
    }
}
