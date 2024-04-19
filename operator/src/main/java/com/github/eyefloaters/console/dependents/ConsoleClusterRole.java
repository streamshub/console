package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleClusterRole.class)
public class ConsoleClusterRole extends BaseClusterRole {

    public ConsoleClusterRole() {
        super("console", "console.clusterrole.yaml", ConsoleClusterRole::name);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console-clusterrole";
    }

}