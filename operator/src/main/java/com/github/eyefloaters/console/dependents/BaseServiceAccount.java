package com.github.eyefloaters.console.dependents;

import java.util.Optional;
import java.util.function.Function;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseServiceAccount extends CRUDKubernetesDependentResource<ServiceAccount, Console>
    implements ResourceDiscriminator<ServiceAccount, Console>,
        ConsoleResource {

    private final String name;
    private final String resourceName;
    private final Function<Console, String> nameBuilder;

    protected BaseServiceAccount(String name, String resourceName, Function<Console, String> nameBuilder) {
        super(ServiceAccount.class);
        this.name = name;
        this.resourceName = resourceName;
        this.nameBuilder = nameBuilder;
    }

    @Override
    public Optional<ServiceAccount> distinguish(Class<ServiceAccount> resourceType, Console primary, Context<Console> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> name.equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
    }

    @Override
    protected ServiceAccount desired(Console primary, Context<Console> context) {
        return load(context, resourceName, ServiceAccount.class)
            .edit()
            .editMetadata()
                .withName(nameBuilder.apply(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(MANAGEMENT_LABEL)
                .addToLabels(NAME_LABEL, name)
            .endMetadata()
            .build();
    }
}