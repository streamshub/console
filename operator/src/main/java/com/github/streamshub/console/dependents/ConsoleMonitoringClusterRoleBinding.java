package com.github.streamshub.console.dependents;

import java.util.Collections;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource.Type;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@ApplicationScoped
@KubernetesDependent(
        namespaces = Constants.WATCH_ALL_NAMESPACES,
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class ConsoleMonitoringClusterRoleBinding extends BaseClusterRoleBinding {

    public static final String NAME = "console-monitoring-clusterrolebinding";

    @Inject
    ConsoleClusterRole clusterRole;

    @Inject
    ConsoleServiceAccount serviceAccount;

    public ConsoleMonitoringClusterRoleBinding() {
        super("console", "console-monitoring.clusterrolebinding.yaml", NAME);
    }

    @Override
    protected String roleName(Console primary) {
        // Hard-coded, pre-existing cluster role available in OCP
        return "cluster-monitoring-view";
    }

    @Override
    protected String subjectName(Console primary) {
        return serviceAccount.instanceName(primary);
    }

    /**
     * The cluster role binding to `cluster-monitoring-view` will only be created
     * if one of the metrics sources is OpenShift Monitoring.
     */
    public static class Precondition implements Condition<ClusterRoleBinding, Console> {
        @Override
        public boolean isMet(DependentResource<ClusterRoleBinding, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            var metricsSources = Optional.ofNullable(primary.getSpec().getMetricsSources())
                    .orElseGet(Collections::emptyList);

            if (metricsSources.isEmpty()) {
                return false;
            }

            return metricsSources.stream()
                .anyMatch(prometheus -> prometheus.getType() == Type.OPENSHIFT_MONITORING);
        }
    }
}
