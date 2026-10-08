package com.github.streamshub.console.dependents;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.DexConfig;

import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import static com.github.streamshub.console.dependents.support.ConfigSupport.resourceSupported;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class DexDeployment extends BaseDeployment {

    public static final String NAME = "dex-deployment";
    private static final String HTTPS = "HTTPS";

    @Inject
    DexServiceAccount serviceAccount;

    @Inject
    DexSecret secret;

    @Inject
    DexService service;

    @Inject
    @ConfigProperty(name = "console.deployment.dex-image")
    String dexImage;

    public DexDeployment() {
        super(NAME);
    }

    @Override
    protected Deployment desired(Console primary, Context<Console> context) {
        String name = instanceName(primary);

        var edit = load(context, "dex.deployment.yaml", Deployment.class)
            .edit()
            .editMetadata()
                .withName(name)
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("dex"))
            .endMetadata()
            .editSpec()
                .editSelector()
                    .withMatchLabels(Map.of(INSTANCE_LABEL, name))
                .endSelector()
                .editTemplate()
                    .editMetadata()
                        .addToLabels(Map.of(INSTANCE_LABEL, name))
                        .addToAnnotations("streamshub.github.com/dependency-digest",
                                serializeDigest(context, "dex-digest"))
                    .endMetadata()
                    .editSpec()
                        .withServiceAccountName(serviceAccount.instanceName(primary))
                        .editMatchingVolume(v -> "config".equals(v.getName()))
                            .editSecret()
                                .withSecretName(secret.instanceName(primary))
                            .endSecret()
                        .endVolume()
                        .editMatchingContainer(c -> "dex".equals(c.getName()))
                            .withImage(dexImage)
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec();

        /*
         * On OpenShift the service CA issues a serving certificate for the Dex Service.
         * Add the TLS volume/mount and switch the probe scheme to HTTPS so Dex
         * terminates TLS for re-encrypt.
         */
        if (resourceSupported(context, Route.class)) {
            edit = edit
                .editSpec()
                    .editTemplate()
                        .editSpec()
                            .addToVolumes(new VolumeBuilder()
                                .withName("tls")
                                .withNewSecret()
                                    .withSecretName(service.servingCertSecretName(primary))
                                .endSecret()
                                .build())
                            .editMatchingContainer(c -> "dex".equals(c.getName()))
                                .addToVolumeMounts(new VolumeMountBuilder()
                                    .withName("tls")
                                    .withMountPath(DexConfig.DEX_TLS_MOUNT_PATH)
                                    .withReadOnly(true)
                                    .build())
                                .editStartupProbe()
                                    .editHttpGet()
                                        .withScheme(HTTPS)
                                    .endHttpGet()
                                .endStartupProbe()
                                .editLivenessProbe()
                                    .editHttpGet()
                                        .withScheme(HTTPS)
                                    .endHttpGet()
                                .endLivenessProbe()
                                .editReadinessProbe()
                                    .editHttpGet()
                                        .withScheme(HTTPS)
                                    .endHttpGet()
                                .endReadinessProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec();
        }

        return edit.build();
    }
}
