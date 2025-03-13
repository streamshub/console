package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DeploymentUtils {

    private static final Logger LOGGER = LogManager.getLogger(DeploymentUtils.class);


    // -------------
    // Client
    // -------------
    private static MixedOperation<Deployment, DeploymentList, Resource<Deployment>> deploymentClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(Deployment.class, DeploymentList.class);
    }

    // -------------
    // Get
    // -------------
    public static Deployment getDeployment(String namespace, String name) {
        return deploymentClient().inNamespace(namespace).withName(name).get();
    }

    public static Deployment getDeploymentByPrefix(String namespace, String prefix) {
        List<Deployment> dep = deploymentClient().inNamespace(namespace).list().getItems().stream().filter(d -> d.getMetadata().getName().startsWith(prefix)).toList();
        return  dep.isEmpty() ? null : dep.get(0);
    }

    // -------------
    // List
    // -------------
    public static DeploymentList listDeployments(String namespace) {
        return deploymentClient().inNamespace(namespace).list();
    }

    // -------------
    // Wait
    // -------------
    public static void waitForDeploymentBySuffixReady(String namespaceName, String suffix) {
        LOGGER.info("Waiting for Deployment: {}/*-{} to be ready", namespaceName, suffix);

        CommonUtils.waitFor("readiness of Deployment: " + namespaceName + "/*-" + suffix,
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.READINESS_TIMEOUT,
            () ->  {
                Deployment deployment = listDeployments(namespaceName).getItems().stream().filter(
                    dep -> dep.getMetadata().getName().endsWith(suffix)).toList().get(0);

                if (deployment != null) {
                    String deploymentName = deployment.getMetadata().getName();
                    return deploymentName != null && isDeploymentReady(namespaceName, deploymentName);
                }
                return false;
            });

        LOGGER.info("Deployment: {}/*-{} is ready", namespaceName, suffix);
    }

    public static void waitForDeploymentReady(String namespaceName, String deploymentName) {
        LOGGER.info("Waiting for Deployment: {}/{} to be ready", namespaceName, deploymentName);

        CommonUtils.waitFor("readiness of Deployment: " + namespaceName + "/" + deploymentName,
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.READINESS_TIMEOUT,
            () -> isDeploymentReady(namespaceName, deploymentName));

        LOGGER.info("Deployment: {}/{} is ready", namespaceName, deploymentName);
    }

    public static void waitForDeploymentByPrefixReady(String namespaceName, String prefix) {
        LOGGER.info("Waiting for Deployment: {}/{}-* to be ready", namespaceName, prefix);

        CommonUtils.waitFor("readiness of Deployment: " + namespaceName + "/" + prefix + "-*",
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.READINESS_TIMEOUT,
            () ->  {
                Deployment deployment = getDeploymentByPrefix(namespaceName, prefix);
                if (deployment != null) {
                    String deploymentName = deployment.getMetadata().getName();
                    return deploymentName != null && isDeploymentReady(namespaceName, deploymentName);
                }
                return false;
            });

        LOGGER.info("Deployment: {}/{}-* is ready", namespaceName, prefix);
    }

    // -------------
    // Update
    // -------------
    public static void updateDeployment(Deployment resource) {
        deploymentClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    // -------------
    // StatusReady
    // -------------
    public static boolean isDeploymentReady(String namespaceName, String deploymentName) {
        return deploymentClient().inNamespace(namespaceName).withName(deploymentName).isReady();
    }
}
