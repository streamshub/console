package com.github.streamshub.console.dependents;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;

public interface ConsoleResource<R extends HasMetadata> extends DependentResource<R, Console> {

    static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    static final String NAME_LABEL = "app.kubernetes.io/name";
    static final String COMPONENT_LABEL = "app.kubernetes.io/component";
    static final String INSTANCE_LABEL = "app.kubernetes.io/instance";
    static final String MANAGER = "streamshub-console-operator";

    public static final Map<String, String> MANAGEMENT_LABEL = Map.of(MANAGED_BY_LABEL, MANAGER);
    static final String MANAGEMENT_SELECTOR = MANAGED_BY_LABEL + '=' + MANAGER;
    static final HexFormat DIGEST_FORMAT = HexFormat.of();
    static final String DEFAULT_DIGEST = "0".repeat(40);

    String resourceName();

    default String instanceName(Console primary) {
        return primary.getMetadata().getName() + "-" + resourceName();
    }

    @Override
    default Optional<R> getSecondaryResource(Console primary, Context<Console> context) {
        String instanceName = instanceName(primary);

        return context.getSecondaryResourcesAsStream(resourceType())
                .filter(resource -> Objects.equals(instanceName, resource.getMetadata().getName()))
                .findFirst();
    }

    default <T extends HasMetadata> T load(Context<Console> context, String resourceName, Class<T> resourceType) {
        try (InputStream base = ConsoleResource.class.getResourceAsStream(resourceName)) {
            return context.getClient().resources(resourceType).load(base).item();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default <T> Optional<T> getOptionalAttribute(Context<Console> context, String key, Class<T> type) {
        return context.managedWorkflowAndDependentResourceContext().get(key, type);
    }

    default <T> T getAttribute(Context<Console> context, String key, Class<T> type) {
        return getOptionalAttribute(context, key, type)
                .orElseThrow(() -> new NoSuchElementException("No such attribute: " + key));
    }

    default <T> void setAttribute(Context<Console> context, String key, T value) {
        context.managedWorkflowAndDependentResourceContext().put(key, value);
    }

    default Map<String, String> commonLabels(String appName) {
        return commonLabels(appName, null);
    }

    default Map<String, String> commonLabels(String appName, String componentName) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.putAll(MANAGEMENT_LABEL);
        labels.put(NAME_LABEL, appName);
        if (componentName != null) {
            labels.put(COMPONENT_LABEL, componentName);
        }
        return labels;
    }

    private MessageDigest messageDigest() {
        try {
            // Ignore java:S4790, SHA-1 is not used in sensitive context
            return MessageDigest.getInstance("SHA-1"); // NOSONAR
        } catch (NoSuchAlgorithmException e) {
            throw new ReconciliationException("Failed to get SHA-1 MessageDigest", e);
        }
    }

    /**
     * Name of the workflow context attribute holding this resource's content digest,
     * as stored by {@link #putDigest} and read by the Deployment that must restart
     * when the content changes.
     */
    default String digestName() {
        return resourceName() + "-digest";
    }

    /**
     * Compute a digest of the values in {@code data}, ordered by key, and store the
     * hex-encoded result in the workflow context under {@code digestName}. Consumers
     * (typically a Deployment that must restart when the data changes) retrieve it
     * with {@link #getOptionalAttribute}. The digest is calculated from a single map
     * and stored in one step, so it does not depend on the order or concurrency of
     * other workflow nodes.
     */
    default void putDigest(Context<Console> context, String digestName, Map<String, String> data) {
        var digest = messageDigest();

        data.entrySet()
            .stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .forEach(entry -> digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8)));

        setAttribute(context, digestName, DIGEST_FORMAT.formatHex(digest.digest()));
    }

    default String encodeString(String value) {
        return ConfigSupport.encodeString(value);
    }

    default String decodeString(String encodedValue) {
        return ConfigSupport.decodeString(encodedValue);
    }

    default <T> List<T> coalesce(List<T> value, Supplier<List<T>> defaultValue) {
        return value != null ? value : defaultValue.get();
    }
}
