package com.github.eyefloaters.console.dependents;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;

interface ConsoleResource {

    static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    static final String NAME_LABEL = "app.kubernetes.io/name";
    static final String MANAGER = "streamshub-console-operator";

    static final Map<String, String> MANAGEMENT_LABEL = Map.of(MANAGED_BY_LABEL, MANAGER);
    static final String MANAGEMENT_SELECTOR = MANAGED_BY_LABEL + '=' + MANAGER;

    default <T extends HasMetadata> T load(Context<Console> context, String resourceName, Class<T> resourceType) {
        try (InputStream base = ConsoleResource.class.getResourceAsStream(resourceName)) {
            return context.getClient().resources(resourceType).load(base).item();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
