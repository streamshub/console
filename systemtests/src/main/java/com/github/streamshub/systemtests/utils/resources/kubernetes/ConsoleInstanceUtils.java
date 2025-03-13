package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsoleInstanceUtils {

    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceUtils.class);

    // -------------
    // Client
    // -------------
    public static MixedOperation<Console, KubernetesResourceList<Console>, Resource<Console>> consoleInstanceClient() {
        if (CustomResourceDefinitionUtils.getCrdsByContains(ResourceKinds.CONSOLE_INSTANCE).isEmpty()) {
            LOGGER.warn("Console CRD is not deployed, cannot work with kind: Console");
            return null;
        }
        return KubeResourceManager.getKubeClient().getClient().resources(Console.class);
    }
    
    // -------------
    // Get
    // -------------
    public static Console getConsole(String namespace, String name) {
        if (CustomResourceDefinitionUtils.getCrdsByContains(ResourceKinds.CONSOLE_INSTANCE).isEmpty()) {
            return null;
        }
        return consoleInstanceClient().inNamespace(namespace).withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<Console> listConsoles(String namespace) {
        if (CustomResourceDefinitionUtils.getCrdsByContains(ResourceKinds.CONSOLE_INSTANCE).isEmpty()) {
            return new ArrayList<>();
        }
        return consoleInstanceClient().inNamespace(namespace).list().getItems();
    }

    public static List<Console> listAllConsoles() {
        if (CustomResourceDefinitionUtils.getCrdsByContains(ResourceKinds.CONSOLE_INSTANCE).isEmpty()) {
            return new ArrayList<>();
        }
        return consoleInstanceClient().inAnyNamespace().list().getItems();
    }

    // -------------
    // Wait
    // -------------
    public static void waitForConsoleReady(String namespace) {
        PodUtils.waitForPodsReady(namespace, getConsolePodSelector(namespace), 1, true);
    }

    // -------------
    // Update
    // -------------
    public static void updateResource(Console resource) {
        LOGGER.info("Updating Console {}/{}", resource.getMetadata().getNamespace(), resource.getMetadata().getName());
        consoleInstanceClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    // -------------
    // Label selector
    // -------------
    public static LabelSelector getConsolePodSelector(String namespace) {
        return new LabelSelectorBuilder()
            .withMatchLabels(Map.of("app.kubernetes.io/instance", getConsoleInstanceDeploymentName(namespace)))
            .build();
    }

    // -------------
    // Naming
    // -------------
    public static String getConsoleInstanceName(String namespace) {
        return Constants.CONSOLE_INSTANCE + "-" + CommonUtils.hashStub(namespace);
    }

    public static String getConsoleInstanceDeploymentName(String namespace) {
        return getConsoleInstanceName(namespace) + "-" + Constants.CONSOLE_DEPLOYMENT;
    }

    public static String getPrometheusInstanceDeploymentName(String namespace) {
        return getConsoleInstanceName(namespace) + "-" + Constants.PROMETHEUS_DEPLOYMENT;
    }
    
    
}