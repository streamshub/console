package com.github.streamshub.console.dependents.support;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.cache.Cache;

/**
 * Utility to lazily load display names of resources, primarily intended
 * for use in log statements.
 */
public final class DisplayName {

    private DisplayName() {
        // No instances
    }

    public static String of(HasMetadata source) {
        return Cache.metaNamespaceKeyFunc(source);
    }
}
