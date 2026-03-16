package com.github.streamshub.console.dependents.conditions;

import jakarta.inject.Inject;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.ConsoleIngress;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.ConsoleRoute;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerStatus;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatus;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteIngress;
import io.fabric8.openshift.api.model.RouteStatus;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.Optional;

public class IngressOrRouteReadyCondition implements Condition<Deployment, Console> {

    private static final Logger LOGGER = Logger.getLogger(IngressOrRouteReadyCondition.class);

    @Inject
    ConsoleIngress consoleIngress;

    @Inject
    ConsoleRoute consoleRoute;

    @Override
    public boolean isMet(DependentResource<Deployment, Console> dependentResource, Console primary, Context<Console> context) {
        if (context.getClient().supports(Route.class)) {
            return consoleRoute.getSecondaryResource(primary, context)
                .map(route -> isRouteReady(route, context))
                .orElse(false);
        } else {
            return consoleIngress.getSecondaryResource(primary, context)
                .map(this::isIngressReady)
                .orElse(false);
        }
    }

    private boolean isRouteReady(Route route, Context<Console> context) {
        String routeName = route.getMetadata().getName();

        Optional<RouteIngress> admittedIngress = Optional.ofNullable(route.getStatus())
            .map(RouteStatus::getIngress)
            .filter(list -> !list.isEmpty())
            .flatMap(ingresses -> ingresses.stream()
                .filter(this::isRouteAdmitted)
                .findFirst());

        boolean ready = admittedIngress.isPresent();

        if (ready) {
            // When the user did not specify hostname in the Console spec, openshift auto-assigns it
            // Set INGRESS_URL_KEY so ConsoleDeployment can use it for NEXTAUTH_URL
            admittedIngress
                .map(RouteIngress::getHost)
                .filter(host -> !host.isBlank())
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

    private boolean isRouteAdmitted(RouteIngress ingress) {
        return Optional.ofNullable(ingress.getConditions())
            .map(conditions -> conditions.stream()
                .anyMatch(condition ->
                    "Admitted".equals(condition.getType()) &&
                    "True".equals(condition.getStatus())
                )
            )
            .orElse(false);
    }

    private boolean isIngressReady(Ingress ingress) {
        Boolean ready = Optional.ofNullable(ingress.getStatus())
            .map(IngressStatus::getLoadBalancer)
            .map(IngressLoadBalancerStatus::getIngress)
            .map(Collection::isEmpty)
            .map(Boolean.FALSE::equals)
            .orElse(false);

        LOGGER.debugf("Ingress %s ready: %s", ingress.getMetadata().getName(), ready);
        return ready;
    }
}