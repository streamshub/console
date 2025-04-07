package com.github.streamshub.console.dependents;

import java.util.Optional;
import java.util.Set;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

abstract class BaseClusterRoleBinding extends KubernetesDependentResource<ClusterRoleBinding, Console>
    implements SecondaryToPrimaryMapper<ClusterRoleBinding>,
        Creator<ClusterRoleBinding, Console>,
        Updater<ClusterRoleBinding, Console>,
        Deleter<Console>,
        ConsoleResource<ClusterRoleBinding> {

    private final String appName;
    private final String templateName;
    private final String resourceName;

    protected BaseClusterRoleBinding(String appName, String templateName, String resourceName) {
        super(ClusterRoleBinding.class);
        this.appName = appName;
        this.templateName = templateName;
        this.resourceName = resourceName;
    }

    @Override
    public Optional<ClusterRoleBinding> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    @Override
    protected ClusterRoleBinding desired(Console primary, Context<Console> context) {
        return load(context, templateName, ClusterRoleBinding.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withLabels(commonLabels(appName, resourceName))
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
        return Mappers.fromDefaultAnnotations(Console.class).toPrimaryResourceIDs(resource);
    }
}
