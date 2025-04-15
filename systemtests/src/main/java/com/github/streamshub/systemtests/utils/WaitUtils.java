package com.github.streamshub.systemtests.utils;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.wait.Wait;

import static com.github.streamshub.systemtests.utils.ResourceUtils.listKubeResourcesByPrefix;

public class WaitUtils {

    private WaitUtils() {}

    // ------------
    // Deployment
    // ------------
    public static void waitForDeploymentWithPrefixIsReady(String namespaceName, String deploymentNamePrefix) {
        Wait.until(String.format("creation of Deployment with prefix: %s in Namespace: %s", deploymentNamePrefix, namespaceName),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Deployment dep = listKubeResourcesByPrefix(Deployment.class, namespaceName, deploymentNamePrefix).get(0);
                if (dep == null) {
                    return false;
                }
                return Readiness.isDeploymentReady(dep);
            });
    }

    public static void waitForSecretReady(String namespace, String kafkaUserName) {
        Wait.until(String.format("creation of Secret %s/%s", namespace, kafkaUserName),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Secret secret = ResourceUtils.getKubeResource(Secret.class, namespace, kafkaUserName);
                return secret != null && secret.getData() != null && !secret.getData().isEmpty();
            });
    }
}
