package com.github.eyefloaters.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleClusterRole.class)
public class ConsoleClusterRole extends BaseClusterRole {

    public static final String NAME = "console-clusterrole";

    public ConsoleClusterRole() {
        super("console", "console.clusterrole.yaml");
    }

    @Override
    public String resourceName() {
        return NAME;
    }

}
