package com.github.eyefloaters.console.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public record Condition(
        String status,
        String reason,
        String message,
        String type,
        String lastTransitionTime) {

    public Condition(io.strimzi.api.kafka.model.status.Condition condition) {
        this(condition.getStatus(),
            condition.getReason(),
            condition.getMessage(),
            condition.getType(),
            condition.getLastTransitionTime());
    }

}
