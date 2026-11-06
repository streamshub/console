package com.github.streamshub.console.dependents.conditions;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class DexPrecondition<T extends HasMetadata> implements Condition<T, Console> {

    @Override
    public boolean isMet(DependentResource<T, Console> dependentResource, Console primary, Context<Console> context) {
        return ConfigSupport.dexIntegrationEnabled(primary);
    }

}
