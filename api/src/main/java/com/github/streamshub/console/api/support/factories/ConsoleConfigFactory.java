package com.github.streamshub.console.api.support.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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

    @Produces
    @ApplicationScoped
    public ConsoleConfig produceConsoleConfig() {
        return configPath.map(Path::of)
            .map(Path::toUri)
            .map(uri -> {
                try {
                    return uri.toURL();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .filter(Objects::nonNull)
            .map(this::loadConfiguration)
            .map(validationService::validate)
            .orElseGet(() -> {
                log.warn("Console configuration has not been specified using `console.config-path` property");
                return new ConsoleConfig();
            });
    }

    private ConsoleConfig loadConfiguration(URL url) {
        log.infof("Loading console configuration from %s", url);
        ObjectMapper yamlMapper = mapper.copyWith(new YAMLFactory());
        JsonNode tree;

        try (InputStream stream = url.openStream()) {
            tree = yamlMapper.readTree(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read configuration YAML", e);
        }

        processNode(tree);

        try {
            return mapper.treeToValue(tree, ConsoleConfig.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load configuration model", e);
        }
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
}
