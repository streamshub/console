package com.github.streamshub.console.api.v1alpha1.spec;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.streamshub.console.api.v1alpha1.spec.containers.Containers;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;
import com.github.streamshub.console.api.v1alpha1.spec.security.GlobalSecurity;

import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
// Enable validation rules for unique names when array maxItems and string maxLength can be specified
// to influence Kubernetes's estimated rule cost.
// https://github.com/fabric8io/kubernetes-client/pull/6447
//
// @ValidationRule(value = """
//         !has(self.metricsSources) ||
//           self.metricsSources.all(s1, self.metricsSources.exists_one(s2, s2.name == s1.name))
//         """,
//         message = "Metrics source names must be unique")
public class ConsoleSpec {

    @Required
    String hostname;

    @JsonPropertyDescription("""
            Templates for Console instance containers. The templates allow \
            users to specify how the Kubernetes resources are generated.
            """)
    Containers containers;

    @JsonPropertyDescription("""
            DEPRECATED: Image overrides to be used for the API and UI servers. \
            Use `containers` property instead.
            """)
    Images images;

    GlobalSecurity security;

    List<MetricsSource> metricsSources;

    List<SchemaRegistry> schemaRegistries;

    List<KafkaCluster> kafkaClusters = new ArrayList<>();

    List<KafkaConnect> kafkaConnectClusters;

    @JsonPropertyDescription("""
            DEPRECATED: Environment variables which should be applied to the API container. \
            Use `containers` property instead.
            """)
    List<EnvVar> env;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Containers getContainers() {
        return containers;
    }

    public void setContainers(Containers containers) {
        this.containers = containers;
    }

    public Images getImages() {
        return images;
    }

    public void setImages(Images images) {
        this.images = images;
    }

    public GlobalSecurity getSecurity() {
        return security;
    }

    public void setSecurity(GlobalSecurity security) {
        this.security = security;
    }

    public List<MetricsSource> getMetricsSources() {
        return metricsSources;
    }

    public void setMetricsSources(List<MetricsSource> metricsSources) {
        this.metricsSources = metricsSources;
    }

    public List<SchemaRegistry> getSchemaRegistries() {
        return schemaRegistries;
    }

    public void setSchemaRegistries(List<SchemaRegistry> schemaRegistries) {
        this.schemaRegistries = schemaRegistries;
    }

    public List<KafkaCluster> getKafkaClusters() {
        return kafkaClusters;
    }

    public void setKafkaClusters(List<KafkaCluster> kafkaClusters) {
        this.kafkaClusters = kafkaClusters;
    }

    public List<KafkaConnect> getKafkaConnectClusters() {
        return kafkaConnectClusters;
    }

    public void setKafkaConnectClusters(List<KafkaConnect> kafkaConnectClusters) {
        this.kafkaConnectClusters = kafkaConnectClusters;
    }

    public List<EnvVar> getEnv() {
        return env;
    }

    public void setEnv(List<EnvVar> env) {
        this.env = env;
    }
}
