package com.github.streamshub.console.config.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.sundr.builder.annotations.Buildable;
import io.xlate.validation.constraints.Expression;

@Buildable(editableEnabled = false)
public class RuleConfig {

    static final String INVALID_RESOURCE_NAME = "Invalid resource name. Values delimited by '/' must be valid regular expressions.";

    /**
     * Resources to which this rule applies (required)
     */
    @NotEmpty
    List<@NotNull String> resources = new ArrayList<>();

    /**
     * Specific resource names to which this rule applies (optional)
     */
    List<
        @NotNull
        @Expression(
            staticImports = "com.github.streamshub.console.config.security.RuleConfig.validResourceName",
            targetName = "entry",
            when = "entry != null",
            value = "validResourceName(entry)",
            message = INVALID_RESOURCE_NAME
        )
        String
        > resourceNames = new ArrayList<>();

    /**
     * Privileges/actions that may be performed for subjects having this rule
     */
    @NotEmpty
    List<@NotNull Privilege> privileges = new ArrayList<>();

    @JsonIgnore
    public static boolean validResourceName(String value) {
        if (value.length() > 1 && value.startsWith("/") && value.endsWith("/")) {
            // When the value has RegExp delimiters, it must be able to be parsed
            try {
                Pattern.compile(value);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public List<String> getResourceNames() {
        return resourceNames;
    }

    @JsonSetter
    public void setResourceNames(List<String> resourceNames) {
        this.resourceNames = Objects.requireNonNullElseGet(resourceNames, ArrayList::new);
    }

    public List<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<Privilege> privileges) {
        this.privileges = privileges;
    }
}
