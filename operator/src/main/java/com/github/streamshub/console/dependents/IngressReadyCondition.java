package com.github.streamshub.console.dependents;

import java.util.Collection;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerStatus;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatus;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class IngressReadyCondition implements Condition<Ingress, Console> {

    private static final Logger LOGGER = Logger.getLogger(IngressReadyCondition.class);

    @Override
    public boolean isMet(DependentResource<Ingress, Console> dependentResource, Console primary, Context<Console> context) {
        return dependentResource.getSecondaryResource(primary, context).map(this::isReady).orElse(false);
    }

    private boolean isReady(Ingress ingress) {
        var ready = Optional.ofNullable(ingress.getStatus())
            .map(IngressStatus::getLoadBalancer)
            .map(IngressLoadBalancerStatus::getIngress)
            .map(Collection::isEmpty)
            .map(Boolean.FALSE::equals)
            .orElse(false);

        LOGGER.debugf("Ingress %s ready: %s", ingress.getMetadata().getName(), ready);
        return ready;
    }
}
