package com.github.streamshub.console.dependents;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        namespaces = Constants.WATCH_ALL_NAMESPACES,
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleClusterRoleBinding.Discriminator.class)
public class ConsoleClusterRoleBinding extends BaseClusterRoleBinding {

    public static final String NAME = "console-clusterrolebinding";

    public static class Discriminator extends ConsoleLabelDiscriminator {
        public Discriminator() {
            super(Map.of(COMPONENT_LABEL, NAME));
        }
    }

    @Inject
    ConsoleClusterRole clusterRole;

    @Inject
    ConsoleServiceAccount serviceAccount;

    public ConsoleClusterRoleBinding() {
        super("console", "console.clusterrolebinding.yaml", NAME);
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
