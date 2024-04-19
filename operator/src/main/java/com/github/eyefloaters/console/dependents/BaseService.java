package com.github.eyefloaters.console.dependents;

import java.util.Optional;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseService extends CRUDKubernetesDependentResource<Service, Console>
    implements ResourceDiscriminator<Service, Console>, ConsoleResource {

    private final String appName;
    private final String resourceName;

    protected BaseService(String appName, String resourceName) {
        super(Service.class);
        this.appName = appName;
        this.resourceName = resourceName;
    }

    @Override
    public Optional<Service> distinguish(Class<Service> resourceType, Console primary, Context<Console> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> appName.equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
    }

    @Override
    protected Service desired(Console primary, Context<Console> context) {
        return load(context, resourceName, Service.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels(appName))
            .endMetadata()
            .editSpec()
                .addToSelector(INSTANCE_LABEL, appName(primary))
            .endSpec()
            .build();
    }

    protected abstract String appName(Console primary);

}
