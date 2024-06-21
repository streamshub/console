package com.github.streamshub.console.api.v1alpha1;

import io.fabric8.generator.annotation.Required;

public class KafkaCluster {

    @Required
    String namespace;

    @Required
    String name;

    String listener;

    String kafkaUserName;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getListener() {
        return listener;
    }

    public void setListener(String listener) {
        this.listener = listener;
    }

    public String getKafkaUserName() {
        return kafkaUserName;
    }

    public void setKafkaUserName(String kafkaUserName) {
        this.kafkaUserName = kafkaUserName;
    }

}
