package com.github.streamshub.console.config;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class KafkaClusterConfig implements Named {

    private String id;
    @NotBlank(message = "Kafka cluster `name` is required")
    private String name;
    private String namespace;
    private String listener;
    /**
     * Name of a configured metrics source used by this Kafka cluster
     */
    private String metricsSource;
    /**
     * Name of a configured schema registry that will be used to ser/des configurations
     * with this Kafka cluster.
     */
    private String schemaRegistry;
    private Map<String, String> properties = new LinkedHashMap<>();
    private Map<String, String> adminProperties = new LinkedHashMap<>();
    private Map<String, String> consumerProperties = new LinkedHashMap<>();
    private Map<String, String> producerProperties = new LinkedHashMap<>();

    @JsonIgnore
    public String clusterKey() {
        return hasNamespace() ? "%s/%s".formatted(namespace, name) : name;
    }

    @JsonIgnore
    public boolean hasNamespace() {
        return namespace != null && !namespace.isBlank();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getListener() {
        return listener;
    }

    public void setListener(String listener) {
        this.listener = listener;
    }

    public String getMetricsSource() {
        return metricsSource;
    }

    public void setMetricsSource(String metricsSource) {
        this.metricsSource = metricsSource;
    }

    public String getSchemaRegistry() {
        return schemaRegistry;
    }

    public void setSchemaRegistry(String schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getAdminProperties() {
        return adminProperties;
    }

    public void setAdminProperties(Map<String, String> adminProperties) {
        this.adminProperties = adminProperties;
    }

    public Map<String, String> getConsumerProperties() {
        return consumerProperties;
    }

    public void setConsumerProperties(Map<String, String> consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public Map<String, String> getProducerProperties() {
        return producerProperties;
    }

    public void setProducerProperties(Map<String, String> producerProperties) {
        this.producerProperties = producerProperties;
    }

}
