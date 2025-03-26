package com.github.streamshub.systemtests.utils.resources;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.skodjob.testframe.resources.KubeResourceManager;

public class ClusterRoleBindingUtils {
    private ClusterRoleBindingUtils() {}

    public static ClusterRoleBinding getCrb(String name) {
        return KubeResourceManager.getKubeClient().getClient().rbac().clusterRoleBindings().withName(name).get();
    }
}
