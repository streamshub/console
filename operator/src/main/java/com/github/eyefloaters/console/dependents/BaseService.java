package com.github.eyefloaters.console.dependents;

import java.util.Optional;
import java.util.function.Function;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseService extends CRUDKubernetesDependentResource<Service, Console>
    implements ResourceDiscriminator<Service, Console>, ConsoleResource {

    private final String name;
    private final String resourceName;
    private final Function<Console, String> nameBuilder;

    protected BaseService(String name, String resourceName, Function<Console, String> nameBuilder) {
        super(Service.class);
        this.name = name;
        this.resourceName = resourceName;
        this.nameBuilder = nameBuilder;
    }

    @Override
    public Optional<Service> distinguish(Class<Service> resourceType, Console primary, Context<Console> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> name.equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
    }

    @Override
    protected Service desired(Console primary, Context<Console> context) {
        return load(context, resourceName, Service.class)
            .edit()
            .editMetadata()
                .withName(nameBuilder.apply(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(MANAGEMENT_LABEL)
                .addToLabels(NAME_LABEL, name)
            .endMetadata()
            .editSpec()
                .addToSelector("app", appName(primary))
            .endSpec()
            .build();
    }

    protected abstract String appName(Console primary);

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console-service";
    }
}