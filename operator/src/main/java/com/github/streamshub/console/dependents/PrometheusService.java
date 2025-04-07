package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class PrometheusService extends BaseService {

    public static final String NAME = "prometheus-service";

    @Inject
    PrometheusDeployment deployment;

    public PrometheusService() {
        super("prometheus", "prometheus.service.yaml", NAME);
    }

    @Override
    protected String appName(Console primary) {
        return deployment.instanceName(primary);
    }

    String getUrl(Console primary, Context<Console> context) {
        Service desired = super.desired(primary, context);

        return "http://%s.%s.svc.cluster.local:%d".formatted(
                desired.getMetadata().getName(),
                desired.getMetadata().getNamespace(),
                desired.getSpec().getPorts().get(0).getPort());
    }
}
