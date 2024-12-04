package com.github.streamshub.console.dependents;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.Images;
import com.github.streamshub.console.api.v1alpha1.spec.containers.ContainerTemplate;
import com.github.streamshub.console.api.v1alpha1.spec.containers.Containers;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleLabelDiscriminator.class)
public class ConsoleDeployment extends CRUDKubernetesDependentResource<Deployment, Console> implements ConsoleResource {

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
    public String resourceName() {
        return NAME;
    }

    @Override
    protected Deployment desired(Console primary, Context<Console> context) {
        Deployment desired = load(context, "console.deployment.yaml", Deployment.class);
        String name = instanceName(primary);
        String configSecretName = secret.instanceName(primary);

        var containers = Optional.ofNullable(primary.getSpec().getContainers());
        var templateAPI = containers.map(Containers::getApi);
        var templateUI = containers.map(Containers::getUi);

        // deprecated
        var images = Optional.ofNullable(primary.getSpec().getImages());

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
                            .withImage(templateAPI.map(ContainerTemplate::getImage)
                                    .or(() -> images.map(Images::getApi))
                                    .orElse(defaultAPIImage))
                            // deprecated env list
                            .addAllToEnv(coalesce(primary.getSpec().getEnv(), Collections::emptyList))
                            // Env from template
                            .addAllToEnv(templateAPI.map(ContainerTemplate::getEnv).orElseGet(Collections::emptyList))
                            .withResources(templateAPI.map(ContainerTemplate::getResources).orElse(null))
                        .endContainer()
                        // Set UI container image options
                        .editMatchingContainer(c -> "console-ui".equals(c.getName()))
                            .withImage(templateUI.map(ContainerTemplate::getImage)
                                    .or(() -> images.map(Images::getUi))
                                    .orElse(defaultUIImage))
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
                            .addAllToEnv(templateUI.map(ContainerTemplate::getEnv).orElseGet(Collections::emptyList))
                            .withResources(templateUI.map(ContainerTemplate::getResources).orElse(null))
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }
}
