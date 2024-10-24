package com.github.streamshub.console.api.security;

import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.streamshub.console.config.security.Privilege;

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
        Set<Privilege> validActions = new HashSet<>(actions.length, 1);
        for (Privilege action : actions) {
            validActions.add(validateAndTrim(action, "Action"));
        }
        return Collections.unmodifiableSet(validActions);
    }

    private static Privilege validateAndTrim(Privilege action, String paramName) {
        if (action == null) {
            throw new IllegalArgumentException(String.format("%s must not be null", paramName));
        }

        return action;
    }

    public String resource() {
        return resource;
    }

    public ConsolePermission resource(String resource) {
        this.resource = resource;
        return this;
    }

    public ConsolePermission resourceName(String resourceName) {
        this.resourceNames = Collections.singleton(resourceName);
        return this;
    }

    @Override
    public boolean implies(Permission requiredPermission) {
        if (requiredPermission instanceof ConsolePermission other) {
            if (!getName().equals(other.getName())) {
                return false;
            }

            return implies(other);
        } else {
            return false;
        }
    }

    boolean implies(ConsolePermission requiredPermission) {
        if (!requiredPermission.resource.startsWith(resource)) {
            return false;
        }

        if (requiredPermission.resource.equals(resource)) {
            if (!requiredPermission.resourceNames.isEmpty()
                    && !resourceNames.isEmpty()
                    && requiredPermission.resourceNames.stream().noneMatch(resourceNames::contains)) {
                return false;
            }
        } else if (requiredPermission.resourceNames.isEmpty() && !resourceNames.isEmpty()) {
            boolean matches = false;
            for (String name : resourceNames) {
                String fullName = resource + '/' + name;
                if (fullName.equals(requiredPermission.resource)) {
                    matches = true;
                }
            }
            if (!matches) {
                return false;
            }
        } else {
            return false;
        }

        // actions are optional, however if at least one action was specified,
        // an intersection of compared sets must not be empty
        if (requiredPermission.actions.isEmpty()) {
            // no required actions
            return true;
        }

        if (actions.isEmpty()) {
            // no possessed actions
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
        return getName() + ":" + resource() + ":" + resourceNames + ":" + actions;
    }

    /**
     * @return null if no actions were specified, or actions joined together with the {@link #ACTIONS_SEPARATOR}
     */
    @Override
    public String getActions() {
        return actions.isEmpty() ? null : actions.stream().map(Enum::name).collect(Collectors.joining(ACTIONS_SEPARATOR));
    }
}
