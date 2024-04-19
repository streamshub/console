package com.github.eyefloaters.console;

import com.github.eyefloaters.console.api.v1alpha1.Console;
import com.github.eyefloaters.console.dependents.ConsoleClusterRole;
import com.github.eyefloaters.console.dependents.ConsoleClusterRoleBinding;
import com.github.eyefloaters.console.dependents.ConsoleDeployment;
import com.github.eyefloaters.console.dependents.ConsoleIngress;
import com.github.eyefloaters.console.dependents.ConsoleSecret;
import com.github.eyefloaters.console.dependents.ConsoleService;
import com.github.eyefloaters.console.dependents.ConsoleServiceAccount;
import com.github.eyefloaters.console.dependents.PrometheusClusterRole;
import com.github.eyefloaters.console.dependents.PrometheusClusterRoleBinding;
import com.github.eyefloaters.console.dependents.PrometheusConfigMap;
import com.github.eyefloaters.console.dependents.PrometheusDeployment;
import com.github.eyefloaters.console.dependents.PrometheusService;
import com.github.eyefloaters.console.dependents.PrometheusServiceAccount;

import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

@ControllerConfiguration(dependents = {
        @Dependent(
            name = "prometheus.cluster-role",
            //useEventSourceWithName = "cluster-roles",
            type = PrometheusClusterRole.class),
        @Dependent(
            name = "prometheus.service-account",
            //useEventSourceWithName = "service-accounts",
            type = PrometheusServiceAccount.class),
        @Dependent(
            name = "prometheus.cluster-role-binding",
            //useEventSourceWithName = "cluster-role-bindings",
            type = PrometheusClusterRoleBinding.class,
            dependsOn = {
                "prometheus.cluster-role",
                "prometheus.service-account"
            }),
        @Dependent(
            name = "prometheus.configmap",
            type = PrometheusConfigMap.class),
        @Dependent(
            name = "prometheus.deployment",
            //useEventSourceWithName = "deployments",
            type = PrometheusDeployment.class,
            dependsOn = {
                "prometheus.cluster-role-binding",
                "prometheus.configmap"
            }),
        @Dependent(
            name = "prometheus.service",
            //useEventSourceWithName = "services",
            type = PrometheusService.class,
            dependsOn = {
                "prometheus.deployment"
            }),
        @Dependent(
            name = "console.cluster-role",
            //useEventSourceWithName = "cluster-roles",
            type = ConsoleClusterRole.class),
        @Dependent(
            name = "console.service-account",
            //useEventSourceWithName = "service-accounts",
            type = ConsoleServiceAccount.class),
        @Dependent(
            name = "console.cluster-role-binding",
            //useEventSourceWithName = "cluster-role-bindings",
            type = ConsoleClusterRoleBinding.class,
            dependsOn = {
                "console.cluster-role",
                "console.service-account"
            }),
        @Dependent(
            name = "console.secret",
            type = ConsoleSecret.class),
        @Dependent(
            name = "console.deployment",
            //useEventSourceWithName = "deployments",
            type = ConsoleDeployment.class,
            dependsOn = {
                "console.cluster-role-binding",
                "console.secret"
            }),
        @Dependent(
            name = "console.service",
            //useEventSourceWithName = "services",
            type = ConsoleService.class,
            dependsOn = {
                "console.deployment"
            }),
        @Dependent(
            name = "console.ingress",
            type = ConsoleIngress.class,
            dependsOn = {
                "console.service"
            }),
})
public class ConsoleReconciler implements /* EventSourceInitializer<Console> */ Reconciler<Console>, Cleaner<Console> {

//    @Override
//    public Map<String, EventSource> prepareEventSources(EventSourceContext<Console> context) {
//        // TODO Auto-generated method stub
//        return Collections.emptyMap();
//    }

    @Override
    public UpdateControl<Console> reconcile(Console resource, Context<Console> context) throws Exception {
        return UpdateControl.patchStatus(resource);
    }

    @Override
    public DeleteControl cleanup(Console resource, Context<Console> context) {
        return DeleteControl.defaultDelete();
    }

}
