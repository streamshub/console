package com.github.streamshub.console.dependents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.Images;
import com.github.streamshub.console.api.v1alpha1.spec.containers.ContainerSpec;
import com.github.streamshub.console.api.v1alpha1.spec.containers.ContainerTemplateSpec;
import com.github.streamshub.console.api.v1alpha1.spec.containers.Containers;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class ConsoleDeployment extends CRUDKubernetesDependentResource<Deployment, Console> implements ConsoleResource<Deployment> {

    public static final String NAME = "console-deployment";

    @Inject
    ConsoleServiceAccount serviceAccount;

    @Inject
    ConsoleSecret secret;

    @Inject
    @ConfigProperty(name = "console.deployment.default-api-image")
    String defaultAPIImage;

    @Inject
    @ConfigProperty(name = "console.deployment.default-ui-image")
    String defaultUIImage;

    public ConsoleDeployment() {
        super(Deployment.class);
    }

    @Override
    public Optional<Deployment> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected Deployment desired(Console primary, Context<Console> context) {
        Deployment desired = load(context, "console.deployment.yaml", Deployment.class);
        String name = instanceName(primary);
        String configSecretName = secret.instanceName(primary);

        var containers = Optional.ofNullable(primary.getSpec().getContainers());
        var templateAPI = containers.map(Containers::getApi).map(ContainerTemplateSpec::getSpec);
        var templateUI = containers.map(Containers::getUi).map(ContainerTemplateSpec::getSpec);
        // deprecated
        var images = Optional.ofNullable(primary.getSpec().getImages());

        String imageAPI = templateAPI.map(ContainerSpec::getImage)
                .or(() -> images.map(Images::getApi))
                .orElse(defaultAPIImage);
        String imageUI = templateUI.map(ContainerSpec::getImage)
                .or(() -> images.map(Images::getUi))
                .orElse(defaultUIImage);

        var trustResources = getTrustResources("TrustStoreResources", context);
        List<EnvVar> envVars = new ArrayList<>();
        envVars.addAll(coalesce(primary.getSpec().getEnv(), Collections::emptyList));
        envVars.addAll(templateAPI.map(ContainerSpec::getEnv).orElseGet(Collections::emptyList));
        envVars.addAll(getResourcesByType(trustResources, EnvVar.class));

        var trustResourcesUI = getTrustResources("TrustStoreResourcesUI", context);
        List<EnvVar> envVarsUI = new ArrayList<>();
        envVarsUI.addAll(templateUI.map(ContainerSpec::getEnv).orElseGet(Collections::emptyList));
        envVarsUI.addAll(getResourcesByType(trustResourcesUI, EnvVar.class));

        return desired.edit()
            .editMetadata()
                .withName(name)
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("console"))
            .endMetadata()
            .editSpec()
                .editSelector()
                    .withMatchLabels(Map.of(INSTANCE_LABEL, name))
                .endSelector()
                .editTemplate()
                    .editMetadata()
                        .addToLabels(Map.of(INSTANCE_LABEL, name))
                        .addToAnnotations(
                                "streamshub.github.com/dependency-digest",
                                serializeDigest(context, "console-digest"))
                    .endMetadata()
                    .editSpec()
                        .withServiceAccountName(serviceAccount.instanceName(primary))
                        .editMatchingVolume(vol -> "config".equals(vol.getName()))
                            .editSecret()
                                .withSecretName(configSecretName)
                            .endSecret()
                        .endVolume()
                        .addAllToVolumes(getResourcesByType(trustResources, Volume.class))
                        // Set API container image options
                        .editMatchingContainer(c -> "console-api".equals(c.getName()))
                            .withImage(imageAPI)
                            .withImagePullPolicy(pullPolicy(imageAPI))
                            .withResources(templateAPI.map(ContainerSpec::getResources).orElse(null))
                            .addAllToVolumeMounts(getResourcesByType(trustResources, VolumeMount.class))
                            .addAllToEnv(envVars)
                        .endContainer()
                        // Set UI container image options
                        .editMatchingContainer(c -> "console-ui".equals(c.getName()))
                            .withImage(imageUI)
                            .withImagePullPolicy(pullPolicy(imageUI))
                            .withResources(templateUI.map(ContainerSpec::getResources).orElse(null))
                            .editMatchingEnv(env -> "NEXTAUTH_URL".equals(env.getName()))
                                .withValue(getAttribute(context, ConsoleIngress.NAME + ".url", String.class))
                            .endEnv()
                            .editMatchingEnv(env -> "NEXTAUTH_SECRET".equals(env.getName()))
                                .editValueFrom()
                                    .editSecretKeyRef()
                                        .withName(configSecretName)
                                    .endSecretKeyRef()
                                .endValueFrom()
                            .endEnv()
                            .addAllToEnv(envVarsUI)
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }

    @SuppressWarnings("unchecked")
    <R extends KubernetesResource> Map<Class<R>, List<R>> getTrustResources(String key, Context<Console> context) {
        return context.managedDependentResourceContext().getMandatory(key, Map.class);
    }

    @SuppressWarnings("unchecked")
    <R extends KubernetesResource> List<R> getResourcesByType(
            Map<Class<KubernetesResource>, List<KubernetesResource>> resources,
            Class<R> key) {
        return (List<R>) resources.getOrDefault(key, Collections.emptyList());
    }

    private String pullPolicy(String image) {
        return image.contains("sha256:") ? "IfNotPresent" : "Always";
    }
}
