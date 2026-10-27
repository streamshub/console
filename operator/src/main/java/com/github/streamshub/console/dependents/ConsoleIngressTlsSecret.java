package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * A {@code kubernetes.io/tls} Secret holding the user-provided certificate and key
 * ({@code spec.tls}) for use by the {@link ConsoleIngress} to terminate TLS. It is
 * only used on plain Kubernetes when Dex is enabled; in that case the Console pod and
 * Dex are served over HTTP behind the Ingress.
 */
@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleIngressTlsSecret extends BaseSecret {

    public static final String NAME = "console-ingress-tls";

    public ConsoleIngressTlsSecret() {
        super(NAME);
    }

    /**
     * Used as the {@code activationCondition} (rather than a reconcile precondition) so
     * that the {@link ConsoleIngress}, which depends on this resource, is still reconciled
     * when no Secret is needed.
     */
    public static class Activation implements Condition<Secret, Console> {
        @Override
        public boolean isMet(DependentResource<Secret, Console> dependentResource,
                Console primary,
                Context<Console> context) {
            return required(primary, context);
        }
    }

    static boolean required(Console primary, Context<Console> context) {
        return !ConfigSupport.resourceSupported(context, Route.class)
                && ConfigSupport.dexIntegrationEnabled(primary)
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
