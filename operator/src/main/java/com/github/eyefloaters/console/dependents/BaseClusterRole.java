package com.github.eyefloaters.console.dependents;

import java.util.Optional;
import java.util.Set;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

abstract class BaseClusterRole extends KubernetesDependentResource<ClusterRole, Console>
    implements SecondaryToPrimaryMapper<ClusterRole>,
        ResourceDiscriminator<ClusterRole, Console>,
        Creator<ClusterRole, Console>,
        Updater<ClusterRole, Console>,
        Deleter<Console>,
        ConsoleResource {

    private final String appName;
    private final String resourceName;

    protected BaseClusterRole(String appName, String resourceName) {
        super(ClusterRole.class);
        this.appName = appName;
        this.resourceName = resourceName;
    }

    @Override
    public Optional<ClusterRole> distinguish(Class<ClusterRole> resourceType, Console primary, Context<Console> context) {
        return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> appName.equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
    }

    @Override
    protected ClusterRole desired(Console primary, Context<Console> context) {
        return load(context, resourceName, ClusterRole.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withLabels(commonLabels(appName))
            .endMetadata()
            .build();
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(ClusterRole resource) {
        return Mappers.fromDefaultAnnotations().toPrimaryResourceIDs(resource);
    }
}
