package com.github.streamshub.console.api.support.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            .map(url -> {
                log.infof("Loading console configuration from %s", url);
                ObjectMapper yamlMapper = mapper.copyWith(new YAMLFactory());

                try (InputStream stream = url.openStream()) {
                    return yamlMapper.readValue(stream, ConsoleConfig.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .map(consoleConfig -> {
                consoleConfig.getSchemaRegistries().forEach(registry -> {
                    registry.setUrl(resolveValue(registry.getUrl()));
                });

                consoleConfig.getKafka().getClusters().forEach(cluster -> {
                    resolveValues(cluster.getProperties());
                    resolveValues(cluster.getAdminProperties());
                    resolveValues(cluster.getProducerProperties());
                    resolveValues(cluster.getConsumerProperties());
                });

                return consoleConfig;
            })
            .map(validationService::validate)
            .orElseGet(() -> {
                log.warn("Console configuration has not been specified using `console.config-path` property");
                return new ConsoleConfig();
            });
    }

    private void resolveValues(Map<String, String> properties) {
        properties.entrySet().forEach(entry ->
            entry.setValue(resolveValue(entry.getValue())));
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
