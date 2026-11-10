package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.conditions.DexPrecondition;
import com.github.streamshub.console.dependents.conditions.OpenShiftPrecondition;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class DexRoute extends BaseRoute {

    public static final String NAME = "dex-route";

    @Inject
    DexService dexService;

    public DexRoute() {
        super(NAME);
    }

    @Override
    protected Route desired(Console primary, Context<Console> context) {
        // Read the hostname that OpenShift assigned to the ConsoleRoute. By the time
        // DexRoute runs, the hostname is known from the admitted Route status.
        String host = getAttribute(context, "host", String.class);

        return new RouteBuilder()
            .withNewMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("dex"))
            .endMetadata()
            .withNewSpec()
                .withHost(host)
                .withPath("/dex")
                .withNewTo()
                    .withKind("Service")
                    .withName(dexService.instanceName(primary))
                    .withWeight(100)
                .endTo()
                .withTls(buildTlsConfig(primary, context))
            .endSpec()
            .build();
    }

    /**
     * Active only on OpenShift (Route API supported) when Dex is configured.
     * Combines {@link OpenShiftPrecondition} and {@link DexPrecondition}.
     * Not a CDI bean — conditions are instantiated by the operator SDK.
     */
    public static class Precondition implements Condition<Route, Console> {
        private static final OpenShiftPrecondition<Route> OPENSHIFT = new OpenShiftPrecondition<>();
        private static final DexPrecondition<Route> DEX = new DexPrecondition<>();

        @Override
        public boolean isMet(DependentResource<Route, Console> dependentResource, Console primary, Context<Console> context) {
            return OPENSHIFT.isMet(dependentResource, primary, context)
                    && DEX.isMet(dependentResource, primary, context);
        }
    }
}
