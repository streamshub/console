package com.github.streamshub.systemtests.utils;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.wait.Wait;

import static com.github.streamshub.systemtests.utils.ResourceUtils.listKubeResourcesByPrefix;

public class WaitUtils {

    // ------------
    // Deployment
    // ------------
    public static void waitForDeploymentWithPrefix(String namespaceName, String deploymentNamePrefix) {
        Wait.until(String.format("creation of Deployment with prefix: %s in Namespace: %s", deploymentNamePrefix, namespaceName),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> !listKubeResourcesByPrefix(Deployment.class, namespaceName, deploymentNamePrefix).isEmpty());
    }

    // ------------
    // ConsoleOperator
    // ------------
    // public static void waitForConsoleOperator(String namespace, String name) {
    //     DeploymentResource.waitForDeploymentByPrefixReady(namespace, name);
    //     String consoleOperatorDeploymentFullName = DeploymentResource.getDeploymentByPrefix(namespace, name).getMetadata().getName();
    //     PodResource.waitForPodsReady(namespace, TestUtils.getConsoleOperatorPodLabelSelector(consoleOperatorDeploymentFullName), 1, false);
    // }
}
