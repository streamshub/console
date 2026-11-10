package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.conditions.ServingCertReadyCondition;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import static com.github.streamshub.console.dependents.support.ConfigSupport.resourceSupported;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class DexService extends BaseService {

    public static final String NAME = "dex-service";

    @Inject
    DexDeployment deployment;

    public DexService() {
        super("dex", "dex.service.yaml", NAME);
    }

    @Override
    protected String appName(Console primary) {
        return deployment.instanceName(primary);
    }

    @Override
    protected Service desired(Console primary, Context<Console> context) {
        var builder = super.desired(primary, context).edit();

        if (resourceSupported(context, Route.class)) {
            builder = builder
                .editMetadata()
                    .addToAnnotations(
                        ServingCertReadyCondition.ANNOTATION,
                        servingCertSecretName(primary))
                .endMetadata();
        }

        return builder.build();
    }
}
