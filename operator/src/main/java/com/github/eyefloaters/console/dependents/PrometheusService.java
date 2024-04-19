package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusService.class)
public class PrometheusService extends BaseService {

    public PrometheusService() {
        super("prometheus", "prometheus.service.yaml", PrometheusService::name);
    }

    @Override
    protected String appName(Console primary) {
        return PrometheusDeployment.name(primary);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-prometheus-service";
    }

    public static String host(Console primary) {
        return "%s.%s.svc.cluster.local".formatted(name(primary), primary.getMetadata().getNamespace());
    }
}