package com.github.streamshub.console.dependents;

import java.util.Optional;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseDeployment extends CRUDKubernetesDependentResource<Deployment, Console>
    implements ConsoleResource<Deployment> {

    private final String resourceName;

    protected BaseDeployment(String resourceName) {
        super(Deployment.class);
        this.resourceName = resourceName;
    }

    @Override
    public Optional<Deployment> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    public String resourceName() {
        return resourceName;
    }
}
