package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusServiceAccount.class)
public class PrometheusServiceAccount extends BaseServiceAccount {

    public PrometheusServiceAccount() {
        super("prometheus", "prometheus.serviceaccount.yaml", PrometheusServiceAccount::name);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-prometheus-serviceaccount";
    }
}