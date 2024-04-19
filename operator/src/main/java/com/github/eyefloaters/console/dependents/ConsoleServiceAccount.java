package com.github.eyefloaters.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleServiceAccount.class)
public class ConsoleServiceAccount extends BaseServiceAccount {

    public static final String NAME = "console-serviceaccount";

    public ConsoleServiceAccount() {
        super("console", "console.serviceaccount.yaml");
    }

    @Override
    public String resourceName() {
        return NAME;
    }
}
