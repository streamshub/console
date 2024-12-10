package com.github.streamshub.console.api.security;

import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.streamshub.console.config.security.Privilege;

import static java.util.function.Predicate.not;

public class ConsolePermission extends Permission {

    private static final long serialVersionUID = 1L;
    public static final String ACTIONS_SEPARATOR = ",";

    private String resource;
    private Collection<String> resourceNames;
    private final Set<Privilege> actions;

    public ConsolePermission(String resource, Privilege... actions) {
        super("console");
        this.resource = resource;
        this.resourceNames = Collections.emptySet();
        this.actions = checkActions(actions);
    }

    public ConsolePermission(String resource, Collection<String> resourceNames, Privilege... actions) {
        super("console");
        this.resource = resource;
        this.resourceNames = resourceNames;
        this.actions = checkActions(actions);
    }

    private static Set<Privilege> checkActions(Privilege[] actions) {
        Objects.requireNonNull(actions);

        if (actions.length == 0) {
            throw new IllegalArgumentException("actions must not be zero length");
        }

        Set<Privilege> validActions = new HashSet<>(actions.length, 1);

        for (Privilege action : actions) {
            validActions.add(Objects.requireNonNull(action));
        }

        return Collections.unmodifiableSet(validActions);
    }

    ConsolePermission resourceName(String resourceName) {
        this.resourceNames = Collections.singleton(resourceName);
        return this;
    }

    @Override
    public boolean implies(Permission other) {
        if (other instanceof ConsolePermission requiredPermission) {
            if (!getName().equals(requiredPermission.getName())) {
                return false;
            }

            return implies(requiredPermission);
        } else {
            return false;
        }
    }

    boolean implies(ConsolePermission requiredPermission) {
        if (resourceDenied(requiredPermission)) {
            return false;
        }

        if (actions.contains(Privilege.ALL)) {
            // all actions possessed
            return true;
        }

        for (Privilege action : requiredPermission.actions) {
            if (actions.contains(action)) {
                // has at least one of required actions
                return true;
            }
        }

        return false;
    }

    boolean resourceDenied(ConsolePermission requiredPermission) {
        /*
         * The action requires a permission unrelated to this configured
         * permission.
         * E.g. consumerGroups versus topics
         */
        if (!requiredPermission.resource.equals(resource)) {
            return true;
        }

        if (resourceNames.isEmpty()) {
            /*
             * Configuration does not specify any resource names, so
             * access to any is allowed.
             */
            return false;
        }

        if (requiredPermission.resourceNames.isEmpty()) {
            /*
             * Configuration specifies named resources, but this request
             * has no resource name. I.e., the request is for an index/list
             * end point. The permission is granted here, but individual
             * resources in the list response may be filtered later.
             */
            return false;
        }

        /*
         * Deny when any of the required names are not given in configuration.
         */
        return requiredPermission.resourceNames.stream().anyMatch(not(this::matchesResourceName));
    }

    boolean matchesResourceName(String requiredName) {
        if (resourceNames.contains(requiredName)) {
            return true;
        }

        return resourceNames.stream()
                .filter(n -> n.endsWith("*"))
                .map(n -> n.substring(0, n.length() - 1))
                .anyMatch(requiredName::startsWith);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ConsolePermission other)) {
            return false;
        }

        return getName().equals(other.getName())
                && resource.equals(other.resource)
                && actions.equals(other.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), resource, actions);
    }

    @Override
    public String toString() {
        return getName() + ":" + resource + ":" + resourceNames + ":" + actions;
    }

    /**
     * @return null if no actions were specified, or actions joined together with the {@link #ACTIONS_SEPARATOR}
     */
    @Override
    public String getActions() {
        return actions.isEmpty() ? null : actions.stream().map(Enum::name).collect(Collectors.joining(ACTIONS_SEPARATOR));
    }
}
