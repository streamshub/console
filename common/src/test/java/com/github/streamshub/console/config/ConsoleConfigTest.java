package com.github.streamshub.console.config;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.GlobalSecurityConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
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
        assertEquals(KafkaConfig.UNIQUE_NAMES_MESSAGE, violations.iterator().next().getMessage());
    }

    @Test
    void testKafkaNamesWithUniqueNamespacesPassValidation() {
        for (String name : List.of("name1", "name2", "name1")) {
            KafkaClusterConfig cluster = new KafkaClusterConfig();
            cluster.setName(name);
            cluster.setNamespace(UUID.randomUUID().toString());
            config.getKafka().getClusters().add(cluster);
        }

        var violations = validator.validate(config);

        assertTrue(violations.isEmpty());
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

    @ParameterizedTest
    @CsvSource({
        "'default/name1/false', 'default/name1/false'",
    })
    void testKafkaConnectNotUniqueFailsValidation(String kc1, String kc2) {
        for (String kcString : List.of(kc1, kc2)) {
            String[] elements = kcString.split(Pattern.quote("/"));
            KafkaConnectConfig kcc = new KafkaConnectConfig();
            kcc.setName(elements[0]);
            kcc.setNamespace(elements[1]);
            kcc.setMirrorMaker(Boolean.valueOf(elements[2]));
            kcc.setUrl("http://example.com");
            kcc.getKafkaClusters().add("my-kafka");
            config.getKafkaConnectClusters().add(kcc);
        }

        var violations = validator.validate(config);

        assertEquals(1, violations.size());
        assertEquals(
            "Kafka Connect name, namespace, and mirrorMaker combinations must be unique",
            violations.iterator().next().getMessage()
        );
    }

    @ParameterizedTest
    @CsvSource({
        "'default/name1/false', 'default/name1/true'",
        "'default/name1/false', 'custom-namespace/name1/false'",
        "'default/name1/false', 'default/name2/false'",
    })
    void testKafkaConnectUniquePassesValidation(String kc1, String kc2) {
        for (String kcString : List.of(kc1, kc2)) {
            String[] elements = kcString.split(Pattern.quote("/"));
            KafkaConnectConfig kcc = new KafkaConnectConfig();
            kcc.setName(elements[0]);
            kcc.setNamespace(elements[1]);
            kcc.setMirrorMaker(Boolean.valueOf(elements[2]));
            kcc.setUrl("http://example.com");
            kcc.getKafkaClusters().add("my-kafka");
            config.getKafkaConnectClusters().add(kcc);
        }

        var violations = validator.validate(config);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testKnownResourceTypesPassValidation() {
        config.setSecurity(new GlobalSecurityConfigBuilder()
                .addNewAudit()
                    .withDecision(Decision.ALLOWED)
                    .withResources(ResourceTypes.Global.KAFKAS.value())
                    .withPrivileges(Privilege.forValue("*"))
                .endAudit()
                .addNewRole()
                    .withName("role1")
                    .addNewRule()
                        .withResources(ResourceTypes.Global.KAFKAS.value())
                        .withPrivileges(Privilege.forValue("*"))
                    .endRule()
                .endRole()
            .build());

        config.getKafka().getClusters().add(new KafkaClusterConfigBuilder()
                .withName("kafka1")
                .withNewSecurity()
                    .addNewAudit()
                        .withDecision(Decision.ALLOWED)
                        .withResources(ResourceTypes.Kafka.ALL.value())
                        .withPrivileges(Privilege.forValue("*"))
                    .endAudit()
                    .addNewRole()
                        .withName("role1")
                        .addNewRule()
                            .withResources(ResourceTypes.Kafka.ALL.value())
                            .withPrivileges(Privilege.forValue("*"))
                        .endRule()
                    .endRole()
                .endSecurity()
            .build());

        var violations = validator.validate(config);
        assertTrue(violations.isEmpty(), () -> String.valueOf(violations));
    }

    @Test
    void testKnownResourceTypesFailValidation() {
        String unknownResource = "unknown";

        config.setSecurity(new GlobalSecurityConfigBuilder()
                .addNewAudit()
                    .withDecision(Decision.ALLOWED)
                    .withResources(
                            ResourceTypes.Global.KAFKAS.value(),
                            unknownResource)
                    .withPrivileges(Privilege.forValue("*"))
                .endAudit()
                .addNewRole()
                    .withName("role1")
                    .addNewRule()
                        .withResources(ResourceTypes.Global.KAFKAS.value())
                        .withPrivileges(Privilege.forValue("*"))
                    .endRule()
                    .addNewRule()
                        .withResources(
                                unknownResource,
                                ResourceTypes.Global.KAFKAS.value())
                        .withPrivileges(Privilege.forValue("*"))
                    .endRule()
                .endRole()
            .build());

        config.getKafka().getClusters().add(new KafkaClusterConfigBuilder()
                .withName("kafka1")
                .withNewSecurity()
                    .addNewAudit()
                        .withDecision(Decision.ALLOWED)
                        .withResources(ResourceTypes.Kafka.ALL.value())
                        .withPrivileges(Privilege.forValue("CREATE"))
                    .endAudit()
                    .addNewAudit()
                        .withDecision(Decision.DENIED)
                        .withResources(unknownResource)
                        .withPrivileges(Privilege.forValue("DELETE"))
                    .endAudit()
                    .addNewAudit()
                        .withDecision(Decision.ALL)
                        .withResources(ResourceTypes.Kafka.GROUPS.value(), unknownResource)
                        .withPrivileges(Privilege.forValue("UPDATE"))
                    .endAudit()
                    .addNewRole()
                        .withName("role1")
                        .addNewRule()
                            .withResources(ResourceTypes.Kafka.NODE_CONFIGS.value())
                            .withPrivileges(Privilege.forValue("*"))
                        .endRule()
                        .addNewRule()
                            .withResources(unknownResource, ResourceTypes.Kafka.ALL.value())
                            .withPrivileges(Privilege.forValue("*"))
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName("role2")
                        .addNewRule()
                            .withResources(ResourceTypes.Kafka.GROUPS.value(), unknownResource)
                            .withPrivileges(Privilege.forValue("*"))
                        .endRule()
                    .endRole()
                .endSecurity()
            .build());

        var violations = validator.validate(config);
        assertEquals(6, violations.size(), () -> String.valueOf(violations));
        assertThat(violations, everyItem(hasProperty("message", equalTo("Invalid resource"))));

        var propertyPaths = violations.stream().map(v -> v.getPropertyPath().toString()).toList();
        assertThat(propertyPaths, hasItem(equalTo("security.audit[0].resources[1]")));
        assertThat(propertyPaths, hasItem(equalTo("security.roles[0].rules[1].resources[0]")));
        assertThat(propertyPaths, hasItem(equalTo("kafka.clusters[0].security.audit[1].resources[0]")));
        assertThat(propertyPaths, hasItem(equalTo("kafka.clusters[0].security.audit[2].resources[1]")));
        assertThat(propertyPaths, hasItem(equalTo("kafka.clusters[0].security.roles[0].rules[1].resources[0]")));
        assertThat(propertyPaths, hasItem(equalTo("kafka.clusters[0].security.roles[1].rules[0].resources[1]")));
    }
}
