package com.github.streamshub.console.config;

import java.util.Comparator;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleConfigTest {

    ConsoleConfig config;
    Validator validator;

    @BeforeEach
    void setup() {
        config = new ConsoleConfig();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testRegistryNamesNotUniqueFailsValidation() {
        for (String name : List.of("name1", "name2", "name1")) {
            SchemaRegistryConfig registry = new SchemaRegistryConfig();
            registry.setName(name);
            registry.setUrl("http://example.com");
            config.getSchemaRegistries().add(registry);
        }

        var violations = validator.validate(config);

        assertEquals(1, violations.size());
        assertEquals("Schema registry names must be unique", violations.iterator().next().getMessage());
    }

    @Test
    void testRegistryNamesUniquePassesValidation() {
        for (String name : List.of("name1", "name2", "name3")) {
            SchemaRegistryConfig registry = new SchemaRegistryConfig();
            registry.setName(name);
            registry.setUrl("http://example.com");
            config.getSchemaRegistries().add(registry);
        }

        var violations = validator.validate(config);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testRegistryMissingPropertiesFailsValidation() {
        SchemaRegistryConfig registry = new SchemaRegistryConfig();
        // name and url are null
        config.getSchemaRegistries().add(registry);

        var violations = validator.validate(config).stream()
                .sorted(Comparator.comparing(ConstraintViolation::getMessage))
                .toList();

        assertEquals(2, violations.size());
        assertEquals("Schema registry `name` is required", violations.get(0).getMessage());
        assertEquals("Schema registry `url` is required", violations.get(1).getMessage());
    }

    @Test
    void testKafkaNamesNotUniqueFailsValidation() {
        for (String name : List.of("name1", "name2", "name1")) {
            KafkaClusterConfig cluster = new KafkaClusterConfig();
            cluster.setName(name);
            config.getKafka().getClusters().add(cluster);
        }

        var violations = validator.validate(config);

        assertEquals(1, violations.size());
        assertEquals("Kafka cluster names must be unique", violations.iterator().next().getMessage());
    }

    @Test
    void testKafkaNameMissingFailsValidation() {
        config.getKafka().getClusters().add(new KafkaClusterConfig());

        var violations = validator.validate(config);

        assertEquals(1, violations.size());
        assertEquals("Kafka cluster `name` is required", violations.iterator().next().getMessage());
    }

    @Test
    void testKnownReferenceNamesPassValidation() {
        SchemaRegistryConfig registry = new SchemaRegistryConfig();
        registry.setName("known-registry");
        registry.setUrl("http://example.com");
        config.getSchemaRegistries().add(registry);

        PrometheusConfig metrics = new PrometheusConfig();
        metrics.setName("known-prometheus");
        metrics.setUrl("http://example.com");
        config.getMetricsSources().add(metrics);

        KafkaClusterConfig cluster = new KafkaClusterConfig();
        cluster.setName("name1");
        cluster.setMetricsSource("known-prometheus");
        cluster.setSchemaRegistry("known-registry");
        config.getKafka().getClusters().add(cluster);

        var violations = validator.validate(config);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testUnknownReferenceNamesFailValidation() {
        KafkaClusterConfig cluster = new KafkaClusterConfig();
        cluster.setName("name1");
        cluster.setMetricsSource("unknown-prometheus");
        cluster.setSchemaRegistry("unknown-registry");
        config.getKafka().getClusters().add(cluster);

        var violations = validator.validate(config);

        assertEquals(2, violations.size());
        List<String> messages = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertTrue(messages.contains("Kafka cluster references an unknown metrics source"));
        assertTrue(messages.contains("Kafka cluster references an unknown schema registry"));
    }

    @Test
    void testMetricsSourceNamesNotUniqueFailsValidation() {
        for (String name : List.of("name1", "name2", "name1")) {
            PrometheusConfig metrics = new PrometheusConfig();
            metrics.setName(name);
            metrics.setUrl("http://example.com");
            config.getMetricsSources().add(metrics);
        }

        var violations = validator.validate(config);

        assertEquals(1, violations.size());
        assertEquals("Metrics source names must be unique", violations.iterator().next().getMessage());
    }

    @Test
    void testMetricsSourceNamesUniquePassesValidation() {
        for (String name : List.of("name1", "name2", "name3")) {
            PrometheusConfig metrics = new PrometheusConfig();
            metrics.setName(name);
            metrics.setUrl("http://example.com");
            config.getMetricsSources().add(metrics);
        }

        var violations = validator.validate(config);

        assertTrue(violations.isEmpty());
    }
}
