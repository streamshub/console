package com.github.eyefloaters.console.dependents;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusDeployment.Discriminator.class)
public class PrometheusDeployment extends CRUDKubernetesDependentResource<Deployment, Console> implements ConsoleResource {

    public static final String NAME = "prometheus-deployment";

    public static class Discriminator implements ResourceDiscriminator<Deployment, Console> {
        @Override
        public Optional<Deployment> distinguish(Class<Deployment> resourceType, Console primary, Context<Console> context) {
            return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> "prometheus".equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
        }
    }

    @Inject
    PrometheusServiceAccount serviceAccount;

    @Inject
    PrometheusConfigMap configMap;

    public PrometheusDeployment() {
        super(Deployment.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected Deployment desired(Console primary, Context<Console> context) {
        Deployment desired = load(context, "prometheus.deployment.yaml", Deployment.class);
        String name = instanceName(primary);

        return desired.edit()
            .editMetadata()
                .withName(name)
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("prometheus"))
            .endMetadata()
            .editSpec()
                .editSelector()
                    .withMatchLabels(Map.of(INSTANCE_LABEL, name))
                .endSelector()
                .editTemplate()
                    .editMetadata()
                        .addToLabels(Map.of(INSTANCE_LABEL, name))
                        .addToAnnotations("eyefloaters.github.com/dependency-digest", serializeDigest(context, "prometheus-digest"))
                    .endMetadata()
                    .editSpec()
                        .withServiceAccountName(serviceAccount.instanceName(primary))
                        .editMatchingVolume(c -> "config-volume".equals(c.getName()))
                            .editConfigMap()
                                .withName(configMap.instanceName(primary))
                            .endConfigMap()
                        .endVolume()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }

}
