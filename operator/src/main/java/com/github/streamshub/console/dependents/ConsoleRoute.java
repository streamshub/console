package com.github.streamshub.console.dependents;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteIngress;
import io.fabric8.openshift.api.model.RouteStatus;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleRoute extends CRUDKubernetesDependentResource<Route, Console>
        implements ConsoleResource<Route> {

    public static final String NAME = "console-route";

    @Inject
    ConsoleService service;

    public ConsoleRoute() {
        super(Route.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    public Optional<Route> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    protected Route desired(Console primary, Context<Console> context) {
        // hostname is optional on openshift — when null, the router auto-assigns one from the route
        // name, namespace, and cluster domain (issue #1471)
        String host = primary.getSpec().getHostname();
        String serviceName = service.instanceName(primary);

        return new RouteBuilder()
            .withNewMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("console"))
            .endMetadata()
            .withNewSpec()
                // null is valid — OpenShift assigns the host automatically
                .withHost(host)
                .withNewTo()
                    .withKind("Service")
                    .withName(serviceName)
                    .withWeight(100)
                .endTo()
                .withNewTls()
                    .withTermination("edge")
                    .withInsecureEdgeTerminationPolicy("Redirect")
                .endTls()
            .endSpec()
            .build();
    }

    /**
     * Used as BOTH {@code reconcilePrecondition} AND {@code activationCondition}
     * in the workflow so that:
     * <ul>
     *   <li>No Route is reconciled on plain Kubernetes clusters.</li>
     *   <li>No informer/watch for {@link Route} is registered on clusters that
     *       lack the Route API, avoiding API-discovery errors on startup.</li>
     * </ul>
     * Note: not a CDI bean — conditions are instantiated by the operator SDK.
     */
    public static class Precondition implements Condition<Route, Console> {
        @Override
        public boolean isMet(DependentResource<Route, Console> dependentResource, Console primary, Context<Console> context) {
            return context.getClient().supports(Route.class);
        }
    }

    public static class Postcondition implements Condition<Route, Console> {
        private static final Logger LOGGER = Logger.getLogger(Postcondition.class);

        @Override
        public boolean isMet(DependentResource<Route, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            boolean ready = dependentResource.getSecondaryResource(primary, context).map(this::isReady).orElse(false);
            context.managedWorkflowAndDependentResourceContext().put(NAME, ready);
            return ready;
        }

        private boolean isReady(Route route) {
            String routeName = route.getMetadata().getName();

            Optional<RouteIngress> admittedIngress = Optional.ofNullable(route.getStatus())
                .map(RouteStatus::getIngress)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(this::isAdmitted)
                .findFirst();

            boolean ready = admittedIngress.isPresent();

            if (ready) {
                // When the user did not specify hostname in the Console spec, OpenShift auto-assigns it
                admittedIngress
                    .map(RouteIngress::getHost)
                    .filter(Predicate.not(String::isBlank))
                    .ifPresent(host -> LOGGER.debugf("Route %s ready: true, auto-assigned host: %s", routeName, host));
            } else {
                LOGGER.debugf("Route %s ready: false", routeName);
            }

            return ready;
        }

        private boolean isAdmitted(RouteIngress ingress) {
            return Optional.ofNullable(ingress.getConditions())
                .orElseGet(Collections::emptyList)
                .stream()
                .anyMatch(condition ->
                    "Admitted".equals(condition.getType()) &&
                    "True".equals(condition.getStatus())
                );
        }
    }
}
