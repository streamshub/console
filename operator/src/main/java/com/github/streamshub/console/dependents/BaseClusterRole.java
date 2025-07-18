package com.github.streamshub.console.dependents;

import java.util.Optional;
import java.util.Set;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

abstract class BaseClusterRole extends KubernetesDependentResource<ClusterRole, Console>
    implements SecondaryToPrimaryMapper<ClusterRole>,
        Creator<ClusterRole, Console>,
        Updater<ClusterRole, Console>,
        Deleter<Console>,
        ConsoleResource<ClusterRole> {

    private final String appName;
    private final String templateName;
    private final String resourceName;

    protected BaseClusterRole(String appName, String templateName, String resourceName) {
        super(ClusterRole.class);
        this.appName = appName;
        this.templateName = templateName;
        this.resourceName = resourceName;
    }

    @Override
    public String instanceName(Console primary) {
        return primary.getMetadata().getNamespace() + "-" + ConsoleResource.super.instanceName(primary);
    }

    @Override
    public Optional<ClusterRole> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    @Override
    protected ClusterRole desired(Console primary, Context<Console> context) {
        return load(context, templateName, ClusterRole.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withLabels(commonLabels(appName))
            .endMetadata()
            .build();
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(ClusterRole resource) {
        return Mappers.fromDefaultAnnotations(Console.class).toPrimaryResourceIDs(resource);
    }
}
