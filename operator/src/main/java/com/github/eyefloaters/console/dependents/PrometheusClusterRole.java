package com.github.eyefloaters.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusClusterRole.class)
public class PrometheusClusterRole extends BaseClusterRole {

    public static final String NAME = "prometheus-clusterrole";

    public PrometheusClusterRole() {
        super("prometheus", "prometheus.clusterrole.yaml");
    }

    @Override
    public String resourceName() {
        return NAME;
    }

}
