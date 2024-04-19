package com.github.eyefloaters.console.api.v1alpha1;

public class KafkaCluster {

    String name;
    String namespace;
    String listener;
    String kafkaUserName;

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

    public String getKafkaUserName() {
        return kafkaUserName;
    }

    public void setKafkaUserName(String kafkaUserName) {
        this.kafkaUserName = kafkaUserName;
    }

}
