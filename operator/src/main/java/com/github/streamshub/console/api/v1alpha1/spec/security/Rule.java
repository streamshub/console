package com.github.streamshub.console.api.v1alpha1.spec.security;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class Rule {

    @Required
    @JsonPropertyDescription("Resources to which this rule applies (required)")
    List<String> resources;

    @JsonPropertyDescription("""
            Specific resource names to which this rule applies (optional).

            The entries in the array may be one of the following:
            1. the literal name of a resource that must match exactly for
               the rule to apply
            2. a name prefix ending with a wildcard (*) that must match
               the part of a resource name up to the wildcard
            3. a valid regular expression starting and ending with a slash (/).
               The rule is applied to resources that match the pattern.
            """)
    List<String> resourceNames;

    @Required
    @JsonPropertyDescription("Privileges/actions that may be performed for subjects linked to this rule")
    List<Privilege> privileges;

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

    public enum Privilege {
        CREATE,
        DELETE,
        GET,
        LIST,
        UPDATE,
        ALL;

        @JsonCreator
        public static Privilege forValue(String value) {
            if ("*".equals(value)) {
                return ALL;
            }
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

}
