package com.github.streamshub.console.api.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.PropertiesConfigSource;

/**
 * A SmallRye Config {@link ConfigSourceFactory} that inspects the console configuration YAML
 * at startup and, when a {@code tls} block is present, automatically sets
 * {@code quarkus.http.insecure-requests=disabled} so that the HTTP server only binds on the
 * SSL port (8443) and rejects plain HTTP connections.
 * <p>
 * This source is loaded by the {@code ServiceLoader} mechanism very early in the Quarkus
 * runtime bootstrap — before the Vert.x HTTP server recorder reads its configuration — so the
 * property is visible to all subsequent configuration consumers including the HTTP server setup.
 * <p>
 * The config path is resolved by checking previously-loaded SmallRye Config sources provided
 * to the {@link getConfigSources(ConfigSourceContext context)} method.
 */
public class ConsoleTlsConfigSource implements ConfigSourceFactory {

    private static final String INSECURE_REQUESTS_KEY = "quarkus.http.insecure-requests";

    /**
     * Ordinal above the default application.properties ordinal (250) so this source wins when
     * TLS is detected, but below explicit environment-variable overrides (300+).
     */
    private static final int ORDINAL = 275;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        String insecureRequests = tlsConfigured(context) ? "disabled" : "enabled";
        var properties =  Map.of(INSECURE_REQUESTS_KEY, insecureRequests);
        return List.of(new PropertiesConfigSource(properties, "ConsoleTlsConfigSource", ORDINAL));
    }

    /**
     * Resolves the console config YAML path and parses it just enough to detect a top-level
     * {@code tls} field, returning true when found.
     */
    private static boolean tlsConfigured(ConfigSourceContext context) {
        String configPath = Optional.ofNullable(context.getValue("console.config-path"))
                .map(ConfigValue::getValue)
                .orElse("");

        if (!configPath.isBlank()) {
            try (InputStream in = Files.newInputStream(Path.of(configPath))) {
                JsonNode root = new ObjectMapper(new YAMLFactory()).readTree(in);
                return root != null && root.hasNonNull("tls");
            } catch (IOException e) {
                // Unreadable / malformed YAML — leave the default behaviour alone
            }
        }

        return false;
    }
}
