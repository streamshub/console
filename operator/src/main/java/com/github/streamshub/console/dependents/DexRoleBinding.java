package com.github.streamshub.console.dependents;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class DexRoleBinding extends CRUDKubernetesDependentResource<RoleBinding, Console>
        implements ConsoleResource<RoleBinding> {

    public static final String NAME = "dex-rolebinding";

    @Inject
    DexRole role;

    @Inject
    DexServiceAccount serviceAccount;

    public DexRoleBinding() {
        super(RoleBinding.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    public Optional<RoleBinding> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    protected RoleBinding desired(Console primary, Context<Console> context) {
        return load(context, "dex.rolebinding.yaml", RoleBinding.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("dex"))
            .endMetadata()
            .editRoleRef()
                .withName(role.instanceName(primary))
            .endRoleRef()
            .editFirstSubject()
                .withName(serviceAccount.instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
            .endSubject()
            .build();
    }
}
