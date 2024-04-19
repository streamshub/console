package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleServiceAccount.class)
public class ConsoleServiceAccount extends BaseServiceAccount {

    public ConsoleServiceAccount() {
        super("console", "console.serviceaccount.yaml", ConsoleServiceAccount::name);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console-serviceaccount";
    }
}