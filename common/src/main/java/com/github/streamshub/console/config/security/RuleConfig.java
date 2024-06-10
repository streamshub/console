package com.github.streamshub.console.config.security;

import java.util.ArrayList;
import java.util.List;

public class RuleConfig {

    /**
     * Resources to which this rule applies (required)
     */
    List<String> resources = new ArrayList<>();

    /**
     * Specific resource names to which this rule applies (optional)
     */
    List<String> resourceNames = new ArrayList<>();

    /**
     * Privileges/actions that may be performed for subjects having this rule
     */
    List<Privilege> privileges = new ArrayList<>();

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public List<String> getResourceNames() {
        return resourceNames;
    }

    public void setResourceNames(List<String> resourceNames) {
        this.resourceNames = resourceNames;
    }

    public List<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<Privilege> privileges) {
        this.privileges = privileges;
    }

}
