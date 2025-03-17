package com.github.streamshub.systemtests.templates;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class ClusterRoleTemplates {

    private static final Logger LOGGER = LogManager.getLogger(ClusterRoleTemplates.class);

    public static List<ClusterRoleBinding> clusterRoleBindingsForAllNamespaces(String namespace) {
        return clusterRoleBindingsForAllNamespaces(namespace, "strimzi-cluster-operator");
    }

    public static List<ClusterRoleBinding> clusterRoleBindingsForAllNamespaces(String namespace, String coName) {
        LOGGER.debug("Return list of CRB for watching all namespaces");
        final List<ClusterRoleBinding> kCRBList = Arrays.asList(
            getClusterOperatorNamespacedCrb(coName, namespace),
            getClusterOperatorEntityOperatorCrb(coName, namespace),
            getClusterOperatorWatchedCrb(coName, namespace)
        );

        return kCRBList;
    }

    public static ClusterRoleBinding getClusterOperatorNamespacedCrb(final String coName, final String namespaceName) {
        return new ClusterRoleBindingBuilder()
            .withNewMetadata()
                .withName(coName + "-namespaced")
            .endMetadata()
            .withNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(coName + "-namespaced")
            .endRoleRef()
            .withSubjects(new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(coName)
                .withNamespace(namespaceName)
                .build())
            .build();
    }

    public static ClusterRoleBinding getClusterOperatorEntityOperatorCrb(final String coName, final String namespaceName) {
        return new ClusterRoleBindingBuilder()
            .withNewMetadata()
                .withName(coName + "-entity-operator")
            .endMetadata()
            .withNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName("strimzi-entity-operator")
            .endRoleRef()
            .withSubjects(new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(coName)
                .withNamespace(namespaceName)
                .build()
            )
            .build();
    }

    public static ClusterRoleBinding getClusterOperatorWatchedCrb(final String coName, final String namespaceName) {
        return new ClusterRoleBindingBuilder()
                .withNewMetadata()
                    .withName(coName + "-watched")
                .endMetadata()
                .withNewRoleRef()
                    .withApiGroup("rbac.authorization.k8s.io")
                    .withKind("ClusterRole")
                    .withName(coName + "-watched")
                .endRoleRef()
                .withSubjects(new SubjectBuilder()
                    .withKind("ServiceAccount")
                    .withName(coName)
                    .withNamespace(namespaceName)
                    .build()
                )
                .build();
    }
}

