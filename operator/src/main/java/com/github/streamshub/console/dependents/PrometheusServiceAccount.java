package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class PrometheusServiceAccount extends BaseServiceAccount {

    public static final String NAME = "prometheus-serviceaccount";

    public PrometheusServiceAccount() {
        super("prometheus", "prometheus.serviceaccount.yaml", NAME);
    }

}
