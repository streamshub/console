package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
    informer = @Informer(
        namespaces = Constants.WATCH_ALL_NAMESPACES,
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class PrometheusClusterRoleBinding extends BaseClusterRoleBinding {

    public static final String NAME = "prometheus-clusterrolebinding";

    @Inject
    PrometheusClusterRole clusterRole;

    @Inject
    PrometheusServiceAccount serviceAccount;

    public PrometheusClusterRoleBinding() {
        super("prometheus", "prometheus.clusterrolebinding.yaml", NAME);
    }

    @Override
    protected String roleName(Console primary) {
        return clusterRole.instanceName(primary);
    }

    @Override
    protected String subjectName(Console primary) {
        return serviceAccount.instanceName(primary);
    }
}
