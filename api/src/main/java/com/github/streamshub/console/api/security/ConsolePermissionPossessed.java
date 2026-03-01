package com.github.streamshub.console.api.security;

import java.security.Permission;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.security.Privilege;

import static java.util.function.Predicate.not;

public final class ConsolePermissionPossessed extends ConsolePermission {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(ConsolePermissionPossessed.class);

    private final Collection<String> resourceNames;
    private final transient Predicate<String> resourceNamePredicate;

    public ConsolePermissionPossessed(String resource, Collection<String> resourceNames, Privilege... actions) {
        super(resource, actions);
        this.resourceNames = new HashSet<>(resourceNames);
        boolean allUnmatchable = allUnmatchable(this.resourceNames);
        this.resourceNames.removeIf(UNMATCHABLE::equals);

        if (allUnmatchable) {
            this.resourceNamePredicate = x -> false;
        } else if (this.resourceNames.isEmpty()) {
            this.resourceNamePredicate = null;
        } else {
            resourceNamePredicate = resourceNames.stream()
                    .filter(Objects::nonNull)
                    .map(name -> {
                        Predicate<String> predicate;

                        if (isDelimitedRegex(name)) {
                            predicate = regexPredicate(name);
                        } else if (name.endsWith("*")) {
                            String match = name.substring(0, name.length() - 1);
                            predicate = value -> value.startsWith(match);
                        } else {
                            predicate = name::equals;
                        }

                        return predicate;
                    })
                    .reduce(x -> false, Predicate::or);
        }
    }

    static boolean isDelimitedRegex(String value) {
        return value.length() > 1 && value.startsWith("/") && value.endsWith("/");
    }

    private boolean allUnmatchable(Collection<String> resourceNames) {
        return !resourceNames.isEmpty() && resourceNames.stream().allMatch(UNMATCHABLE::equals);
    }

    private Predicate<String> regexPredicate(String name) {
        Pattern pattern = Pattern.compile(name.substring(1, name.length() - 1));
        return value -> {
            boolean matched = pattern.matcher(value).matches();
            if (log.isDebugEnabled()) {
                String matchMsg = matched ? "matched" : "did not match";
                log.debugf("Resource %s %s regex pattern %s", value, matchMsg, pattern);
            }
            return matched;
        };
    }

    @Override
    protected boolean matchesAnyResourceName() {
        return Objects.isNull(resourceNamePredicate);
    }

    @Override
    public boolean implies(Permission other) {
        return other instanceof ConsolePermissionRequired required
                && this.implies(required);
    }

    boolean implies(ConsolePermissionRequired requiredPermission) {
        if (resourceDenied(requiredPermission)) {
            return false;
        }

        if (actions().contains(Privilege.ALL)) {
            // all actions possessed
            return true;
        }

        for (Privilege action : requiredPermission.actions()) {
            if (actions().contains(action)) {
                // has at least one of required actions
                return true;
            }
        }

        return false;
    }

    boolean resourceDenied(ConsolePermissionRequired requiredPermission) {
        /*
         * The action requires a permission unrelated to this configured
         * permission.
         * E.g. groups versus topics
         */
        if (!requiredPermission.resource().equals(resource())) {
            return true;
        }

        if (this.matchesAnyResourceName()) {
            /*
             * Configuration does not specify any resource names, so
             * access to any is allowed.
             */
            return false;
        }

        if (requiredPermission.matchesAnyResourceName()) {
            /*
             * Configuration specifies named resources, but this request
             * has no resource name. I.e., the request is for an index/list
             * end point. The permission is granted here, but individual
             * resources in the list response may be filtered later.
             */
            return false;
        }

        /*
         * Deny when the required name does not match the configuration.
         */
        return requiredPermission.matches(not(resourceNamePredicate));
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof ConsolePermissionPossessed other)) {
            return false;
        }

        return Objects.equals(resourceNames, other.resourceNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resourceNames);
    }
}
