package com.github.streamshub.console.dependents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.Images;
import com.github.streamshub.console.api.v1alpha1.spec.containers.ContainerSpec;
import com.github.streamshub.console.api.v1alpha1.spec.containers.ContainerTemplateSpec;
import com.github.streamshub.console.api.v1alpha1.spec.containers.Containers;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
    informer = @Informer(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleDeployment extends BaseDeployment {

    public static final String NAME = "console-deployment";
    private static final String ALLOWED_IMAGE_PREFIXES = "console.deployment.allowed-image-prefixes";

    @Inject
    Logger logger;

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

    @Inject
    @ConfigProperty(name = ALLOWED_IMAGE_PREFIXES)
    Optional<List<String>> allowedImagePrefixes;

    List<String> effectiveAllowedPrefixes;

    @PostConstruct
    void initAllowedPrefixes() {
        effectiveAllowedPrefixes = allowedImagePrefixes.orElseGet(() ->
            List.of(defaultAPIImage.substring(0, defaultAPIImage.lastIndexOf('/') + 1)));

        if (effectiveAllowedPrefixes.stream().noneMatch(defaultAPIImage::startsWith)) {
            logger.warnf(
                    "Configured `%s` of %s blocks use of default API image `%s`",
                    ALLOWED_IMAGE_PREFIXES,
                    effectiveAllowedPrefixes,
                    defaultAPIImage);
        }

        if (effectiveAllowedPrefixes.stream().noneMatch(defaultUIImage::startsWith)) {
            logger.warnf(
                    "Configured `%s` of %s blocks use of default UI image `%s`",
                    ALLOWED_IMAGE_PREFIXES,
                    effectiveAllowedPrefixes,
                    defaultUIImage);
        }
    }

    public ConsoleDeployment() {
        super(NAME);
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
        validateImage(imageAPI, effectiveAllowedPrefixes);
        String imageUI = templateUI.map(ContainerSpec::getImage)
                .or(() -> images.map(Images::getUi))
                .orElse(defaultUIImage);
        validateImage(imageUI, effectiveAllowedPrefixes);

        List<EnvVar> envVars = new ArrayList<>();
        envVars.addAll(coalesce(primary.getSpec().getEnv(), Collections::emptyList));
        envVars.addAll(templateAPI.map(ContainerSpec::getEnv).orElseGet(Collections::emptyList));

        List<EnvVar> envVarsUI = new ArrayList<>();
        envVarsUI.addAll(templateUI.map(ContainerSpec::getEnv).orElseGet(Collections::emptyList));

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
                        // Set API container image options
                        .editMatchingContainer(c -> "console-api".equals(c.getName()))
                            .withImage(imageAPI)
                            .withImagePullPolicy(pullPolicy(imageAPI))
                            .withResources(templateAPI.map(ContainerSpec::getResources).orElse(null))
                            .addAllToEnv(envVars)
                        .endContainer()
                        // Set UI container image options
                        .editMatchingContainer(c -> "console-ui".equals(c.getName()))
                            .withImage(imageUI)
                            .withImagePullPolicy(pullPolicy(imageUI))
                            .withResources(templateUI.map(ContainerSpec::getResources).orElse(null))
                            .editMatchingEnv(env -> "NEXTAUTH_URL".equals(env.getName()))
                                .withValue(getAttribute(context, INGRESS_URL_KEY, String.class))
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
        return context.managedWorkflowAndDependentResourceContext().getMandatory(key, Map.class);
    }

    @SuppressWarnings("unchecked")
    <R extends KubernetesResource> List<R> getResourcesByType(
            Map<Class<KubernetesResource>, List<KubernetesResource>> resources,
            Class<R> key) {
        return (List<R>) resources.getOrDefault(key, Collections.emptyList());
    }

    private static void validateImage(String image, List<String> allowedPrefixes) {
        if (allowedPrefixes.stream().noneMatch(image::startsWith)) {
            throw new ReconciliationException(
                "Console image '%s' is not permitted. Allowed prefixes: %s"
                    .formatted(image, allowedPrefixes));
        }
    }

    private String pullPolicy(String image) {
        return image.contains("sha256:") ? "IfNotPresent" : "Always";
    }
}
