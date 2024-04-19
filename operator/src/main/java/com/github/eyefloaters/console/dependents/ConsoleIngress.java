package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class ConsoleIngress extends CRUDKubernetesDependentResource<Ingress, Console> implements ConsoleResource {

    public ConsoleIngress() {
        super(Ingress.class);
    }

    @Override
    protected Ingress desired(Console primary, Context<Console> context) {
        return load(context, "console.ingress.yaml", Ingress.class)
            .edit()
            .editMetadata()
                .withName(name(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(MANAGEMENT_LABEL)
            .endMetadata()
            .editSpec()
                .editDefaultBackend()
                    .editService()
                        .withName(ConsoleService.name(primary))
                    .endService()
                .endDefaultBackend()
                .editFirstRule()
                    .withHost(host(primary))
                    .editHttp()
                        .editFirstPath()
                            .editBackend()
                                .editService()
                                    .withName(ConsoleService.name(primary))
                                .endService()
                            .endBackend()
                        .endPath()
                    .endHttp()
                .endRule()
            .endSpec()
            .build();
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console-ingress";
    }

    public static String host(Console primary) {
        return primary.getSpec().getHostname();
    }
}