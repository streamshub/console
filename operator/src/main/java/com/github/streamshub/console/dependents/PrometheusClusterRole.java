package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
    informer = @Informer(
        namespaces = Constants.WATCH_ALL_NAMESPACES,
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class PrometheusClusterRole extends BaseClusterRole {

    public static final String NAME = "prometheus-clusterrole";

    public PrometheusClusterRole() {
        super("prometheus", "prometheus.clusterrole.yaml", NAME);
    }

}
