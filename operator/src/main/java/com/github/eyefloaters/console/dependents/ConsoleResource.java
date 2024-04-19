package com.github.eyefloaters.console.dependents;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;

interface ConsoleResource {

    static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    static final String NAME_LABEL = "app.kubernetes.io/name";
    static final String INSTANCE_LABEL = "app.kubernetes.io/instance";
    static final String MANAGER = "streamshub-console-operator";

    static final Map<String, String> MANAGEMENT_LABEL = Map.of(MANAGED_BY_LABEL, MANAGER);
    static final String MANAGEMENT_SELECTOR = MANAGED_BY_LABEL + '=' + MANAGER;

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
        Map<String, String> labels = new LinkedHashMap<>();
        labels.putAll(MANAGEMENT_LABEL);
        labels.put(NAME_LABEL, appName);
        return labels;
    }

    private MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
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
                .map(digest -> String.format("%040x", new BigInteger(1, digest.digest())))
                .orElseGet(() -> "0".repeat(40));
    }
}
