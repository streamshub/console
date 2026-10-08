package com.github.streamshub.console.dependents;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Reasons;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Types;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;
import com.github.streamshub.console.support.DisplayName;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteIngress;
import io.fabric8.openshift.api.model.RouteStatus;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.NameSetter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import static com.github.streamshub.console.dependents.support.ConfigSupport.resourceSupported;

@ApplicationScoped
public class HostnameAvailableGate implements DependentResource<HasMetadata, Console>, NameSetter, ConsoleResource<HasMetadata> {

    public static final String NAME = "HostnameAvailableGate"; // NOSONAR

    @Inject
    Logger logger;

    @Inject
    ConsoleRoute consoleRoute;

    private String instanceName;

    public static class Postcondition implements Condition<HasMetadata, Console> {
        @Override
        public boolean isMet(DependentResource<HasMetadata, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            return context.managedWorkflowAndDependentResourceContext()
                    .get("host", String.class)
                    .isPresent();
        }
    }

    @Override
    public ReconcileResult<HasMetadata> reconcile(Console primary, Context<Console> context) {
        final String host;
        final String specHost = Optional.ofNullable(primary.getSpec().getHostname())
                .filter(Predicate.not(String::isBlank))
                .orElse(null);

        if (specHost != null) {
            host = specHost;

            if (logger.isDebugEnabled()) {
                logger.debugf("Console %s host name is available from spec: %s", DisplayName.of(primary), host);
            }
        } else if (resourceSupported(context, Route.class)) {
            // Read the hostname that OpenShift assigned to the ConsoleRoute. By the time
            // DexRoute runs (dependsOn ConsoleRoute via readyPostcondition), the hostname
            // is known from the admitted Route status.
            host = consoleRoute.getSecondaryResource(primary, context)
                    .flatMap(r -> Optional.ofNullable(r.getStatus())
                            .map(RouteStatus::getIngress)
                            .orElse(Collections.emptyList())
                            .stream()
                            .map(RouteIngress::getHost)
                            .filter(Objects::nonNull)
                            .filter(Predicate.not(String::isBlank))
                            .findFirst())
                    .map(assignedHost -> {
                        if (logger.isDebugEnabled()) {
                            logger.debugf("Console %s host name is available Route status: %s", DisplayName.of(primary), assignedHost);
                        }
                        return assignedHost;
                    })
                    .orElseGet(() -> {
                        if (logger.isDebugEnabled()) {
                            logger.debugf("Console %s host name is not yet available from Route status", DisplayName.of(primary));
                        }
                        return null;
                    });
        } else {
            host = null;
            primary.getOrCreateStatus()
                    .updateCondition(new ConditionBuilder()
                        .withType(Types.ERROR)
                        .withStatus("True")
                        .withLastTransitionTime(Instant.now().toString())
                        .withReason(Reasons.INVALID_CONFIGURATION)
                        .withMessage("spec.hostname is required for Ingress")
                        .build());
        }

        context.managedWorkflowAndDependentResourceContext().put("host", host);
        return ReconcileResult.noOperation(primary);
    }

    @Override
    public String name() {
        return instanceName;
    }

    @Override
    public void setName(String name) {
        this.instanceName = name;
    }

    @Override
    public Class<HasMetadata> resourceType() {
        return HasMetadata.class;
    }

    @Override
    public String resourceName() {
        return NAME;
    }

}