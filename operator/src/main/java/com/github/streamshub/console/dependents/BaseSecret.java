package com.github.streamshub.console.dependents;

import java.util.Optional;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseSecret extends CRUDKubernetesDependentResource<Secret, Console>
        implements ConsoleResource<Secret> {

    private final String resourceName;

    protected BaseSecret(String resourceName) {
        super(Secret.class);
        this.resourceName = resourceName;
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    @Override
    public Optional<Secret> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }
}
