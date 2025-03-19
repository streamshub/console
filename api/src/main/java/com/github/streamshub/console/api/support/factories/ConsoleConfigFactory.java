package com.github.streamshub.console.api.support.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.console.api.support.TrustedTlsConfiguration;
import com.github.streamshub.console.api.support.ValidationProxy;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.Trustable;

import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;

@Singleton
public class ConsoleConfigFactory {

    public static final String TRUST_PREFIX_METRICS = "metrics-source:";
    public static final String TRUST_PREFIX_SCHEMA_REGISTRY = "schema-registry:";
    public static final String TRUST_PREFIX_OIDC_PROVIDER = "oidc-provider:";

    @Inject
    @ConfigProperty(name = "console.config-path")
    Optional<String> configPath;

    @Inject
    Logger log;

    @Inject
    Config config;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Inject
    Vertx vertx;

    @Inject
    ObjectMapper mapper;

    @Inject
    ValidationProxy validationService;

    // Note: extract this class and use generally where IOExceptions are simply re-thrown
    interface UncheckedIO<R> {
        R call() throws IOException;

        static <R> R call(UncheckedIO<R> io, Supplier<String> exceptionMessage) {
            try {
                return io.call();
            } catch (IOException e) {
                throw new UncheckedIOException(exceptionMessage.get(), e);
            }
        }
    }

    @Produces
    @ApplicationScoped
    public ConsoleConfig produceConsoleConfig() {
        return configPath.map(Path::of)
            .map(Path::toUri)
            .map(uri -> UncheckedIO.call(uri::toURL,
                    () -> "Unable to convert %s to URL".formatted(uri)))
            .filter(Objects::nonNull)
            .map(this::loadConfiguration)
            .map(validationService::validate)
            .map(this::registerTrustStores)
            .orElseGet(() -> {
                log.warn("Console configuration has not been specified using `console.config-path` property");
                return new ConsoleConfig();
            });
    }

    private ConsoleConfig loadConfiguration(URL url) {
        log.infof("Loading console configuration from %s", url);
        ObjectMapper yamlMapper = mapper.copyWith(new YAMLFactory());

        JsonNode tree = UncheckedIO.call(() -> {
            try (InputStream stream = url.openStream()) {
                return yamlMapper.readTree(stream);
            }
        }, () -> "Failed to read configuration YAML");

        // Replace properties specified within string values in the configuration model
        processNode(tree);

        return UncheckedIO.call(
                () -> mapper.treeToValue(tree, ConsoleConfig.class),
                () -> "Failed to load configuration model");
    }

    private void processNode(JsonNode node) {
        if (node.isArray()) {
            int i = 0;
            for (JsonNode entry : node) {
                processNode((ArrayNode) node, i++, entry);
            }
        } else if (node.isObject()) {
            for (var cursor = node.fields(); cursor.hasNext();) {
                var field = cursor.next();
                processNode((ObjectNode) node, field.getKey(), field.getValue());
            }
        }
    }

    private void processNode(ObjectNode parent, String key, JsonNode node) {
        if (node.isValueNode()) {
            if (node.isTextual()) {
                parent.put(key, resolveValue(node.asText()));
            }
        } else {
            processNode(node);
        }
    }

    private void processNode(ArrayNode parent, int position, JsonNode node) {
        if (node.isValueNode()) {
            if (node.isTextual()) {
                parent.set(position, resolveValue(node.asText()));
            }
        } else {
            processNode(node);
        }
    }

    /**
     * If the given value is an expression referencing a configuration value,
     * replace it with the target property value.
     *
     * @param value configuration value that may be a reference to another
     *              configuration property
     * @return replacement property or the same value if the given string is not a
     *         reference.
     */
    private String resolveValue(String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            String replacement = value.substring(2, value.length() - 1);
            return config.getOptionalValue(replacement, String.class).orElse(value);
        }

        return value;
    }

    private ConsoleConfig registerTrustStores(ConsoleConfig config) {
        registerTrustStores(TRUST_PREFIX_METRICS, config.getMetricsSources());
        registerTrustStores(TRUST_PREFIX_SCHEMA_REGISTRY, config.getSchemaRegistries());

        var oidcConfig = config.getSecurity().getOidc();
        if (oidcConfig != null) {
            registerTrustStores(TRUST_PREFIX_OIDC_PROVIDER, List.of(oidcConfig));
        }

        return config;
    }

    private void registerTrustStores(String prefix, List<? extends Trustable> trustables) {
        for (var source : trustables) {
            var trustStore = source.getTrustStore();
            if (trustStore != null) {
                String name = prefix + source.getName();
                var tlsConfig = new TrustedTlsConfiguration(name, vertx, trustStore);
                this.tlsRegistry.register(name, tlsConfig);
            }
        }
    }
}
