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

    @Inject
    ConsoleServiceAccount serviceAccount;

    @Inject
    ConsoleSecret secret;

    @Inject
    @ConfigProperty(name = "console.deployment.default-api-image")
    String defaultAPIImage;

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
        // deprecated
        var images = Optional.ofNullable(primary.getSpec().getImages());

        String imageAPI = templateAPI.map(ContainerSpec::getImage)
                .or(() -> images.map(Images::getApi))
                .orElse(defaultAPIImage);

        List<EnvVar> envVars = new ArrayList<>();
        envVars.addAll(coalesce(primary.getSpec().getEnv(), Collections::emptyList));
        envVars.addAll(templateAPI.map(ContainerSpec::getEnv).orElseGet(Collections::emptyList));

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
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }

    private String pullPolicy(String image) {
        return image.contains("sha256:") ? "IfNotPresent" : "Always";
    }
}
