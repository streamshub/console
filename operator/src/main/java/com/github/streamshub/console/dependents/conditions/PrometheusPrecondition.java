package com.github.streamshub.console.dependents.conditions;

import java.util.Collections;
import java.util.Optional;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PrometheusPrecondition implements Condition<Object, Console> {

    @Override
    public boolean isMet(DependentResource<Object, Console> dependentResource, Console primary, Context<Console> context) {
        var metricsSources = Optional.ofNullable(primary.getSpec().getMetricsSources())
                .orElseGet(Collections::emptyList);

        if (metricsSources.isEmpty()) {
            return true;
        }

        return metricsSources.stream()
                .anyMatch(prometheus -> prometheus.getType() == MetricsSource.Type.EMBEDDED);
    }

}
