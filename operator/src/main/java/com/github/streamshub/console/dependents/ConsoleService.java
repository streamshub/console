package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleLabelDiscriminator.class)
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
}
