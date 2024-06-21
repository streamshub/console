package com.github.streamshub.console.config;

public class ConsoleConfig {

    KafkaConfig kafka = new KafkaConfig();

    public KafkaConfig getKafka() {
        return kafka;
    }

    public void setKafka(KafkaConfig kafka) {
        this.kafka = kafka;
    }
}
