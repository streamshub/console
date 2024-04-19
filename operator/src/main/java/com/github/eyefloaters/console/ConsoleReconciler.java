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
                name = PrometheusClusterRole.NAME,
                //useEventSourceWithName = "cluster-roles",
                type = PrometheusClusterRole.class),
        @Dependent(
                name = PrometheusServiceAccount.NAME,
                //useEventSourceWithName = "service-accounts",
                type = PrometheusServiceAccount.class),
        @Dependent(
                name = PrometheusClusterRoleBinding.NAME,
                //useEventSourceWithName = "cluster-role-bindings",
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
                //useEventSourceWithName = "deployments",
                type = PrometheusDeployment.class,
                dependsOn = {
                        PrometheusClusterRoleBinding.NAME,
                        PrometheusConfigMap.NAME
                }),
        @Dependent(
                name = PrometheusService.NAME,
                //useEventSourceWithName = "services",
                type = PrometheusService.class,
                dependsOn = {
                        PrometheusDeployment.NAME
                }),
        @Dependent(
                name = ConsoleClusterRole.NAME,
                //useEventSourceWithName = "cluster-roles",
                type = ConsoleClusterRole.class),
        @Dependent(
                name = ConsoleServiceAccount.NAME,
                //useEventSourceWithName = "service-accounts",
                type = ConsoleServiceAccount.class),
        @Dependent(
                name = ConsoleClusterRoleBinding.NAME,
                //useEventSourceWithName = "cluster-role-bindings",
                type = ConsoleClusterRoleBinding.class,
                dependsOn = {
                        ConsoleClusterRole.NAME,
                        ConsoleServiceAccount.NAME
                }),
        @Dependent(
                name = ConsoleSecret.NAME,
                type = ConsoleSecret.class),
        @Dependent(
                name = ConsoleIngress.NAME,
                type = ConsoleIngress.class),
        @Dependent(
                name = ConsoleDeployment.NAME,
                //useEventSourceWithName = "deployments",
                type = ConsoleDeployment.class,
                dependsOn = {
                        ConsoleClusterRoleBinding.NAME,
                        ConsoleSecret.NAME,
                        ConsoleIngress.NAME,
                        PrometheusService.NAME
                }),
        @Dependent(
                name = ConsoleService.NAME,
                //useEventSourceWithName = "services",
                type = ConsoleService.class,
                dependsOn = {
                        ConsoleDeployment.NAME
                }),
})
public class ConsoleReconciler implements /* EventSourceInitializer<Console> */ Reconciler<Console>, Cleaner<Console> {

//    @Override
//    public Map<String, EventSource> prepareEventSources(EventSourceContext<Console> context) {
//        return Collections.emptyMap();
//    }

    @Override
    public UpdateControl<Console> reconcile(Console resource, Context<Console> context) throws Exception {
        resource.getOrCreateStatus();
        return UpdateControl.patchStatus(resource);
    }

    @Override
    public DeleteControl cleanup(Console resource, Context<Console> context) {
        return DeleteControl.defaultDelete();
    }

}
