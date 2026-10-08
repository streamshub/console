package com.github.streamshub.console.dependents;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * A ConfigMap annotated with
 * {@code service.beta.openshift.io/inject-cabundle: "true"} that OpenShift's
 * service CA controller fills with the cluster CA bundle under the key
 * {@code service-ca.crt}. Both {@link ConsoleRoute} and {@link DexRoute} read
 * this bundle as their {@code destinationCACertificate} for re-encrypt TLS
 * termination.
 * <p>
 * Active only on OpenShift (Route API must be available), gated by
 * {@link com.github.streamshub.console.dependents.conditions.OpenShiftPrecondition}.
 */
@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleTrustBundle extends CRUDKubernetesDependentResource<ConfigMap, Console>
        implements ConsoleResource<ConfigMap> {

    public static final String NAME = "console-ca-bundle";

    /** Key under which OCP injects the CA bundle. */
    public static final String CA_BUNDLE_KEY = "service-ca.crt";

    /** Path at which the bundle is mounted in the console container. */
    public static final String MOUNT_PATH = "/etc/service-ca";

    public ConsoleTrustBundle() {
        super(ConfigMap.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    public Optional<ConfigMap> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    protected ConfigMap desired(Console primary, Context<Console> context) {
        // No data is declared; the service CA controller owns the CA bundle entry.
        return new ConfigMapBuilder()
            .withNewMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("console"))
                .addToAnnotations("service.beta.openshift.io/inject-cabundle", "true")
            .endMetadata()
            .build();
    }

    /**
     * Blocks downstream dependents until OCP has injected the CA bundle into
     * the {@code service-ca.crt} key. Only ever evaluated on OpenShift because
     * {@link com.github.streamshub.console.dependents.conditions.OpenShiftPrecondition}
     * also gates the {@code activationCondition}.
     */
    public static class Postcondition implements Condition<ConfigMap, Console> {
        @Override
        public boolean isMet(DependentResource<ConfigMap, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            return dependentResource.getSecondaryResource(primary, context)
                    .map(ConfigMap::getData)
                    .map(data -> data.get(CA_BUNDLE_KEY))
                    .map(ca -> !ca.isBlank())
                    .orElse(false);
        }
    }
}
