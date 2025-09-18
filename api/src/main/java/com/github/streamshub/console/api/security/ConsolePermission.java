package com.github.streamshub.console.api.security;

import java.security.Permission;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.streamshub.console.config.security.Privilege;

public abstract sealed class ConsolePermission extends Permission
        permits ConsolePermissionPossessed, ConsolePermissionRequired {

    private static final long serialVersionUID = 1L;

    static final String UNMATCHABLE = ConsolePermission.class.getName() + ".UNMATCHABLE";
    public static final String ACTIONS_SEPARATOR = ",";

    private final String resource;
    private final Set<Privilege> actions;

    protected ConsolePermission(String resource, Privilege... actions) {
        super("console");
        this.resource = resource;
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

    protected abstract boolean matchesAnyResourceName();

    protected String resource() {
        return resource;
    }

    protected Set<Privilege> actions() {
        return actions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ConsolePermission other)) {
            return false;
        }

        return resource.equals(other.resource)
                && actions.equals(other.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, actions);
    }

    /**
     * @return null if no actions were specified, or actions joined together with the {@link #ACTIONS_SEPARATOR}
     */
    @Override
    public String getActions() {
        return actions.isEmpty() ? null : actions.stream().map(Enum::name).collect(Collectors.joining(ACTIONS_SEPARATOR));
    }
}
