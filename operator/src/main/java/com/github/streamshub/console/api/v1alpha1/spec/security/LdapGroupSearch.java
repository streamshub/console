package com.github.streamshub.console.api.v1alpha1.spec.security;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LdapGroupSearch {

    @Required
    @JsonPropertyDescription("BaseDN to start the search from, e.g. cn=groups,dc=example,dc=com.")
    private String baseDN;

    @JsonPropertyDescription("Optional LDAP filter to apply, e.g. (objectClass=posixGroup).")
    private String filter;

    @JsonPropertyDescription("Search scope: 'sub' (whole subtree, default) or 'one' (one level).")
    private String scope;

    @JsonPropertyDescription("List of field pairs used to match a user to a group. "
            + "Each entry specifies a user attribute and the corresponding group attribute to compare.")
    private List<LdapUserMatcher> userMatchers;

    @JsonPropertyDescription("Attribute on the group entry to use as the group name.")
    private String nameAttr;

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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<LdapUserMatcher> getUserMatchers() {
        return userMatchers;
    }

    public void setUserMatchers(List<LdapUserMatcher> userMatchers) {
        this.userMatchers = userMatchers;
    }

    public String getNameAttr() {
        return nameAttr;
    }

    public void setNameAttr(String nameAttr) {
        this.nameAttr = nameAttr;
    }
}
