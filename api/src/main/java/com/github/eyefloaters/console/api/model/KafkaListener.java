package com.github.eyefloaters.console.api.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;

public record KafkaListener(
        @Schema(implementation = KafkaListenerType.class)
        String type,
        String bootstrapServers,
        String authType) {
}
