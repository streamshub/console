package com.github.streamshub.console.api.v1alpha1.spec.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LdapUserMatcher {

    @JsonPropertyDescription("Attribute on the user entry whose value is matched against groupAttr.")
    private String userAttr;

    @JsonPropertyDescription("Attribute on the group entry that contains member values.")
    private String groupAttr;

    public String getUserAttr() {
        return userAttr;
    }

    public void setUserAttr(String userAttr) {
        this.userAttr = userAttr;
    }

    public String getGroupAttr() {
        return groupAttr;
    }

    public void setGroupAttr(String groupAttr) {
        this.groupAttr = groupAttr;
    }
}
