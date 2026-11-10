package com.github.streamshub.console.api.v1alpha1.spec.security;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@ValidationRule(
    value = "[has(self.oidc), has(self.openshift), has(self.ldap)].filter(x, x).size() <= 1",
    message = "At most one of `oidc`, `openshift`, or `ldap` may be specified")
public class GlobalSecurity extends Security {

    private Oidc oidc;

    @JsonPropertyDescription("""
            Use the OpenShift cluster's OAuth server as the identity source. The operator deploys \
            and configures a Dex identity broker for this purpose. Mutually exclusive with \
            `oidc` and `ldap`. \
            Note: on plain Kubernetes there is no TLS between the Ingress and the Console or \
            Dex pods; TLS is terminated by the Ingress controller (see `spec.tls`).
            """)
    private OpenshiftIdentityProvider openshift;

    @JsonPropertyDescription("""
            Use an LDAP directory as the identity source. The operator deploys and configures \
            a Dex identity broker for this purpose. Mutually exclusive with `oidc` and \
            `openshift`. \
            Note: on plain Kubernetes there is no TLS between the Ingress and the Console or \
            Dex pods; TLS is terminated by the Ingress controller (see `spec.tls`).
            """)
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
