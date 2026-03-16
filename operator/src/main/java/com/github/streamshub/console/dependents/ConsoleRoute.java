package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.github.streamshub.console.api.v1alpha1.Console;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import java.util.Optional;

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

        // Only set INGRESS_URL_KEY now, if we already know the hostname
        // when missing, RouteReadyCondition sets it once the router has set
        // route.status.ingress[0].host so that ConsoleDeployment gets the right NEXTAUTH_URL
        if (host != null) {
            setAttribute(context, INGRESS_URL_KEY, "https://" + host);
        }

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
                .withNewPort()
                    // 80 is the HTTP port on the console-ui service
                    .withNewTargetPort(80)
                .endPort()
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
}