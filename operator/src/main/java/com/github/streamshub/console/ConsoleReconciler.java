package com.github.streamshub.console;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.ConsoleClusterRole;
import com.github.streamshub.console.dependents.ConsoleClusterRoleBinding;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleIngress;
import com.github.streamshub.console.dependents.ConsoleMonitoringClusterRoleBinding;
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
import com.github.streamshub.console.dependents.PrometheusPrecondition;
import com.github.streamshub.console.dependents.PrometheusService;
import com.github.streamshub.console.dependents.PrometheusServiceAccount;

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
                    name = PrometheusClusterRole.NAME,
                    type = PrometheusClusterRole.class,
                    reconcilePrecondition = PrometheusPrecondition.class),
            @Dependent(
                    name = PrometheusServiceAccount.NAME,
                    type = PrometheusServiceAccount.class,
                    reconcilePrecondition = PrometheusPrecondition.class),
            @Dependent(
                    name = PrometheusClusterRoleBinding.NAME,
                    type = PrometheusClusterRoleBinding.class,
                    reconcilePrecondition = PrometheusPrecondition.class,
                    dependsOn = {
                        PrometheusClusterRole.NAME,
                        PrometheusServiceAccount.NAME
                    }),
            @Dependent(
                    name = PrometheusConfigMap.NAME,
                    type = PrometheusConfigMap.class,
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
                    name = ConsoleMonitoringClusterRoleBinding.NAME,
                    type = ConsoleMonitoringClusterRoleBinding.class,
                    reconcilePrecondition = ConsoleMonitoringClusterRoleBinding.Precondition.class,
                    dependsOn = {
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
            skipRange = "placeholder",
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
            @InstallMode(type = "OwnNamespace", supported = true),
            @InstallMode(type = "SingleNamespace", supported = true),
            @InstallMode(type = "MultiNamespace", supported = false),
        },
        minKubeVersion = "1.19.0",
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
        return UpdateControl.patchStatus(resource);
    }

    @Override
    public ErrorStatusUpdateControl<Console> updateErrorStatus(Console resource,
            Context<Console> context,
            Exception e) {

        determineReadyCondition(resource, context);

        var status = resource.getOrCreateStatus();
        var warning = status.getCondition("Warning");
        warning.setStatus("True");
        warning.setReason("ReconcileException");

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

        warning.setMessage(message);
        return ErrorStatusUpdateControl.patchStatus(resource);
    }

    @Override
    public DeleteControl cleanup(Console resource, Context<Console> context) {
        return DeleteControl.defaultDelete();
    }

    private void determineReadyCondition(Console resource, Context<Console> context) {
        var result = context.managedDependentResourceContext().getWorkflowReconcileResult();
        var status = resource.getOrCreateStatus();
        var readyCondition = status.getCondition("Ready");
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
            status.clearCondition("Warning");
        } else {
            readyCondition.setReason("DependentsNotReady");
            readyCondition.setMessage(notReady.map(Collection::stream)
                    .map(deps -> "Resources not ready: %s"
                        .formatted(deps.map(ConsoleResource.class::cast)
                                .map(r -> "%s[%s]".formatted(r.getClass().getSimpleName(), r.instanceName(resource)))
                                .collect(Collectors.joining("; "))))
                    .orElse(""));
        }
    }
}
