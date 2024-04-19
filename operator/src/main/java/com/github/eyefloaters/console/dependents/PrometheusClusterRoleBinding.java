package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusClusterRoleBinding.class)
public class PrometheusClusterRoleBinding extends BaseClusterRoleBinding {

    public PrometheusClusterRoleBinding() {
        super("prometheus", "prometheus.clusterrolebinding.yaml", PrometheusClusterRoleBinding::name);
    }

    @Override
    protected String roleName(Console primary) {
        return PrometheusClusterRole.name(primary);
    }

    @Override
    protected String subjectName(Console primary) {
        return PrometheusServiceAccount.name(primary);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-prometheus-clusterrolebinding";
    }
}
