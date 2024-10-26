package com.github.streamshub.console.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.xlate.validation.constraints.Expression;

@Expression(
    message = "Kafka cluster references an unknown metrics source",
    value = """
        metricsSources = self.metricsSources.stream()
            .map(metrics -> metrics.getName())
            .toList();
        self.kafka.clusters.stream()
            .map(cluster -> cluster.getMetricsSource())
            .filter(source -> source != null)
            .allMatch(source -> metricsSources.contains(source))
        """)
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
@JsonInclude(Include.NON_NULL)
public class ConsoleConfig {

    KubernetesConfig kubernetes = new KubernetesConfig();

    @Valid
    List<PrometheusConfig> metricsSources = new ArrayList<>();

    @Valid
    List<SchemaRegistryConfig> schemaRegistries = new ArrayList<>();

    @Valid
    KafkaConfig kafka = new KafkaConfig();

    @JsonIgnore
    @AssertTrue(message = "Metrics source names must be unique")
    public boolean hasUniqueMetricsSourceNames() {
        return Named.uniqueNames(metricsSources);
    }

    @JsonIgnore
    @AssertTrue(message = "Schema registry names must be unique")
    public boolean hasUniqueRegistryNames() {
        return Named.uniqueNames(schemaRegistries);
    }

    public KubernetesConfig getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesConfig kubernetes) {
        this.kubernetes = kubernetes;
    }

    public List<PrometheusConfig> getMetricsSources() {
        return metricsSources;
    }

    public void setMetricsSources(List<PrometheusConfig> metricsSources) {
        this.metricsSources = metricsSources;
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
