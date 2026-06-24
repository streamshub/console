package com.github.streamshub.console.dependents;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteIngress;
import io.fabric8.openshift.api.model.RouteStatus;
import io.fabric8.openshift.api.model.TLSConfig;
import io.fabric8.openshift.api.model.TLSConfigBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import static com.github.streamshub.console.dependents.support.ConfigSupport.resourceSupported;

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
        boolean tls = primary.getSpec().getTls() != null;

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
                .withTls(buildTls(tls))
            .endSpec()
            .build();
    }

    private static TLSConfig buildTls(boolean tls) {
        TLSConfigBuilder tlsBuilder = new TLSConfigBuilder();

        if (tls) {
            // The console terminates TLS itself; the router passes through the connection
            tlsBuilder = tlsBuilder.withTermination("passthrough");
        } else {
            // The router terminates TLS at the edge and forwards plain HTTP to the console
            tlsBuilder = tlsBuilder
                .withTermination("edge")
                .withInsecureEdgeTerminationPolicy("Redirect");
        }

        return tlsBuilder.build();
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
            return resourceSupported(context, Route.class);
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
            List<RouteIngress> admittedIngresses = Optional.ofNullable(route.getStatus())
                .map(RouteStatus::getIngress)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(this::isAdmitted)
                .toList();

            if (admittedIngresses.isEmpty()) {
                LOGGER.debugf("Route %s ready: false", route.getMetadata().getName());
                return false;
            }

            // When the user did not specify hostname in the Console spec, OpenShift auto-assigns it
            var hosts = admittedIngresses.stream().map(RouteIngress::getHost).toList();
            LOGGER.debugf("Route %s ready: true, hosts: %s", route.getMetadata().getName(), hosts);
            return true;
        }

        private boolean isAdmitted(RouteIngress ingress) {
            return Optional.ofNullable(ingress.getConditions())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(condition -> "Admitted".equals(condition.getType()))
                .anyMatch(condition -> "True".equals(condition.getStatus()));
        }
    }
}
