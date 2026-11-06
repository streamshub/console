package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * A {@code kubernetes.io/tls} Secret holding the user-provided certificate and key
 * ({@code spec.tls}). On plain Kubernetes with Dex enabled, it is referenced by {@link ConsoleIngress}
 * to terminate TLS at the ingress. On OpenShift with Dex enabled, it is referenced by {@link ConsoleRoute}
 * and {@link DexRoute} via {@code externalCertificate} so the user's private key is not exposed inline
 * in the Route definition.
 */
@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleTlsSecret extends BaseSecret {

    public static final String NAME = "console-tls";

    public ConsoleTlsSecret() {
        super(NAME);
    }

    public static class Precondition implements Condition<Secret, Console> {
        @Override
        public boolean isMet(DependentResource<Secret, Console> dependentResource,
                Console primary,
                Context<Console> context) {
            return required(primary, context);
        }
    }

    public static boolean required(Console primary, Context<Console> context) {
        return ConfigSupport.dexIntegrationEnabled(primary)
                && primary.getSpec().getTls() != null;
    }

    @Override
    protected Secret desired(Console primary, Context<Console> context) {
        String namespace = primary.getMetadata().getNamespace();
        var tls = primary.getSpec().getTls();
        byte[] certificate = ConfigSupport.getValue(context, namespace, tls.getCertificate());
        byte[] key = ConfigSupport.getValue(context, namespace, tls.getKey());

        if (certificate == null || key == null) {
            throw new ReconciliationException("spec.tls certificate and key must resolve to a value");
        }

        return new SecretBuilder()
                .withNewMetadata()
                    .withName(instanceName(primary))
                    .withNamespace(namespace)
                    .withLabels(commonLabels("console"))
                .endMetadata()
                .withType("kubernetes.io/tls")
                .addToData("tls.crt", ConfigSupport.encodeBytes(certificate))
                .addToData("tls.key", ConfigSupport.encodeBytes(key))
                .build();
    }
}
