package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.systemtests.utils.YamlUtils;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RoleBindingUtils {

    private static final Logger LOGGER = LogManager.getLogger(RoleBindingUtils.class);

    // -------------
    // Client
    // -------------
    private static MixedOperation<RoleBinding, RoleBindingList, Resource<RoleBinding>> roleBindingClient() {
        return KubeResourceManager.getKubeClient().getClient().rbac().roleBindings();
    }

    // -------------
    // Get
    // -------------
    public static RoleBinding get(String name) {
        return roleBindingClient().withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<RoleBinding> listRoleBindings() {
        return roleBindingClient().list().getItems();
    }

    public static List<RoleBinding> listRoleBindingsWithContains(String containName) {
        List<RoleBinding> rb = listRoleBindings();
        if (rb != null && !rb.isEmpty()) {
            return rb.stream().filter(r -> r.getMetadata().getName().contains(containName)).toList();
        }
        return new ArrayList<>();
    }

    // -------------
    // Delete
    // -------------
    public static void delete(String name) {
        LOGGER.debug("Deleting role binding {}", name);
        roleBindingClient().withName(name).delete();
    }

    public static void deleteRoleBindingByContains(String name) {
        for (RoleBinding rb : listRoleBindingsWithContains(name)) {
            delete(rb.getMetadata().getName());
        }
    }

    // -------------
    // ConfigFromYaml
    // -------------
    public static RoleBinding getRoleBindingFromYaml(String namespace, String clientNamespace, String yamlPath) {
        return new RoleBindingBuilder(YamlUtils.configFromYaml(yamlPath, RoleBinding.class))
            .editMetadata()
                .withNamespace(clientNamespace)
            .endMetadata()
            .editFirstSubject()
                .withNamespace(namespace)
            .endSubject()
            .build();
    }
}
