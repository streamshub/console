package com.github.streamshub.console.dependents;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class DeploymentReadyCondition implements Condition<Deployment, Console> {

    private static final Logger LOGGER = Logger.getLogger(DeploymentReadyCondition.class);

    @Override
    public boolean isMet(DependentResource<Deployment, Console> dependentResource, Console primary, Context<Console> context) {
        return dependentResource.getSecondaryResource(primary, context).map(this::isReady).orElse(false);
    }

    private boolean isReady(Deployment deployment) {
        var readyReplicas = deployment.getStatus().getReadyReplicas();
        var ready = deployment.getSpec().getReplicas().equals(readyReplicas);
        LOGGER.debugf("Deployment %s ready: %s", deployment.getMetadata().getName(), ready);
        return ready;
    }
}
