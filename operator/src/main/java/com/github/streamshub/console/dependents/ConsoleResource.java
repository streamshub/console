package com.github.streamshub.console.dependents;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;

public interface ConsoleResource {

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

    default <T extends HasMetadata> T load(Context<Console> context, String resourceName, Class<T> resourceType) {
        try (InputStream base = ConsoleResource.class.getResourceAsStream(resourceName)) {
            return context.getClient().resources(resourceType).load(base).item();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default <T> Optional<T> getOptionalAttribute(Context<Console> context, String key, Class<T> type) {
        return context.managedDependentResourceContext().get(key, type);
    }

    default <T> T getAttribute(Context<Console> context, String key, Class<T> type) {
        return getOptionalAttribute(context, key, type)
                .orElseThrow(() -> new NoSuchElementException("No such attribute: " + key));
    }

    default <T> void setAttribute(Context<Console> context, String key, T value) {
        context.managedDependentResourceContext().put(key, value);
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
            throw new RuntimeException(e);
        }
    }

    default void updateDigest(Context<Console> context, String digestName, Map<String, String> data) {
        var digest = getOptionalAttribute(context, digestName, MessageDigest.class)
                .orElseGet(this::messageDigest);

        data.entrySet()
            .stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .forEach(entry -> digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8)));

        setAttribute(context, digestName, digest);
    }

    default String serializeDigest(Context<Console> context, String digestName) {
        var resourceContext = context.managedDependentResourceContext();
        return resourceContext.get(digestName, MessageDigest.class)
                .map(MessageDigest::digest)
                .map(DIGEST_FORMAT::formatHex)
                .orElse(DEFAULT_DIGEST);
    }

    default String encodeString(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    default String decodeString(String encodedValue) {
        return new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8);
    }

    default <T> List<T> coalesce(List<T> value, Supplier<List<T>> defaultValue) {
        return value != null ? value : defaultValue.get();
    }


}
