package com.github.streamshub.console.dependents;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleSecret extends CRUDKubernetesDependentResource<Secret, Console> implements ConsoleResource<Secret> {

    public static final String NAME = "console-secret";

    public ConsoleSecret() {
        super(Secret.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected Secret desired(Console primary, Context<Console> context) {
        @SuppressWarnings("unchecked")
        Map<String, String> data = context.managedWorkflowAndDependentResourceContext()
                .getMandatory("ConsoleSecretData", Map.class);

        updateDigest(context, "console-digest", data);

        return new SecretBuilder()
                .withNewMetadata()
                    .withName(instanceName(primary))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .withLabels(commonLabels("console"))
                .endMetadata()
                .withData(data)
                .build();
    }
}
