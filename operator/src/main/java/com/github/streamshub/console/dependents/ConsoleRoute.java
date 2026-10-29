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
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleRoute extends BaseRoute {

    public static final String NAME = "console-route";

    @Inject
    ConsoleService service;

    public ConsoleRoute() {
        super(NAME);
    }

    @Override
    protected Route desired(Console primary, Context<Console> context) {
        // hostname is optional on openshift — when null, the router auto-assigns one from the route
        // name, namespace, and cluster domain (issue #1471)
        String host = primary.getSpec().getHostname();

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
                    .withName(service.instanceName(primary))
                    .withWeight(100)
                .endTo()
                .withTls(buildTlsConfig(primary, context))
            .endSpec()
            .build();
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
