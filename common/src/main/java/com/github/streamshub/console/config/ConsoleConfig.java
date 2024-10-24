package com.github.streamshub.console.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ConsoleConfig {

    KubernetesConfig kubernetes = new KubernetesConfig();
    List<SchemaRegistryConfig> schemaRegistries = new ArrayList<>();
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
