package com.github.streamshub.console.dependents.conditions;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import static com.github.streamshub.console.dependents.support.ConfigSupport.resourceSupported;

/**
 * Condition that is met only on OpenShift clusters (where the Route API is
 * available). Used as both {@code activationCondition} and
 * {@code reconcilePrecondition} for resources that rely on OpenShift-specific
 * controllers (service CA, route admission, etc.).
 */
public class OpenShiftPrecondition<T extends HasMetadata> implements Condition<T, Console> {

    @Override
    public boolean isMet(DependentResource<T, Console> dependentResource,
            Console primary,
            Context<Console> context) {

        return resourceSupported(context, Route.class);
    }

}
