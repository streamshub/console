package com.github.eyefloaters.console.dependents;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleService.class)
public class ConsoleService extends BaseService {

    public ConsoleService() {
        super("console", "console.service.yaml", ConsoleService::name);
    }

    @Override
    protected String appName(Console primary) {
        return ConsoleDeployment.name(primary);
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console-service";
    }
}