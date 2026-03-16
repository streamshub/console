package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.api.model.TLSConfigBuilder;
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
        String host = primary.getSpec().getHostname();
        String serviceName = service.instanceName(primary);

        // Store the URL so ConsoleDeployment can use it for NEXTAUTH_URL
        setAttribute(context, INGRESS_URL_KEY, "https://" + host);

        return new RouteBuilder()
                .withNewMetadata()
                    .withName(instanceName(primary))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .withLabels(commonLabels("console"))
                .endMetadata()
                .withNewSpec()
                    .withHost(host)
                    .withTo(new RouteTargetReferenceBuilder()
                            .withKind("Service")
                            .withName(serviceName)
                            .withWeight(100)
                            .build())
                    .withNewPort()
                        .withNewTargetPort("https")
                    .endPort()
                    .withTls(new TLSConfigBuilder()
                            .withTermination("edge")
                            .withInsecureEdgeTerminationPolicy("Redirect")
                            .build())
                .endSpec()
                .build();
    }

    /**
     * Reconcile precondition: only create the Route when the cluster supports
     * OpenShift Route resources (i.e. OpenShift / MicroShift).
     */
    @ApplicationScoped
    public static class Precondition implements Condition<Route, Console> {
        @Override
        public boolean isMet(DependentResource<Route, Console> dependentResource,
                Console primary,
                Context<Console> context) {
            return context.getClient().supports(Route.class);
        }
    }
}