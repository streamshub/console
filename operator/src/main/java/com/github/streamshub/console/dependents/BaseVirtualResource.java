package com.github.streamshub.console.dependents;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.NameSetter;

/**
 * Base class to "virtual" resources. These are dependent resources that do not
 * create any object in Kubernetes. Rather, they serve as gates in the reconcile
 * workflow that check conditions of dependents earlier in the DAG, map
 * configurations, etc.
 */
abstract class BaseVirtualResource
        implements DependentResource<HasMetadata, Console>, NameSetter, ConsoleResource<HasMetadata> {

    private final String resourceName;
    private String instanceName;

    protected BaseVirtualResource(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String name() {
        return instanceName;
    }

    @Override
    public void setName(String name) {
        this.instanceName = name;
    }

    @Override
    public Class<HasMetadata> resourceType() {
        return HasMetadata.class;
    }

    @Override
    public String resourceName() {
        return resourceName;
    }
}
