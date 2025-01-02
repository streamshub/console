package com.github.streamshub.console.dependents.conditions;

import java.util.Objects;

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

    /**
     * Check the deployment's status in a similar way to kubectl.
     *
     * @see https://github.com/kubernetes/kubectl/blob/24d21a0ee42ecb5e5bed731f36b2d2c9c0244c35/pkg/polymorphichelpers/rollout_status.go#L76-L89
     */
    private boolean isReady(Deployment deployment) {
        String deploymentName = deployment.getMetadata().getName();
        var status = deployment.getStatus();
        var deploymentTimedOut = status.getConditions().stream()
            .filter(c -> "Progressing".equals(c.getType()))
            .findFirst()
            .map(c -> "ProgressDeadlineExceeded".equals(c.getReason()))
            .orElse(false)
            .booleanValue();

        if (deploymentTimedOut) {
            LOGGER.warnf("Deployment %s has timed out", deployment.getMetadata().getName());
            return false;
        }

        var desiredReplicas = deployment.getSpec().getReplicas();
        var replicas = Objects.requireNonNullElse(status.getReplicas(), 0);
        var updatedReplicas = Objects.requireNonNullElse(status.getUpdatedReplicas(), 0);
        var availableReplicas = Objects.requireNonNullElse(status.getAvailableReplicas(), 0);

        if (desiredReplicas != null && updatedReplicas < desiredReplicas) {
            LOGGER.debugf("Waiting for deployment %s rollout to finish: %d out of %d new replicas have been updated...", deploymentName, updatedReplicas, desiredReplicas);
            return false;
        }

        if (replicas > updatedReplicas) {
            LOGGER.debugf("Waiting for deployment %s rollout to finish: %d old replicas are pending termination...", deploymentName, replicas - updatedReplicas);
            return false;
        }

        if (availableReplicas < updatedReplicas) {
            LOGGER.debugf("Waiting for deployment %s rollout to finish: %d of %d updated replicas are available...", deploymentName, availableReplicas, updatedReplicas);
            return false;
        }

        LOGGER.debugf("Deployment %s ready", deployment.getMetadata().getName());
        return true;
    }
}
