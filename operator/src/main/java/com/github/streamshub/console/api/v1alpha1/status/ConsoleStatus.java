package com.github.streamshub.console.api.v1alpha1.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import io.sundr.builder.annotations.Buildable;

@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsoleStatus extends ObservedGenerationAwareStatus {

    private List<Condition> conditions = new ArrayList<>();

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    @JsonIgnore
    public Condition getCondition(String type) {
        return conditions.stream()
            .filter(c -> type.equals(c.getType()))
            .findFirst()
            .orElseGet(() -> {
                var condition = new ConditionBuilder()
                        .withType(type)
                        .withLastTransitionTime(Instant.now().toString())
                        .build();
                conditions.add(condition);
                return condition;
            });
    }

    @JsonIgnore
    public void clearCondition(String type) {
        conditions.removeIf(c -> type.equals(c.getType()));
    }
}
