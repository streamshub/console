package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleService extends BaseService {

    public static final String NAME = "console-service";

    @Inject
    ConsoleDeployment deployment;

    public ConsoleService() {
        super("console", "console.service.yaml", NAME);
    }

    @Override
    protected String appName(Console primary) {
        return deployment.instanceName(primary);
    }

    @Override
    protected Service desired(Console primary, Context<Console> context) {
        boolean tls = primary.getSpec().getTls() != null;

        return super.desired(primary, context)
            .edit()
            .editSpec()
                .editFirstPort()
                    .withName(tls ? "https" : "http")
                    .withPort(tls ? 443 : 80)
                    .withTargetPort(new IntOrString(tls ? 8443 : 8080))
                .endPort()
            .endSpec()
            .build();
    }
}
