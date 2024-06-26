package com.github.streamshub.console;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;
import com.github.streamshub.console.dependents.ConsoleClusterRole;
import com.github.streamshub.console.dependents.ConsoleClusterRoleBinding;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleIngress;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.ConsoleSecret;
import com.github.streamshub.console.dependents.ConsoleService;
import com.github.streamshub.console.dependents.ConsoleServiceAccount;
import com.github.streamshub.console.dependents.DeploymentReadyCondition;
import com.github.streamshub.console.dependents.IngressReadyCondition;
import com.github.streamshub.console.dependents.PrometheusClusterRole;
import com.github.streamshub.console.dependents.PrometheusClusterRoleBinding;
import com.github.streamshub.console.dependents.PrometheusConfigMap;
import com.github.streamshub.console.dependents.PrometheusDeployment;
import com.github.streamshub.console.dependents.PrometheusService;
import com.github.streamshub.console.dependents.PrometheusServiceAccount;

import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Provider;

@ControllerConfiguration(dependents = {
        @Dependent(
                name = PrometheusClusterRole.NAME,
                type = PrometheusClusterRole.class),
        @Dependent(
                name = PrometheusServiceAccount.NAME,
                type = PrometheusServiceAccount.class),
        @Dependent(
                name = PrometheusClusterRoleBinding.NAME,
                type = PrometheusClusterRoleBinding.class,
                dependsOn = {
                        PrometheusClusterRole.NAME,
                        PrometheusServiceAccount.NAME
                }),
        @Dependent(
                name = PrometheusConfigMap.NAME,
                type = PrometheusConfigMap.class),
        @Dependent(
                name = PrometheusDeployment.NAME,
                type = PrometheusDeployment.class,
                dependsOn = {
                        PrometheusClusterRoleBinding.NAME,
                        PrometheusConfigMap.NAME
                },
                readyPostcondition = DeploymentReadyCondition.class),
        @Dependent(
                name = PrometheusService.NAME,
                type = PrometheusService.class,
                dependsOn = {
                        PrometheusDeployment.NAME
                }),
        @Dependent(
                name = ConsoleClusterRole.NAME,
                type = ConsoleClusterRole.class),
        @Dependent(
                name = ConsoleServiceAccount.NAME,
                type = ConsoleServiceAccount.class),
        @Dependent(
                name = ConsoleClusterRoleBinding.NAME,
                type = ConsoleClusterRoleBinding.class,
                dependsOn = {
                        ConsoleClusterRole.NAME,
                        ConsoleServiceAccount.NAME
                }),
        @Dependent(
                name = ConsoleSecret.NAME,
                type = ConsoleSecret.class),
        @Dependent(
                name = ConsoleService.NAME,
                type = ConsoleService.class),
        @Dependent(
                name = ConsoleIngress.NAME,
                type = ConsoleIngress.class,
                dependsOn = {
                        ConsoleService.NAME
                },
                readyPostcondition = IngressReadyCondition.class),
        @Dependent(
                name = ConsoleDeployment.NAME,
                type = ConsoleDeployment.class,
                dependsOn = {
                        ConsoleClusterRoleBinding.NAME,
                        ConsoleSecret.NAME,
                        ConsoleIngress.NAME,
                        PrometheusService.NAME
                },
                readyPostcondition = DeploymentReadyCondition.class),
})
@CSVMetadata(
        provider = @Provider(name = "StreamsHub", url = "https://github.com/streamshub"),
        description = "StreamsHub console provides a user interface for managing and monitoring your streaming resources")
public class ConsoleReconciler implements EventSourceInitializer<Console>, Reconciler<Console>, Cleaner<Console> {

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Console> context) {
        return Collections.emptyMap();
    }

    @Override
    public UpdateControl<Console> reconcile(Console resource, Context<Console> context) {
        var result = context.managedDependentResourceContext().getWorkflowReconcileResult();
        var status = resource.getOrCreateStatus();

        var readyCondition = Optional.ofNullable(status.getConditions())
                .flatMap(conditions -> conditions.stream()
                        .filter(c -> "Ready".equals(c.getType()))
                        .findFirst())
                .orElseGet(() -> {
                    var condition = new ConditionBuilder()
                            .withType("Ready")
                            .withLastTransitionTime(Instant.now().toString())
                            .build();
                    status.getConditions().add(condition);
                    return condition;
                });

        var notReady = result.map(r -> r.getNotReadyDependents());
        boolean isReady = notReady.filter(Collection::isEmpty).map(r -> Boolean.TRUE)
                .orElse(Boolean.FALSE);

        String readyStatus = isReady ? "True" : "False";

        if (!readyStatus.equals(readyCondition.getStatus())) {
            readyCondition.setStatus(readyStatus);
            readyCondition.setLastTransitionTime(Instant.now().toString());
        }

        if (isReady) {
            readyCondition.setReason(null);
            readyCondition.setMessage("All resources ready");
        } else {
            readyCondition.setReason("DependentsNotReady");
            readyCondition.setMessage(notReady.map(Collection::stream)
                    .map(deps -> "Resources not ready: %s"
                        .formatted(deps.map(ConsoleResource.class::cast)
                                .map(r -> "%s[%s]".formatted(r.getClass().getSimpleName(), r.instanceName(resource)))
                                .collect(Collectors.joining("; "))))
                    .orElse(""));
        }

        return UpdateControl.patchStatus(resource);
    }

    @Override
    public DeleteControl cleanup(Console resource, Context<Console> context) {
        return DeleteControl.defaultDelete();
    }

}
