package com.github.eyefloaters.console.dependents;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

abstract class BaseClusterRoleBinding extends KubernetesDependentResource<ClusterRoleBinding, Console>
    implements SecondaryToPrimaryMapper<ClusterRoleBinding>,
        ResourceDiscriminator<ClusterRoleBinding, Console>,
        Creator<ClusterRoleBinding, Console>,
        Updater<ClusterRoleBinding, Console>,
        Deleter<Console>,
        ConsoleResource {

    private final String name;
    private final String resourceName;
    private final Function<Console, String> nameBuilder;

    protected BaseClusterRoleBinding(String name, String resourceName, Function<Console, String> nameBuilder) {
        super(ClusterRoleBinding.class);
        this.name = name;
        this.resourceName = resourceName;
        this.nameBuilder = nameBuilder;
    }

    @Override
    public Optional<ClusterRoleBinding> distinguish(Class<ClusterRoleBinding> resourceType, Console primary, Context<Console> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> name.equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
    }

    @Override
    protected ClusterRoleBinding desired(Console primary, Context<Console> context) {
        return load(context, resourceName, ClusterRoleBinding.class)
            .edit()
            .editMetadata()
                .withName(nameBuilder.apply(primary))
                .withLabels(MANAGEMENT_LABEL)
                .addToLabels(NAME_LABEL, name)
            .endMetadata()
            .editRoleRef()
                .withName(roleName(primary))
            .endRoleRef()
            .editFirstSubject()
                .withName(subjectName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
            .endSubject()
            .build();
    }

    protected abstract String roleName(Console primary);

    protected abstract String subjectName(Console primary);

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(ClusterRoleBinding resource) {
        return Mappers.fromDefaultAnnotations().toPrimaryResourceIDs(resource);
    }

}