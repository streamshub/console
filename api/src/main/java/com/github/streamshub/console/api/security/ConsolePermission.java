package com.github.streamshub.console.api.security;

import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.security.Privilege;

import static java.util.function.Predicate.not;

public final class ConsolePermission extends Permission {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(ConsolePermission.class);

    static final String UNMATCHABLE = ConsolePermission.class.getName() + ".UNMATCHABLE";

    public static final String ACTIONS_SEPARATOR = ",";

    private final String resource;
    private final transient Optional<String> resourceDisplay;
    private Collection<String> resourceNames;
    private transient Optional<Collection<String>> resourceNamesDisplay = Optional.empty();
    private final Set<Privilege> actions;
    private transient Predicate<String> resourceNamePredicate;

    public ConsolePermission(String resource, String resourceDisplay, Collection<String> resourceNames, Privilege... actions) {
        super("console");
        this.resource = resource;
        this.resourceDisplay = Optional.ofNullable(resourceDisplay);
        this.actions = checkActions(actions);
        setResourceNames(resourceNames);
    }

    public ConsolePermission(String resource, Collection<String> resourceNames, Privilege... actions) {
        this(resource, null, resourceNames, actions);
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
        setResourceNames(Collections.singleton(resourceName));
        return this;
    }

    ConsolePermission resourceNamesDisplay(Collection<String> resourceNamesDisplay) {
        this.resourceNamesDisplay = Optional.ofNullable(resourceNamesDisplay);
        return this;
    }

    static boolean isDelimitedRegex(String value) {
        return value.length() > 1 && value.startsWith("/") && value.endsWith("/");
    }

    private boolean allUnmatchable(Collection<String> resourceNames) {
        return !resourceNames.isEmpty() && resourceNames.stream().allMatch(UNMATCHABLE::equals);
    }

    private void setResourceNames(Collection<String> resourceNames) {
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

    Predicate<String> regexPredicate(String name) {
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

    private boolean matchesAnyResourceName() {
        return Objects.isNull(resourceNamePredicate);
    }

    @Override
    public boolean implies(Permission other) {
        return other instanceof ConsolePermission requiredPermission
                && implies(requiredPermission);
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
         * Deny when any of the required names are not given in configuration.
         */
        return requiredPermission.resourceNames.stream().anyMatch(not(resourceNamePredicate));
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
                && actions.equals(other.actions)
                && Objects.equals(resourceNames, other.resourceNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, actions, resourceNames);
    }

    @Override
    public String toString() {
        return getName() + ":" + resourceDisplay.orElse(resource) + ":" + resourceNamesDisplay.orElse(resourceNames) + ":" + actions;
    }

    /**
     * @return null if no actions were specified, or actions joined together with the {@link #ACTIONS_SEPARATOR}
     */
    @Override
    public String getActions() {
        return actions.isEmpty() ? null : actions.stream().map(Enum::name).collect(Collectors.joining(ACTIONS_SEPARATOR));
    }
}
