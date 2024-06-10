package com.github.streamshub.console.config.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class SubjectConfig {

    private String claim;

    @NotEmpty
    private List<@NotNull String> include = new ArrayList<>();

    @NotEmpty
    private List<@NotNull String> roleNames = new ArrayList<>();

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public List<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(List<String> roleNames) {
        this.roleNames = roleNames;
    }

}
