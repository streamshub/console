package com.github.streamshub.systemtests.utils;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.testframe.resources.KubeResourceManager;

public class ResourceUtils {

    private ResourceUtils() {}

    // Vanilla k8s
    public static <T extends HasMetadata> T getKubeResource(Class<T> resourceClass, String namespaceName, String resourceName) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass).inNamespace(namespaceName).withName(resourceName).get();
    }

    public static <T extends HasMetadata> T getKubeResource(Class<T> resourceClass, String resourceName) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass).withName(resourceName).get();

    }
}
