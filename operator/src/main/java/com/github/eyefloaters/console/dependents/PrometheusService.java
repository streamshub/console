package com.github.eyefloaters.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusService.class)
public class PrometheusService extends BaseService {

    public static final String NAME = "prometheus-service";

    @Inject
    PrometheusDeployment deployment;

    public PrometheusService() {
        super("prometheus", "prometheus.service.yaml");
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected String appName(Console primary) {
        return deployment.instanceName(primary);
    }

    @Override
    protected Service desired(Console primary, Context<Console> context) {
        Service desired = super.desired(primary, context);

        setAttribute(context, NAME + ".url", "http://%s.%s.svc.cluster.local:%d".formatted(
                desired.getMetadata().getName(),
                desired.getMetadata().getNamespace(),
                desired.getSpec().getPorts().get(0).getPort()));

        return desired;
    }

    public String host(Console primary) {
        return "%s.%s.svc.cluster.local".formatted(instanceName(primary), primary.getMetadata().getNamespace());
    }

}
