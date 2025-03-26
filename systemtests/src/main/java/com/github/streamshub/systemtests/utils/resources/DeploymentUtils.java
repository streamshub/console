package com.github.streamshub.systemtests.utils.resources;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;

public class DeploymentUtils {
    private DeploymentUtils() {}

    public static Deployment getDeployment(String namespace, String name) {
        return KubeResourceManager.getKubeClient().getClient().apps().deployments().inNamespace(namespace).withName(name).get();
    }
}
