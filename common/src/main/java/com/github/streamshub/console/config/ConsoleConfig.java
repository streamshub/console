package com.github.streamshub.console.config;

public class ConsoleConfig {

    KubernetesConfig kubernetes = new KubernetesConfig();
    KafkaConfig kafka = new KafkaConfig();

    public KubernetesConfig getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesConfig kubernetes) {
        this.kubernetes = kubernetes;
    }

    public KafkaConfig getKafka() {
        return kafka;
    }

    public void setKafka(KafkaConfig kafka) {
        this.kafka = kafka;
    }
}
