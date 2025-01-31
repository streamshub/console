package com.github.streamshub.console.dependents;

import java.util.Optional;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseServiceAccount extends CRUDKubernetesDependentResource<ServiceAccount, Console>
    implements ConsoleResource<ServiceAccount> {

    private final String appName;
    private final String templateName;
    private final String resourceName;

    protected BaseServiceAccount(String appName, String templateName, String resourceName) {
        super(ServiceAccount.class);
        this.appName = appName;
        this.templateName = templateName;
        this.resourceName = resourceName;
    }

    @Override
    public Optional<ServiceAccount> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    @Override
    protected ServiceAccount desired(Console primary, Context<Console> context) {
        return load(context, templateName, ServiceAccount.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels(appName))
            .endMetadata()
            .build();
    }
}
