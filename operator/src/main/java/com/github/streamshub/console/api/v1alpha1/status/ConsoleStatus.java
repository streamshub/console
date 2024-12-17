package com.github.streamshub.console.api.v1alpha1.status;

import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Types;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import io.sundr.builder.annotations.Buildable;

@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsoleStatus extends ObservedGenerationAwareStatus {

    private final Set<Condition> conditions = new TreeSet<>(Comparator
            .comparing(Condition::getType).reversed()
            .thenComparing(Condition::getLastTransitionTime)
            .thenComparing(Condition::getStatus, Comparator.nullsLast(String::compareTo))
            .thenComparing(Condition::getReason, Comparator.nullsLast(String::compareTo))
            .thenComparing(Condition::getMessage, Comparator.nullsLast(String::compareTo)));

    public Set<Condition> getConditions() {
        return conditions;
    }

    @JsonIgnore
    public boolean hasCondition(String type) {
        return conditions.stream().anyMatch(c -> type.equals(c.getType()));
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
    public void updateCondition(Condition condition) {
        condition.setLastUpdatedTime(Instant.now());

        conditions.stream()
            .filter(condition::equals)
            .findFirst()
            .ifPresentOrElse(
                    c -> c.setLastUpdatedTime(condition.getLastUpdatedTime()),
                    () -> conditions.add(condition));
    }

    @JsonIgnore
    public void clearConditions(String type) {
        conditions.removeIf(c -> type.equals(c.getType()));
    }

    @JsonIgnore
    public void clearErrorsBefore(Instant minLastUpdated) {
        conditions.removeIf(c -> Types.ERROR.equals(c.getType())
                && minLastUpdated.isAfter(c.getLastUpdatedTime()));
    }
}
