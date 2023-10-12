package com.github.eyefloaters.console.api.support;

import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

class AbstractOperationFilter {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    protected final Config config = ConfigProvider.getConfig();

    protected boolean disabled(String method, String operationdId) {
        boolean enabled;

        if (READ_METHODS.contains(method)) {
            // Read-only operations enabled unless explicitly disabled
            enabled = operationEnabled(operationdId, true);
        } else if (config.getOptionalValue("console.read-only", Boolean.class).orElse(false)) {
            // Write operations are disabled when read-only config is set unless explicitly enabled
            enabled = operationEnabled(operationdId, false);
        } else {
            enabled = operationEnabled(operationdId, true);
        }

        return !enabled;
    }

    protected boolean operationEnabled(String operationId, boolean defaultValue) {
        String operationKey = "console.operations.%s.enabled".formatted(operationId);
        return config.getOptionalValue(operationKey, Boolean.class).orElse(defaultValue);
    }
}
