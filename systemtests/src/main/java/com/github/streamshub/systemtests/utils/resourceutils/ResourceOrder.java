package com.github.streamshub.systemtests.utils.resourceutils;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

import java.util.Comparator;
import java.util.List;

public class ResourceOrder {

    private ResourceOrder() {}

    // Define the canonical install order
    private static final List<Class<? extends HasMetadata>> ORDER = List.of(
        CustomResourceDefinition.class,
        ServiceAccount.class,
        ClusterRole.class,
        ClusterRoleBinding.class,
        Role.class,
        RoleBinding.class,
        ConfigMap.class,
        Secret.class,
        Service.class,
        Deployment.class,
        DaemonSet.class,
        StatefulSet.class,
        ServiceMonitor.class
    );

    public static List<HasMetadata> sort(List<HasMetadata> resources) {
        return resources.stream()
            .sorted(Comparator.comparingInt(r -> {
                int idx = ORDER.indexOf(r.getClass());
                // unknown types go last
                return idx == -1 ? ORDER.size() : idx;
            }))
            .toList();
    }
}