package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleIngress extends CRUDKubernetesDependentResource<Ingress, Console>
        implements ConsoleResource<Ingress> {

    public static final String NAME = "console-ingress";

    @Inject
    ConsoleService service;

    public ConsoleIngress() {
        super(Ingress.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected Ingress desired(Console primary, Context<Console> context) {
        String host = primary.getSpec().getHostname();
        setAttribute(context, INGRESS_URL_KEY, "https://" + host);

        return load(context, "console.ingress.yaml", Ingress.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("console"))
            .endMetadata()
            .editSpec()
                // Plain Kubernetes (non-OCP) clusters don't need a class name; the
                // default ingress controller picks it up automatically.
                .withIngressClassName(null)
                .editDefaultBackend()
                    .editService()
                        .withName(service.instanceName(primary))
                    .endService()
                .endDefaultBackend()
                .editFirstRule()
                    .withHost(host)
                    .editHttp()
                        .editFirstPath()
                            .editBackend()
                                .editService()
                                    .withName(service.instanceName(primary))
                                .endService()
                            .endBackend()
                        .endPath()
                    .endHttp()
                .endRule()
            .endSpec()
            .build();
    }

    /**
     * Only create the plain Ingress on clusters that do NOT support OpenShift
     * Routes. On OpenShift / MicroShift, {@link ConsoleRoute} is used instead.
     * <p>
     * Note: not a CDI bean — conditions are instantiated by the operator SDK.
     */
    public static class Precondition implements Condition<Ingress, Console> {
        @Override
        public boolean isMet(DependentResource<Ingress, Console> dependentResource, Console primary, Context<Console> context) {
            return !context.getClient().supports(Route.class);
        }
    }
}