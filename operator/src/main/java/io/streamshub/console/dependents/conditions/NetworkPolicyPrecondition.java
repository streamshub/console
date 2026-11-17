package io.streamshub.console.dependents.conditions;

import io.streamshub.console.api.v1alpha1.Console;
import io.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class NetworkPolicyPrecondition implements Condition<NetworkPolicy, Console> {

    @Override
    public boolean isMet(DependentResource<NetworkPolicy, Console> dependentResource, Console primary, Context<Console> context) {
        return ConfigSupport.networkPolicyEnabled(primary);
    }
}
