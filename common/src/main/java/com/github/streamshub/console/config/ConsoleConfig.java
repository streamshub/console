package com.github.streamshub.console.config;

import com.github.streamshub.console.config.security.GlobalSecurityConfig;

public class ConsoleConfig {

    KubernetesConfig kubernetes = new KubernetesConfig();
    GlobalSecurityConfig security = new GlobalSecurityConfig();
    KafkaConfig kafka = new KafkaConfig();

    public KubernetesConfig getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesConfig kubernetes) {
        this.kubernetes = kubernetes;
    }

    public GlobalSecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(GlobalSecurityConfig security) {
        this.security = security;
    }

    public KafkaConfig getKafka() {
        return kafka;
    }

    public void setKafka(KafkaConfig kafka) {
        this.kafka = kafka;
    }
}
