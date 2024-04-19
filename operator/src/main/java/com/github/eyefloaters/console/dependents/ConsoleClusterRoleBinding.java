package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleClusterRoleBinding.class)
public class ConsoleClusterRoleBinding extends BaseClusterRoleBinding {

    public ConsoleClusterRoleBinding() {
        super("console", "console.clusterrolebinding.yaml", ConsoleClusterRoleBinding::name);
    }

    @Override
    protected String roleName(Console primary) {
        return ConsoleClusterRole.name(primary);
    }

    @Override
    protected String subjectName(Console primary) {
        return ConsoleServiceAccount.name(primary);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console-clusterrolebinding";
    }
}
