package com.github.streamshub.console.api.support.factories;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

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
import com.github.streamshub.console.api.support.TrustStoreSupport;
import com.github.streamshub.console.api.support.UncheckedIO;
import com.github.streamshub.console.api.support.ValidationProxy;
import com.github.streamshub.console.config.ConsoleConfig;

@Singleton
public class ConsoleConfigFactory {

    @Inject
    @ConfigProperty(name = "console.config-path")
    Optional<String> configPath;

    @Inject
    Logger log;

    @Inject
    Config config;

    @Inject
    ObjectMapper mapper;

    @Inject
    ValidationProxy validationService;

    @Inject
    TrustStoreSupport trustStores;

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
            .map(trustStores::registerTrustStores)
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
            for (var field : node.properties()) {
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
}
