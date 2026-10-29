package com.github.streamshub.console.api.v1alpha1.spec.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.Value;

import io.fabric8.generator.annotation.Required;
import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidationRule(
    value = "!(has(self.insecureNoSSL) && self.insecureNoSSL && has(self.startTLS) && self.startTLS)",
    message = "`insecureNoSSL` and `startTLS` are mutually exclusive")
@ValidationRule(
    value = "!(has(self.insecureNoSSL) && self.insecureNoSSL && has(self.trustStore))",
    message = "`trustStore` has no effect when `insecureNoSSL` is true")
@ValidationRule(
    value = "!(has(self.insecureSkipVerify) && self.insecureSkipVerify && has(self.trustStore))",
    message = "`trustStore` has no effect when `insecureSkipVerify` is true")
@ValidationRule(
    value = "!has(self.bindSecret) || has(self.bindDN)",
    message = "`bindDN` is required when `bindSecret` is specified")
public class LdapIdentityProvider {

    @Required
    @JsonPropertyDescription("Host and optional port of the LDAP server (e.g. ldap.example.com:636).")
    private String host;

    @JsonPropertyDescription("DN to bind with for user/group searches.")
    private String bindDN;

    @JsonPropertyDescription("Password for the bind DN.")
    private Value bindSecret;

    @JsonPropertyDescription("If true, connect without TLS. Required when the server does not support TLS.")
    private Boolean insecureNoSSL;

    @JsonPropertyDescription("If true, TLS certificate verification is skipped.")
    private Boolean insecureSkipVerify;

    @JsonPropertyDescription("If true, initiate a StartTLS upgrade on a plain connection instead of using LDAPS.")
    private Boolean startTLS;

    @JsonPropertyDescription("""
            Trust store configuration for when the LDAP server uses \
            TLS certificates signed by an unknown CA.
            """)
    private TrustStore trustStore;

    @JsonPropertyDescription("Configuration for searching and matching user entries.")
    private LdapUserSearch userSearch;

    @JsonPropertyDescription("Configuration for searching and matching group entries.")
    private LdapGroupSearch groupSearch;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBindDN() {
        return bindDN;
    }

    public void setBindDN(String bindDN) {
        this.bindDN = bindDN;
    }

    public Value getBindSecret() {
        return bindSecret;
    }

    public void setBindSecret(Value bindSecret) {
        this.bindSecret = bindSecret;
    }

    public Boolean isInsecureNoSSL() {
        return insecureNoSSL;
    }

    public void setInsecureNoSSL(Boolean insecureNoSSL) {
        this.insecureNoSSL = insecureNoSSL;
    }

    public Boolean isInsecureSkipVerify() {
        return insecureSkipVerify;
    }

    public void setInsecureSkipVerify(Boolean insecureSkipVerify) {
        this.insecureSkipVerify = insecureSkipVerify;
    }

    public Boolean isStartTLS() {
        return startTLS;
    }

    public void setStartTLS(Boolean startTLS) {
        this.startTLS = startTLS;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }

    public LdapUserSearch getUserSearch() {
        return userSearch;
    }

    public void setUserSearch(LdapUserSearch userSearch) {
        this.userSearch = userSearch;
    }

    public LdapGroupSearch getGroupSearch() {
        return groupSearch;
    }

    public void setGroupSearch(LdapGroupSearch groupSearch) {
        this.groupSearch = groupSearch;
    }
}
