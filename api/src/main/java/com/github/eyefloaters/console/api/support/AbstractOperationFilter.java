package com.github.eyefloaters.console.api.support;

import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Base functionality leveraged by sub-filters that selectively enable or
 * disable REST operations. This class will determine whether an operationId
 * with a given HTTP method are enabled, per application configuration.
 *
 * <p>Generally, read-only operations (identified by HTTP methods GET, HEAD, and
 * OPTIONS) are enabled by default unless disabled explicitly with
 * {@code console.operations.<operationId>.enabled=false}. Write operations
 * (identified by any other HTTP method) may be disabled by setting
 * {@code console.read-only=true} and selectively enabled with
 * {@code console.operations.<operationId>.enabled=true}.
 *
 * @see EnabledOperationFilter
 * @see OASModelFilter
 *
 */
abstract class AbstractOperationFilter {

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
