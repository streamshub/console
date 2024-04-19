package com.github.eyefloaters.console.dependents;

import java.util.Optional;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseServiceAccount extends CRUDKubernetesDependentResource<ServiceAccount, Console>
    implements ResourceDiscriminator<ServiceAccount, Console>,
        ConsoleResource {

    private final String appName;
    private final String resourceName;

    protected BaseServiceAccount(String appName, String resourceName) {
        super(ServiceAccount.class);
        this.appName = appName;
        this.resourceName = resourceName;
    }

    @Override
    public Optional<ServiceAccount> distinguish(Class<ServiceAccount> resourceType, Console primary, Context<Console> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> appName.equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
    }

    @Override
    protected ServiceAccount desired(Console primary, Context<Console> context) {
        return load(context, resourceName, ServiceAccount.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels(appName))
            .endMetadata()
            .build();
    }
}
