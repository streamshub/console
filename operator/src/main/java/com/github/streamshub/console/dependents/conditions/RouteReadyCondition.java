package com.github.streamshub.console.dependents.conditions;

import com.github.streamshub.console.api.v1alpha1.Console;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteIngress;
import io.fabric8.openshift.api.model.RouteIngressCondition;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.jboss.logging.Logger;

import java.util.Optional;

public class RouteReadyCondition implements Condition<Route, Console> {

    private static final Logger LOGGER = Logger.getLogger(RouteReadyCondition.class);

    @Override
    public boolean isMet(DependentResource<Route, Console> dependentResource,
            Console primary,
            Context<Console> context) {
        return dependentResource.getSecondaryResource(primary, context)
                .map(this::isReady)
                .orElse(false);
    }

    private boolean isReady(Route route) {
        // A Route is ready when at least one ingress entry carries an
        // Admitted=True condition — this is how the OpenShift router signals
        // that it has picked up the route (works on both full OCP and MicroShift).
        boolean ready = Optional.ofNullable(route.getStatus())
                .map(s -> s.getIngress())
                .filter(list -> !list.isEmpty())
                .map(ingresses -> ingresses.stream().anyMatch(this::isAdmitted))
                .orElse(false);

        LOGGER.debugf("Route %s ready: %s", route.getMetadata().getName(), ready);
        return ready;
    }

    private boolean isAdmitted(RouteIngress ingress) {
        return Optional.ofNullable(ingress.getConditions())
                .map(conditions -> conditions.stream().anyMatch(this::admittedTrue))
                .orElse(false);
    }

    private boolean admittedTrue(RouteIngressCondition condition) {
        return "Admitted".equals(condition.getType()) && "True".equals(condition.getStatus());
    }
}