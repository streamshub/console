package com.github.streamshub.console.api.v1alpha1.spec.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class Rule {

    /**
     * Resources to which this rule applies (required)
     */
    @Required
    List<String> resources;

    /**
     * Specific resource names to which this rule applies (optional)
     */
    List<String> resourceNames;

    /**
     * Privileges/actions that may be performed for subjects having this rule
     */
    @Required
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
        ALL {
            @Override
            public Set<Privilege> expand() {
                return ALL_EXPANDED;
            }
        };

        private static final Set<Privilege> ALL_EXPANDED = Arrays.stream(Privilege.values())
                .filter(Predicate.not(ALL::equals))
                .collect(Collectors.toSet());

        @JsonCreator
        public static Privilege forValue(String value) {
            if ("*".equals(value)) {
                return ALL;
            }
            return valueOf(value.toUpperCase(Locale.ROOT));
        }

        public Set<Privilege> expand() {
            return Collections.singleton(this);
        }
    }

}
