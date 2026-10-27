package com.github.streamshub.console.dependents;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.rbac.Role;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Namespaced Role granting Dex access to its own storage objects
 * ({@code dex.coreos.com}) in the Console namespace only.
 */
@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class DexRole extends CRUDKubernetesDependentResource<Role, Console>
        implements ConsoleResource<Role> {

    public static final String NAME = "dex-role";

    public DexRole() {
        super(Role.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    public Optional<Role> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    protected Role desired(Console primary, Context<Console> context) {
        return load(context, "dex.role.yaml", Role.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("dex"))
            .endMetadata()
            .build();
    }
}
