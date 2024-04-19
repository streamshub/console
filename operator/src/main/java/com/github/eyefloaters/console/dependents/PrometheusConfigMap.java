package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class PrometheusConfigMap extends CRUDKubernetesDependentResource<ConfigMap, Console> implements ConsoleResource {

    public PrometheusConfigMap() {
        super(ConfigMap.class);
    }

    @Override
    protected ConfigMap desired(Console primary, Context<Console> context) {
        return load(context, "prometheus.configmap.yaml", ConfigMap.class)
                .edit()
                .withNewMetadata()
                    .withName(name(primary))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .withLabels(MANAGEMENT_LABEL)
                .endMetadata()
                .build();
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-prometheus-config";
    }
}