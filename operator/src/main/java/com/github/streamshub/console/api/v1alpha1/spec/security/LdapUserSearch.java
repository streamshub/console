package com.github.streamshub.console.api.v1alpha1.spec.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LdapUserSearch {

    @Required
    @JsonPropertyDescription("BaseDN to start the search from, e.g. cn=users,dc=example,dc=com.")
    private String baseDN;

    @JsonPropertyDescription("Optional LDAP filter to apply, e.g. (objectClass=person).")
    private String filter;

    @Required
    @JsonPropertyDescription("Attribute(s) to match against the entered username.")
    private String username;

    @JsonPropertyDescription("Search scope: 'sub' (whole subtree, default) or 'one' (one level).")
    private String scope;

    @JsonPropertyDescription("Attribute to use as the user ID. Defaults to 'uid'.")
    private String idAttr;

    @JsonPropertyDescription("Attribute to use as the email address. Defaults to 'mail'.")
    private String emailAttr;

    @JsonPropertyDescription("Attribute to use as the display name.")
    private String nameAttr;

    @JsonPropertyDescription("Attribute to use as the preferred username.")
    private String preferredUsernameAttr;

    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getIdAttr() {
        return idAttr;
    }

    public void setIdAttr(String idAttr) {
        this.idAttr = idAttr;
    }

    public String getEmailAttr() {
        return emailAttr;
    }

    public void setEmailAttr(String emailAttr) {
        this.emailAttr = emailAttr;
    }

    public String getNameAttr() {
        return nameAttr;
    }

    public void setNameAttr(String nameAttr) {
        this.nameAttr = nameAttr;
    }

    public String getPreferredUsernameAttr() {
        return preferredUsernameAttr;
    }

    public void setPreferredUsernameAttr(String preferredUsernameAttr) {
        this.preferredUsernameAttr = preferredUsernameAttr;
    }
}
