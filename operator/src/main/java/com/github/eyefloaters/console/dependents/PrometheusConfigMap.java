package com.github.eyefloaters.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class PrometheusConfigMap extends CRUDKubernetesDependentResource<ConfigMap, Console> implements ConsoleResource {

    public static final String NAME = "prometheus-configmap";

    public PrometheusConfigMap() {
        super(ConfigMap.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected ConfigMap desired(Console primary, Context<Console> context) {
        ConfigMap template = load(context, "prometheus.configmap.yaml", ConfigMap.class);
        updateDigest(context, "prometheus-digest", template.getData());

        return template.edit()
                .withNewMetadata()
                    .withName(instanceName(primary))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .withLabels(commonLabels("prometheus"))
                .endMetadata()
                .build();
    }

}
