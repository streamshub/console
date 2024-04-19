package com.github.eyefloaters.console.dependents;

import java.util.Map;
import java.util.Optional;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = PrometheusDeployment.Discriminator.class)
public class PrometheusDeployment extends CRUDKubernetesDependentResource<Deployment, Console> implements ConsoleResource {

    public static class Discriminator implements ResourceDiscriminator<Deployment, Console> {
        @Override
        public Optional<Deployment> distinguish(Class<Deployment> resourceType, Console primary, Context<Console> context) {
            return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> "prometheus".equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
        }
    }

    public PrometheusDeployment() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(Console primary, Context<Console> context) {
        Deployment desired = load(context, "prometheus.deployment.yaml", Deployment.class);
        String name = name(primary);

        return desired.edit()
            .editMetadata()
                .withName(name)
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(MANAGEMENT_LABEL)
                .addToLabels(NAME_LABEL, "prometheus")
            .endMetadata()
            .editSpec()
                .editSelector()
                    .withMatchLabels(Map.of("app", name))
                .endSelector()
                .editTemplate()
                    .editMetadata()
                        .addToLabels(Map.of("app", name))
                    .endMetadata()
                    .editSpec()
                        .withServiceAccountName(PrometheusServiceAccount.name(primary))
                        .editMatchingVolume(c -> "config-volume".equals(c.getName()))
                            .editConfigMap()
                                .withName(PrometheusConfigMap.name(primary))
                            .endConfigMap()
                        .endVolume()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-prometheus";
    }
}