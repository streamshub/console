package com.github.streamshub.console.api.security;

import java.security.Permission;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.github.streamshub.console.config.security.Privilege;

public final class ConsolePermissionRequired extends ConsolePermission {

    private static final long serialVersionUID = 1L;
    private static final String TEMPLATE = "%s:%s:[%s]:%s";

    private final transient Optional<String> resourceDisplay;
    private transient Optional<String> resourceName = Optional.empty();
    private transient Optional<String> resourceNameDisplay = Optional.empty();
    private boolean audited = true;

    public ConsolePermissionRequired(String resource, String resourceDisplay, String resourceName, String resourceNameDisplay, Privilege... actions) {
        super(resource, actions);
        this.resourceDisplay = Optional.ofNullable(resourceDisplay);
        this.resourceName = Optional.ofNullable(resourceName);
        this.resourceNameDisplay = Optional.ofNullable(resourceNameDisplay);
    }

    public ConsolePermissionRequired(String resource, String resourceDisplay, Privilege... actions) {
        super(resource, actions);
        this.resourceDisplay = Optional.ofNullable(resourceDisplay);
    }

    ConsolePermissionRequired setResourceName(String resourceName, String resourceNameDisplay) {
        this.resourceName = Optional.ofNullable(resourceName);
        this.resourceNameDisplay = Optional.ofNullable(resourceNameDisplay);
        return this;
    }

    @Override
    protected boolean matchesAnyResourceName() {
        return resourceName.isEmpty();
    }

    public boolean matches(Predicate<String> resourceNamePredicate) {
        return resourceName.map(resourceNamePredicate::test).orElse(Boolean.FALSE);
    }

    public boolean isAudited() {
        return audited;
    }

    public void setAudited(boolean audited) {
        this.audited = audited;
    }

    @Override
    public boolean implies(Permission permission) {
        // A required permission implies nothing.
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof ConsolePermissionRequired other)) {
            return false;
        }

        return Objects.equals(resourceDisplay, other.resourceDisplay)
                && Objects.equals(resourceName, other.resourceName)
                && Objects.equals(resourceNameDisplay, other.resourceNameDisplay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resourceDisplay, resourceName, resourceNameDisplay);
    }

    @Override
    public String toString() {
        return TEMPLATE.formatted(
            getName(),
            resourceDisplay.orElse(resource()),
            resourceNameDisplay.or(() -> resourceName).orElse(""),
            actions()
        );
    }
}
