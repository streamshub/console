package com.github.streamshub.systemtests.utils.resources.kubernetes;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.List;

public class ClusterRoleUtils {

    // -------------
    // Client
    // -------------
    private static NonNamespaceOperation<ClusterRole, ClusterRoleList, Resource<ClusterRole>> clusterRoleClient() {
        return KubeResourceManager.getKubeClient().getClient().rbac().clusterRoles();
    }

    // -------------
    // Get
    // -------------
    public static ClusterRole get(String name) {
        return clusterRoleClient().withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<ClusterRole> listClusterRoles() {
        return clusterRoleClient().list().getItems();
    }
    public static List<ClusterRole> listClusterRolesByContains(String containsName) {
        return listClusterRoles().stream().filter(cr -> cr.getMetadata().getName().contains(containsName)).toList();
    }

    // -------------
    // Delete
    // -------------
    public static void deleteClusterRole(String name) {
        clusterRoleClient().withName(name).delete();
    }
    public static void deleteClusterRoleByContains(String containsName) {
        for (ClusterRole cr : listClusterRolesByContains(containsName)) {
            deleteClusterRole(cr.getMetadata().getName());
        }
    }
}
