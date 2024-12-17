package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.dependents.discriminators.PrometheusLabelDiscriminator;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusLabelDiscriminator.class)
public class PrometheusServiceAccount extends BaseServiceAccount {

    public static final String NAME = "prometheus-serviceaccount";

    public PrometheusServiceAccount() {
        super("prometheus", "prometheus.serviceaccount.yaml", NAME);
    }

}
