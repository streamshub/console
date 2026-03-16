package com.github.streamshub.console.dependents.conditions;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.ConsoleResource;
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
                .map(route -> isReady(route, context))
                .orElse(false);
    }

    private boolean isReady(Route route, Context<Console> context) {
        String routeName = route.getMetadata().getName();

        Optional<RouteIngress> admittedIngress = Optional.ofNullable(route.getStatus())
                .map(s -> s.getIngress())
                .filter(list -> !list.isEmpty())
                .flatMap(ingresses -> ingresses.stream()
                        .filter(this::isAdmitted)
                        .findFirst());

        boolean ready = admittedIngress.isPresent();

        if (ready) {
            // When the user did not specify hostname in the Console spec, openshift auto-assigns it
            // Set INGRESS_URL_KEY so ConsoleDeployment can use it for NEXTAUTH_URL
            admittedIngress
                    .map(RouteIngress::getHost)
                    .filter(h -> h != null && !h.isBlank())
                    .ifPresent(host -> {
                        Optional<String> existing = context.managedWorkflowAndDependentResourceContext()
                                .get(ConsoleResource.INGRESS_URL_KEY, String.class);
                        if (existing.isEmpty()) {
                            LOGGER.debugf("Route %s: auto-assigned host %s", routeName, host);
                            context.managedWorkflowAndDependentResourceContext()
                                    .put(ConsoleResource.INGRESS_URL_KEY, "https://" + host);
                        }
                    });
        }

        LOGGER.debugf("Route %s ready: %s", routeName, ready);
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