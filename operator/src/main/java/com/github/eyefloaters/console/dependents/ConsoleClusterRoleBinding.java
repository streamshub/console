package com.github.eyefloaters.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleClusterRoleBinding.class)
public class ConsoleClusterRoleBinding extends BaseClusterRoleBinding {

    public static final String NAME = "console-clusterrolebinding";

    @Inject
    ConsoleClusterRole clusterRole;

    @Inject
    ConsoleServiceAccount serviceAccount;

    public ConsoleClusterRoleBinding() {
        super("console", "console.clusterrolebinding.yaml");
    }

    @Override
    public String resourceName() {
        return NAME;
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
