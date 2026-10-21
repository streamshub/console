package com.github.streamshub.console.dependents;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import com.github.streamshub.console.api.v1alpha1.spec.template.ContainerTemplate;
import com.github.streamshub.console.api.v1alpha1.spec.template.ContainerTemplate.ImagePullPolicy;
import com.github.streamshub.console.api.v1alpha1.spec.template.DeploymentSpecTemplate;
import com.github.streamshub.console.api.v1alpha1.spec.template.DeploymentTemplate;
import com.github.streamshub.console.api.v1alpha1.spec.template.MetadataTemplate;
import com.github.streamshub.console.api.v1alpha1.spec.template.PodSpecTemplate;
import com.github.streamshub.console.api.v1alpha1.spec.template.PodTemplate;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Reasons;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Types;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.IntOrString;
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
    @ConfigProperty(name = ALLOWED_IMAGE_PREFIXES)
    Optional<List<String>> allowedImagePrefixes;

    List<String> effectiveAllowedPrefixes;

    @PostConstruct
    void initAllowedPrefixes() {
        effectiveAllowedPrefixes = allowedImagePrefixes.orElseGet(() ->
            List.of(defaultAllowedPrefix(defaultAPIImage)));

        if (effectiveAllowedPrefixes.stream().noneMatch(defaultAPIImage::startsWith)) {
            logger.warnf(
                    "Configured `%s` of %s blocks use of default API image `%s`",
                    ALLOWED_IMAGE_PREFIXES,
                    effectiveAllowedPrefixes,
                    defaultAPIImage);
        }
    }

    /**
     * The prefix permitted when no allowlist is configured: everything in the default
     * image up to and including its last {@code /}. An image without a {@code /} has no
     * such prefix and returning an empty prefix would permit every image.
     * Therefore, in the case of a default image containing no {@code /}, this method will return the default image name itself as the prefix.
     */
    static String defaultAllowedPrefix(String defaultImage) {
        int lastSlash = defaultImage.lastIndexOf('/');
        return lastSlash < 0 ? defaultImage : defaultImage.substring(0, lastSlash + 1);
    }

    public ConsoleDeployment() {
        super(NAME);
    }

    @Override
    protected Deployment desired(Console primary, Context<Console> context) {
        Deployment desired = load(context, "console.deployment.yaml", Deployment.class);
        String name = instanceName(primary);
        String configSecretName = secret.instanceName(primary);

        var template = Optional.ofNullable(primary.getSpec().getDeployment());
        var metaTemplate = template.map(DeploymentTemplate::getMetadata);

        var podTemplate = template.map(DeploymentTemplate::getSpec).map(DeploymentSpecTemplate::getTemplate);
        var podMetaTemplate = podTemplate.map(PodTemplate::getMetadata);
        var podSpecTemplate = podTemplate.map(PodTemplate::getSpec);
        var podRequiredLabels = Map.of(INSTANCE_LABEL, name);
        var podRequiredAnnotations = Map.of("streamshub.github.com/dependency-digest",
                serializeDigest(context, "console-digest"));
        var containerTemplate = podSpecTemplate.map(PodSpecTemplate::getServerContainer);

        // deprecated
        var containers = Optional.ofNullable(primary.getSpec().getContainers());
        var templateAPI = containers.map(Containers::getApi).map(ContainerTemplateSpec::getSpec);
        var images = Optional.ofNullable(primary.getSpec().getImages());
        var envs = Optional.ofNullable(primary.getSpec().getEnv());
        warnDeprecations(primary, containers, images, envs);

        String image = containerTemplate.map(ContainerTemplate::getImage)
                .or(() -> templateAPI.map(ContainerSpec::getImage))
                .or(() -> images.map(Images::getApi))
                .orElse(defaultAPIImage);
        validateImage(image, effectiveAllowedPrefixes);
        String imagePullPolicy = containerTemplate.map(ContainerTemplate::getImagePullPolicy)
                .map(ImagePullPolicy::value)
                .orElseGet(() -> pullPolicy(image));
        var containerResources = containerTemplate.map(ContainerTemplate::getResources)
                .or(() -> templateAPI.map(ContainerSpec::getResources))
                .orElse(null);

        boolean tls = primary.getSpec().getTls() != null;
        String portName = tls ? "https" : "http";
        int containerPort = tls ? 8443 : 8080;
        String probeScheme = tls ? "HTTPS" : "HTTP";

        return desired.edit()
            .editMetadata()
                .withName(name)
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(merge(
                        commonLabels("console"),
                        metaTemplate.map(MetadataTemplate::getLabels)
                ))
                .addToAnnotations(metaTemplate.map(MetadataTemplate::getAnnotations)
                        .orElseGet(Collections::emptyMap))
            .endMetadata()
            .editSpec()
                .editSelector()
                    .withMatchLabels(podRequiredLabels)
                .endSelector()
                .editTemplate()
                    .editMetadata()
                        .addToLabels(merge(
                                podRequiredLabels,
                                podMetaTemplate.map(MetadataTemplate::getLabels)
                        ))
                        .addToAnnotations(merge(
                                podRequiredAnnotations,
                                podMetaTemplate.map(MetadataTemplate::getAnnotations)
                        ))
                    .endMetadata()
                    .editSpec()
                        .withAffinity(podSpecTemplate
                                .map(PodSpecTemplate::getAffinity)
                                .orElse(null))
                        .withTolerations(podSpecTemplate
                                .map(PodSpecTemplate::getTolerations)
                                .orElse(null))
                        .withTopologySpreadConstraints(podSpecTemplate
                                .map(PodSpecTemplate::getTopologySpreadConstraints)
                                .orElse(null))
                        .withNodeSelector(podSpecTemplate
                                .map(PodSpecTemplate::getNodeSelector)
                                .orElse(null))
                        .withServiceAccountName(serviceAccount.instanceName(primary))
                        .editMatchingVolume(vol -> "config".equals(vol.getName()))
                            .editSecret()
                                .withSecretName(configSecretName)
                            .endSecret()
                        .endVolume()
                        // Set API container image and port options
                        .editMatchingContainer(c -> "console-api".equals(c.getName()))
                            .withImage(image)
                            .withImagePullPolicy(imagePullPolicy)
                            .withResources(containerResources)
                            .withEnv(buildEnvVars(desired, List.of(
                                    // new way, preferred
                                    containerTemplate.map(ContainerTemplate::getEnv),
                                    // deprecated, lower precedence
                                    templateAPI.map(ContainerSpec::getEnv),
                                    envs)
                            ))
                            .editFirstPort()
                                .withName(portName)
                                .withContainerPort(containerPort)
                            .endPort()
                            .editStartupProbe()
                                .editHttpGet()
                                    .withPort(new IntOrString(portName))
                                    .withScheme(probeScheme)
                                .endHttpGet()
                            .endStartupProbe()
                            .editLivenessProbe()
                                .editHttpGet()
                                    .withPort(new IntOrString(portName))
                                    .withScheme(probeScheme)
                                .endHttpGet()
                            .endLivenessProbe()
                            .editReadinessProbe()
                                .editHttpGet()
                                    .withPort(new IntOrString(portName))
                                    .withScheme(probeScheme)
                                .endHttpGet()
                            .endReadinessProbe()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }

    private static Map<String, String> merge(Map<String, String> required, Optional<Map<String, String>> configured) {
        Map<String, String> result = new LinkedHashMap<>(required);
        configured.ifPresent(values -> values.forEach(result::putIfAbsent));
        return result;
    }

    private static List<EnvVar> buildEnvVars(Deployment base, List<Optional<List<EnvVar>>> configured) {
        List<EnvVar> envVars = new ArrayList<>(base.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv());

        configured.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(Collection::stream)
            .forEach(envVar -> {
                String configuredName = envVar.getName();

                if (envVars.stream().map(EnvVar::getName).noneMatch(configuredName::equals)) {
                    envVars.add(envVar);
                }
            });

        return envVars;
    }

    private static void validateImage(String image, List<String> allowedPrefixes) {
        if (allowedPrefixes.stream().noneMatch(image::startsWith)) {
            throw new ReconciliationException(
                "Console API image '%s' is not permitted. Allowed prefixes: %s"
                    .formatted(image, allowedPrefixes));
        }
    }

    private static String pullPolicy(String image) {
        return image.contains("sha256:")
                ? ImagePullPolicy.IF_NOT_PRESENT.value()
                : ImagePullPolicy.ALWAYS.value();
    }

    private static void warnDeprecations(Console primary, Optional<?> containers, Optional<?> images, Optional<?> envs) {
        StringBuilder deprecationWarnings = new StringBuilder();

        if (containers.isPresent()) {
            deprecationWarnings.append("""
                    spec.containers is deprecated and will be removed \
                    in a future release. Please use \
                    spec.deployment.spec.template.spec.serverContainer \
                    to configure the server container.""");
        }

        if (images.isPresent()) {
            if (!deprecationWarnings.isEmpty()) {
                deprecationWarnings.append(' ');
            }

            deprecationWarnings.append("""
                    spec.images is deprecated and will be removed \
                    in a future release. Please use \
                    spec.deployment.spec.template.spec.serverContainer.image \
                    to configure the server container image.""");
        }

        if (envs.isPresent()) {
            if (!deprecationWarnings.isEmpty()) {
                deprecationWarnings.append(' ');
            }

            deprecationWarnings.append("""
                    spec.env is deprecated and will be removed \
                    in a future release. Please use \
                    spec.deployment.spec.template.spec.serverContainer.env \
                    to configure the server container.""");
        }

        if (!deprecationWarnings.isEmpty()) {
            primary.getOrCreateStatus().updateCondition(new ConditionBuilder()
                    .withType(Types.WARNING)
                    .withStatus("True")
                    .withLastTransitionTime(Instant.now().toString())
                    .withReason(Reasons.DEPRECATED_CONFIGURATION)
                    .withMessage(deprecationWarnings.toString())
                    .build());
        }
    }
}
