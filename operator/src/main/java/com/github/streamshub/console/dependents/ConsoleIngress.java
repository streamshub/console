package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class ConsoleIngress extends CRUDKubernetesDependentResource<Ingress, Console> implements ConsoleResource {

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
        setAttribute(context, NAME + ".url", "https://" + host);

        return load(context, "console.ingress.yaml", Ingress.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("console"))
            .endMetadata()
            .editSpec()
                .withIngressClassName(getIngressClassName(context))
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
     * The class name is not required for functionality on OCP. However, monitoring
     * will issue an alert if it is not present.
     */
    private String getIngressClassName(Context<Console> context) {
        return context.getClient().supports(Route.class) ? "openshift-default" : null;
    }
}
