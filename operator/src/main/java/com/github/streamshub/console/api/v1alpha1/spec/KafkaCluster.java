package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Required;
import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidationRule(
        // The `namespace` property must be wrapped in double underscore to escape it
        // due to it being a "reserved" word.
        value = "has(self.id) || has(self.__namespace__)",
        message = "One of `id` or `namespace` is required")
@ValidationRule(
        // The `namespace` property must be wrapped in double underscore to escape it
        // due to it being a "reserved" word.
        value = "!has(self.listener) || has(self.__namespace__)",
        message = "Property `listener` may not be used when `namespace` is omitted")
public class KafkaCluster {

    @JsonPropertyDescription("""
            Identifier to be used for this Kafka cluster in the console. When \
            the console is connected to Kubernetes and a Strimzi Kafka custom \
            resource may be discovered using the name and namespace properties, \
            this property is optional. Otherwise, the Kafka cluster identifier \
            published in the Kafka resource's status will be used. If namespace \
            is not given or the console or Kubernetes is not in use, this property \
            is required.

            When provided, this property will override the Kafka cluster id available \
            in the Kafka resource's status.""")
    private String id;

    @Required
    @JsonPropertyDescription("""
            The name of the Kafka cluster. When the console is connected to \
            Kubernetes, a Strimzi Kafka custom resource may be discovered using \
            this property together with the namespace property. In any case, \
            this property will be displayed in the console for the Kafka cluster's \
            name.""")
    private String name;

    @JsonPropertyDescription("""
            The namespace of the Kafka cluster. When the console is connected to \
            Kubernetes, a Strimzi Kafka custom resource may be discovered using \
            this property together with the name property.""")
    private String namespace;

    @JsonPropertyDescription("""
            The name of the listener in the Strimzi Kafka Kubernetes resource that \
            should be used by the console to establish connections.""")
    private String listener;

    private Credentials credentials;

    private ConfigVars properties = new ConfigVars();

    private ConfigVars adminProperties = new ConfigVars();

    private ConfigVars consumerProperties = new ConfigVars();

    private ConfigVars producerProperties = new ConfigVars();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public ConfigVars getProperties() {
        return properties;
    }

    public void setProperties(ConfigVars properties) {
        this.properties = properties;
    }

    public ConfigVars getAdminProperties() {
        return adminProperties;
    }

    public void setAdminProperties(ConfigVars adminProperties) {
        this.adminProperties = adminProperties;
    }

    public ConfigVars getConsumerProperties() {
        return consumerProperties;
    }

    public void setConsumerProperties(ConfigVars consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public ConfigVars getProducerProperties() {
        return producerProperties;
    }

    public void setProducerProperties(ConfigVars producerProperties) {
        this.producerProperties = producerProperties;
    }
}
