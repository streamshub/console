package com.github.streamshub.console.api.v1alpha1.status;

import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsoleStatus {

    private long observedGeneration;

    private final Set<Condition> conditions = new TreeSet<>(Comparator
            .comparing(Condition::getType).reversed()
            .thenComparing(Condition::getLastTransitionTime)
            .thenComparing(Condition::getStatus, Comparator.nullsLast(String::compareTo))
            .thenComparing(Condition::getReason, Comparator.nullsLast(String::compareTo))
            .thenComparing(Condition::getMessage, Comparator.nullsLast(String::compareTo)));

    public long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(long observedGeneration) {
        this.observedGeneration = observedGeneration;
    }

    public Set<Condition> getConditions() {
        return conditions;
    }

    /*
     * Setter used by Jackson. Without this the field is replaced with an unsorted
     * HashSet by Jackson using reflection.
     */
    public void setConditions(Set<Condition> conditions) {
        this.conditions.clear();
        this.conditions.addAll(conditions);
    }

    @JsonIgnore
    public boolean hasCondition(String type) {
        return conditions.stream().anyMatch(c -> type.equals(c.getType()));
    }

    @JsonIgnore
    public boolean hasActiveCondition(String type) {
        return conditions.stream().filter(Condition::isActive).anyMatch(c -> type.equals(c.getType()));
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
        condition.setActive(true);

        conditions.stream()
            .filter(condition::equals)
            .findFirst()
            .ifPresentOrElse(
                    c -> c.setActive(true),
                    () -> conditions.add(condition));
    }

    @JsonIgnore
    public void clearStaleConditions() {
        conditions.removeIf(Predicate.not(Condition::isActive));
    }
}
