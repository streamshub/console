package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.systemtests.utils.YamlUtils;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ClusterRoleBindingUtils {
    private static final Logger LOGGER = LogManager.getLogger(ClusterRoleBindingUtils.class);

    // -------------
    // Client
    // -------------
    private static NonNamespaceOperation<ClusterRoleBinding, ClusterRoleBindingList, Resource<ClusterRoleBinding>> clusterRoleBindingClient() {
        return KubeResourceManager.getKubeClient().getClient().rbac().clusterRoleBindings();
    }

    // -------------
    // Get
    // -------------
    public static ClusterRoleBinding get(String name) {
        return clusterRoleBindingClient().withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<ClusterRoleBinding> listClusterRoleBinding() {
        return clusterRoleBindingClient().list().getItems();
    }
    public static List<ClusterRoleBinding> listClusterRoleBindingByContains(String containsName) {
        return listClusterRoleBinding().stream().filter(cr -> cr.getMetadata().getName().contains(containsName)).toList();
    }

    // -------------
    // Delete
    // -------------
    public static void deleteClusterRoleBinding(String name) {
        clusterRoleBindingClient().withName(name).delete();
    }

    public static void deleteClusterRoleBindingByContains(String containsName) {
        for (ClusterRoleBinding cr : listClusterRoleBindingByContains(containsName)) {
            deleteClusterRoleBinding(cr.getMetadata().getName());
        }
    }

    public static ClusterRoleBinding getClusterRoleBindingFromYaml(String namespace, String yamlPath) {
        LOGGER.info("Creating ClusterRoleBinding from {} in Namespace: {}", yamlPath, namespace);
        return new ClusterRoleBindingBuilder(YamlUtils.configFromYaml(yamlPath, ClusterRoleBinding.class))
            .editMetadata()
                .withNamespace(namespace)
            .endMetadata()
            .editFirstSubject()
                .withNamespace(namespace)
            .endSubject().build();
    }
}
