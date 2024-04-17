package com.github.streamshub.console.dependents;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.streamshub.console.api.v1alpha1.Console;

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
    private static final String DEFAULT_IMAGE_API = "quay.io/streamshub/console-api";
    private static final String DEFAULT_IMAGE_UI = "quay.io/streamshub/console-ui";

    @Inject
    PrometheusService prometheusService;

    @Inject
    ConsoleServiceAccount serviceAccount;

    @Inject
    ConsoleSecret secret;

    @Inject
    @ConfigProperty(name = "quarkus.application.version")
    String applicationVersion;

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
        String imageTag = applicationVersion.toLowerCase(Locale.ROOT);
        String imageAPI = Optional.ofNullable(primary.getSpec().getImages().getApi())
                .orElseGet(() -> DEFAULT_IMAGE_API + ":" + imageTag);
        String imageUI = Optional.ofNullable(primary.getSpec().getImages().getUi())
                .orElseGet(() -> DEFAULT_IMAGE_UI + ":" + imageTag);

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
                        .editMatchingContainer(c -> "console-api".equals(c.getName()))
                            .withImage(imageAPI)
                            .addAllToEnv(primary.getSpec().getEnv())
                        .endContainer()
                        .editMatchingContainer(c -> "console-ui".equals(c.getName()))
                            .withImage(imageUI)
                            .editMatchingEnv(env -> "CONSOLE_METRICS_PROMETHEUS_URL".equals(env.getName()))
                                .withValue(getAttribute(context, PrometheusService.NAME + ".url", String.class))
                            .endEnv()
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
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }
}
