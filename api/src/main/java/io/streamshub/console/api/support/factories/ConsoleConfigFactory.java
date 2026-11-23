package io.streamshub.console.api.support.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.streamshub.console.api.support.UncheckedIO;
import io.streamshub.console.api.support.ValidationProxy;
import io.streamshub.console.config.ConsoleConfig;

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
            .map(uri -> UncheckedIO.call(uri::toURL,
                    () -> "Unable to convert %s to URL".formatted(uri)))
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

        JsonNode tree = UncheckedIO.call(() -> {
            try (InputStream stream = url.openStream()) {
                return yamlMapper.readTree(stream);
            }
        }, () -> "Failed to read configuration YAML");

        // Replace properties specified within string values in the configuration model
        processNode(tree);

        return bindConfig(tree);
    }

    /**
     * Binds the given configuration tree to a {@linkplain ConsoleConfig} instance.
     *
     * By the time this is called, {@link #processNode(JsonNode)} has already replaced
     * any {@code ${...}} placeholders in the tree with their real values (e.g. a
     * secret pulled from the environment). If a value then turns out to be the wrong
     * type for its field, Jackson's error message would normally quote that
     * already-resolved value verbatim, which could leak a secret into the application
     * log. To avoid that, we catch binding failures here and report only the path of
     * the offending property, never the original exception's message.
     */
    ConsoleConfig bindConfig(JsonNode tree) {
        try {
            return mapper.treeToValue(tree, ConsoleConfig.class);
        } catch (JsonMappingException e) {
            throw new UncheckedIOException(
                    "Failed to load configuration model: invalid value at `%s`".formatted(formatPath(e.getPath())),
                    new IOException(e.getClass().getName()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load configuration model", e);
        }
    }

    private static String formatPath(List<Reference> path) {
        var pathString = new StringBuilder();

        for (Reference ref : path) {
            if (ref.getFieldName() != null) {
                if (!pathString.isEmpty()) {
                    pathString.append('.');
                }
                pathString.append(ref.getFieldName());
            } else {
                pathString.append('[').append(ref.getIndex()).append(']');
            }
        }

        return !pathString.isEmpty() ? pathString.toString() : "<root>";
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
