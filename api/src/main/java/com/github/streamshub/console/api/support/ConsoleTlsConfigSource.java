package com.github.streamshub.console.api.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A MicroProfile {@link ConfigSource} that inspects the console configuration YAML at startup
 * and, when a {@code tls} block is present, automatically sets
 * {@code quarkus.http.insecure-requests=disabled} so that the HTTP server only binds on the
 * SSL port (8443) and rejects plain HTTP connections.
 * <p>
 * This source is loaded by the {@code ServiceLoader} mechanism very early in the Quarkus
 * runtime bootstrap — before the Vert.x HTTP server recorder reads its configuration — so the
 * property is visible to all subsequent configuration consumers including the HTTP server setup.
 * <p>
 * The config path is resolved in priority order:
 * <ol>
 *   <li>System property {@code console.config-path} — set as a system property by the Quarkus
 *       test framework when a test profile overrides it via {@code getConfigOverrides()}, or
 *       passed on the command line as {@code -Dconsole.config-path=...}.</li>
 *   <li>Environment variable {@code CONSOLE_CONFIG_PATH} — the canonical production mechanism
 *       used by the operator deployment YAML.</li>
 * </ol>
 */
public class ConsoleTlsConfigSource implements ConfigSource {

    private static final String INSECURE_REQUESTS_KEY = "quarkus.http.insecure-requests";

    /**
     * Ordinal above the default application.properties ordinal (250) so this source wins when
     * TLS is detected, but below explicit environment-variable overrides (300+).
     */
    private static final int ORDINAL = 275;

    private final Map<String, String> properties;

    public ConsoleTlsConfigSource() {
        this.properties = Map.of(INSECURE_REQUESTS_KEY, tlsConfigured() ? "disabled" : "enabled");
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "ConsoleTlsConfigSource";
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }

    /**
     * Resolves the console config YAML path and parses it just enough to detect a top-level
     * {@code tls} field, returning true when found.
     */
    private static boolean tlsConfigured() {
        String configPath = resolveConfigPath();

        if (configPath != null && !configPath.isBlank()) {
            try (InputStream in = Files.newInputStream(Path.of(configPath))) {
                JsonNode root = new ObjectMapper(new YAMLFactory()).readTree(in);
                return root != null && root.hasNonNull("tls");
            } catch (IOException e) {
                // Unreadable / malformed YAML — leave the default behaviour alone
            }
        }

        return false;
    }

    private static String resolveConfigPath() {
        // System property takes precedence (test profiles, -D flags)
        String value = System.getProperty("console.config-path");
        if (value == null) {
            // Fall back to the environment variable used by the operator deployment
            value = System.getenv("CONSOLE_CONFIG_PATH");
        }
        return value;
    }
}
