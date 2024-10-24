package com.github.streamshub.console.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.xlate.validation.constraints.Expression;

@Expression(
    message = "Kafka cluster references an unknown schema registry",
    value = """
        registryNames = self.schemaRegistries.stream()
            .map(registry -> registry.getName())
            .toList();
        self.kafka.clusters.stream()
            .map(cluster -> cluster.getSchemaRegistry())
            .filter(registry -> registry != null)
            .allMatch(registry -> registryNames.contains(registry))
        """)
public class ConsoleConfig {

    KubernetesConfig kubernetes = new KubernetesConfig();

    @Valid
    List<SchemaRegistryConfig> schemaRegistries = new ArrayList<>();

    @Valid
    KafkaConfig kafka = new KafkaConfig();

    @JsonIgnore
    @AssertTrue(message = "Schema registry names must be unique")
    public boolean hasUniqueRegistryNames() {
        return schemaRegistries.stream().map(SchemaRegistryConfig::getName).distinct().count() == schemaRegistries.size();
    }

    public KubernetesConfig getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesConfig kubernetes) {
        this.kubernetes = kubernetes;
    }

    public List<SchemaRegistryConfig> getSchemaRegistries() {
        return schemaRegistries;
    }

    public void setSchemaRegistries(List<SchemaRegistryConfig> schemaRegistries) {
        this.schemaRegistries = schemaRegistries;
    }

    public KafkaConfig getKafka() {
        return kafka;
    }

    public void setKafka(KafkaConfig kafka) {
        this.kafka = kafka;
    }
}
