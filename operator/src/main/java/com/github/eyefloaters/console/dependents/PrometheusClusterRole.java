package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusClusterRole.class)
public class PrometheusClusterRole extends BaseClusterRole {

    public PrometheusClusterRole() {
        super("prometheus", "prometheus.clusterrole.yaml", PrometheusClusterRole::name);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-prometheus-clusterrole";
    }

}