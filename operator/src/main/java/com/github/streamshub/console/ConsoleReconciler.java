package com.github.streamshub.console;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;
import com.github.streamshub.console.dependents.ConfigurationProcessor;
import com.github.streamshub.console.dependents.ConsoleClusterRole;
import com.github.streamshub.console.dependents.ConsoleClusterRoleBinding;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleIngress;
import com.github.streamshub.console.dependents.ConsoleMonitoringClusterRoleBinding;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.ConsoleSecret;
import com.github.streamshub.console.dependents.ConsoleService;
import com.github.streamshub.console.dependents.ConsoleServiceAccount;
import com.github.streamshub.console.dependents.PrometheusClusterRole;
import com.github.streamshub.console.dependents.PrometheusClusterRoleBinding;
import com.github.streamshub.console.dependents.PrometheusConfigMap;
import com.github.streamshub.console.dependents.PrometheusDeployment;
import com.github.streamshub.console.dependents.PrometheusService;
import com.github.streamshub.console.dependents.PrometheusServiceAccount;
import com.github.streamshub.console.dependents.conditions.DeploymentReadyCondition;
import com.github.streamshub.console.dependents.conditions.IngressReadyCondition;
import com.github.streamshub.console.dependents.conditions.PrometheusPrecondition;

import io.javaoperatorsdk.operator.AggregatedOperatorException;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.MaxReconciliationInterval;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Annotations;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Annotations.Annotation;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.InstallMode;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Link;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Provider;

@ControllerConfiguration(
        maxReconciliationInterval = @MaxReconciliationInterval(
                interval = 60,
                timeUnit = TimeUnit.SECONDS),
        dependents = {
            @Dependent(
                    name = ConfigurationProcessor.NAME,
                    type = ConfigurationProcessor.class,
                    readyPostcondition = ConfigurationProcessor.Postcondition.class),
            @Dependent(
                    name = PrometheusClusterRole.NAME,
                    type = PrometheusClusterRole.class,
                    dependsOn = ConfigurationProcessor.NAME,
                    reconcilePrecondition = PrometheusPrecondition.class),
            @Dependent(
                    name = PrometheusServiceAccount.NAME,
                    type = PrometheusServiceAccount.class,
                    dependsOn = ConfigurationProcessor.NAME,
                    reconcilePrecondition = PrometheusPrecondition.class),
            @Dependent(
                    name = PrometheusClusterRoleBinding.NAME,
                    type = PrometheusClusterRoleBinding.class,
                    reconcilePrecondition = PrometheusPrecondition.class,
                    dependsOn = {
                        ConfigurationProcessor.NAME,
                        PrometheusClusterRole.NAME,
                        PrometheusServiceAccount.NAME
                    }),
            @Dependent(
                    name = PrometheusConfigMap.NAME,
                    type = PrometheusConfigMap.class,
                    dependsOn = ConfigurationProcessor.NAME,
                    reconcilePrecondition = PrometheusPrecondition.class),
            @Dependent(
                    name = PrometheusDeployment.NAME,
                    type = PrometheusDeployment.class,
                    reconcilePrecondition = PrometheusPrecondition.class,
                    dependsOn = {
                        PrometheusClusterRoleBinding.NAME,
                        PrometheusConfigMap.NAME
                    },
                    readyPostcondition = DeploymentReadyCondition.class),
            @Dependent(
                    name = PrometheusService.NAME,
                    type = PrometheusService.class,
                    reconcilePrecondition = PrometheusPrecondition.class,
                    dependsOn = {
                        PrometheusDeployment.NAME
                    }),
            @Dependent(
                    name = ConsoleClusterRole.NAME,
                    type = ConsoleClusterRole.class,
                    dependsOn = ConfigurationProcessor.NAME),
            @Dependent(
                    name = ConsoleServiceAccount.NAME,
                    type = ConsoleServiceAccount.class,
                    dependsOn = ConfigurationProcessor.NAME),
            @Dependent(
                    name = ConsoleClusterRoleBinding.NAME,
                    type = ConsoleClusterRoleBinding.class,
                    dependsOn = {
                        ConsoleClusterRole.NAME,
                        ConsoleServiceAccount.NAME
                    }),
            @Dependent(
                    name = ConsoleMonitoringClusterRoleBinding.NAME,
                    type = ConsoleMonitoringClusterRoleBinding.class,
                    reconcilePrecondition = ConsoleMonitoringClusterRoleBinding.Precondition.class,
                    dependsOn = {
                        ConsoleServiceAccount.NAME
                    }),
            @Dependent(
                    name = ConsoleSecret.NAME,
                    type = ConsoleSecret.class,
                    dependsOn = ConfigurationProcessor.NAME),
            @Dependent(
                    name = ConsoleService.NAME,
                    type = ConsoleService.class,
                    dependsOn = ConfigurationProcessor.NAME),
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
                        ConsoleIngress.NAME
                    },
                    readyPostcondition = DeploymentReadyCondition.class),
        })
@CSVMetadata(
        provider = @Provider(name = "StreamsHub", url = "https://github.com/streamshub"),
        annotations = @Annotations(
            containerImage = "placeholder",
            repository = "https://github.com/streamshub/console",
            capabilities = "Basic Install",
            categories = "Streaming & Messaging",
            certified = true,
            others = {
                @Annotation(name = "features.operators.openshift.io/fips-compliant", value = "true"),
                @Annotation(name = "features.operators.openshift.io/disconnected", value = "true"),
                @Annotation(name = "features.operators.openshift.io/proxy-aware", value = "true"),
                @Annotation(name = "createdAt", value = "placeholder"),
                @Annotation(name = "support", value = "Streamshub")
            }),
        description = """
            The Streamshub Console provides a web-based user interface tool for monitoring Apache Kafka® instances within a Kubernetes based cluster.

            It features a user-friendly way to view Kafka topics and consumer groups, facilitating the searching and filtering of streamed messages. The console also offers insights into Kafka broker disk usage, helping administrators monitor and optimize resource utilization. By simplifying complex Kafka operations, the Streamshub Console enhances the efficiency and effectiveness of data streaming management within Kubernetes environments.

            ### Documentation
            Documentation to the current _main_ branch as well as all releases can be found on our [Github](https://github.com/streamshub/console).

            ### Contributing
            You can contribute to Console by:
            * Raising any issues you find while using Console
            * Fixing issues by opening Pull Requests
            * Improving user documentation
            * Talking about Console

            The [Contributor Guide](https://github.com/streamshub/console/blob/main/CONTRIBUTING.md) describes how to contribute to Console.

            ### License
            Console is licensed under the [Apache License, Version 2.0](https://github.com/streamshub/console?tab=Apache-2.0-1-ov-file#readme).
            For more details, visit the GitHub repository.""",
        displayName = "StreamsHub Console Operator",
        keywords = {"kafka", "messaging", "kafka-streams", "data-streaming", "data-streams", "streaming", "streams", "web", "console", "ui", "user interface"},
        maturity = "stable",
        installModes = {
            @InstallMode(type = "AllNamespaces", supported = true),
            @InstallMode(type = "OwnNamespace", supported = false),
            @InstallMode(type = "SingleNamespace", supported = false),
            @InstallMode(type = "MultiNamespace", supported = false),
        },
        minKubeVersion = "1.25.0", // Corresponds to OpenShift 4.12
        links = {
            @Link(name = "GitHub", url = "https://github.com/streamshub/console"),
            @Link(name = "Documentation", url = "https://github.com/streamshub/console/blob/main/README.md")
        })
public class ConsoleReconciler
    implements EventSourceInitializer<Console>, Reconciler<Console>, Cleaner<Console>, ErrorStatusHandler<Console> {

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Console> context) {
        return Collections.emptyMap();
    }

    @Override
    public UpdateControl<Console> reconcile(Console resource, Context<Console> context) {
        determineReadyCondition(resource, context);
        resource.getStatus().clearStaleConditions();
        return UpdateControl.updateStatus(resource);
    }

    @Override
    public ErrorStatusUpdateControl<Console> updateErrorStatus(Console resource,
            Context<Console> context,
            Exception e) {

        determineReadyCondition(resource, context);

        Throwable rootCause = e;

        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        String message;

        if (rootCause instanceof AggregatedOperatorException aggregated) {
            message = aggregated.getAggregatedExceptions().values().stream()
                .map(Exception::getMessage)
                .collect(Collectors.joining("; "));
        } else {
            message = rootCause.getMessage();
        }

        var status = resource.getStatus();

        status.updateCondition(new ConditionBuilder()
                .withType(Condition.Types.ERROR)
                .withStatus("True")
                .withLastTransitionTime(Instant.now().toString())
                .withReason(Condition.Reasons.RECONCILIATION_EXCEPTION)
                .withMessage(message)
                .build());

        status.clearStaleConditions();

        return ErrorStatusUpdateControl.updateStatus(resource);
    }

    @Override
    public DeleteControl cleanup(Console resource, Context<Console> context) {
        return DeleteControl.defaultDelete();
    }

    private void determineReadyCondition(Console resource, Context<Console> context) {
        var result = context.managedDependentResourceContext().getWorkflowReconcileResult();
        var status = resource.getOrCreateStatus();
        var readyCondition = status.getCondition("Ready");
        var notReady = result.map(r -> r.getNotReadyDependents()).filter(Predicate.not(Collection::isEmpty));
        boolean isReady = notReady.isEmpty();

        String readyStatus = isReady ? "True" : "False";
        readyCondition.setActive(true);

        if (!readyStatus.equals(readyCondition.getStatus())) {
            readyCondition.setStatus(readyStatus);
            readyCondition.setLastTransitionTime(Instant.now().toString());
        }

        if (isReady) {
            readyCondition.setReason(null);
            readyCondition.setMessage("All resources ready");
        } else {
            var notReadyResources = notReady.get();

            if (notReadyResources.stream().anyMatch(ConfigurationProcessor.class::isInstance)) {
                readyCondition.setReason(Condition.Reasons.INVALID_CONFIGURATION);
                readyCondition.setMessage("Console resource configuration is invalid");
            } else {
                readyCondition.setReason(Condition.Reasons.DEPENDENTS_NOT_READY);
                readyCondition.setMessage("Resources not ready: %s"
                        .formatted(notReadyResources.stream()
                                .map(ConsoleResource.class::cast)
                                .map(r -> "%s[%s]".formatted(r.getClass().getSimpleName(), r.instanceName(resource)))
                                .collect(Collectors.joining("; "))));
            }
        }
    }
}
