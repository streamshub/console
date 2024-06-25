package com.github.streamshub.console.dependents;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseService extends CRUDKubernetesDependentResource<Service, Console> implements ConsoleResource {

    private final String appName;
    private final String templateName;
    private final String resourceName;

    protected BaseService(String appName, String templateName, String resourceName) {
        super(Service.class);
        this.appName = appName;
        this.templateName = templateName;
        this.resourceName = resourceName;
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    @Override
    protected Service desired(Console primary, Context<Console> context) {
        return load(context, templateName, Service.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels(appName))
            .endMetadata()
            .editSpec()
                .addToSelector(INSTANCE_LABEL, appName(primary))
            .endSpec()
            .build();
    }

    protected abstract String appName(Console primary);

}
