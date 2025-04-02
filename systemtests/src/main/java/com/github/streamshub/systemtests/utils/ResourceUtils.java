package com.github.streamshub.systemtests.utils;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.List;
import java.util.function.Consumer;

public class ResourceUtils {

    private ResourceUtils() {}

    // ------
    // Get
    // ------
    public static <T extends HasMetadata> T getKubeResource(Class<T> resourceClass, String namespaceName, String resourceName) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass).inNamespace(namespaceName).withName(resourceName).get();
    }

    public static <T extends HasMetadata> T getKubeResource(Class<T> resourceClass, String resourceName) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass).withName(resourceName).get();
    }

    // ------
    // List
    // ------
    public static <T extends HasMetadata> List<T> listKubeResources(Class<T> resourceClass, String namespaceName) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass).inNamespace(namespaceName).list().getItems();
    }

    public static <T extends HasMetadata> List<T> listKubeResourcesByPrefix(Class<T> resourceClass, String namespaceName, String prefix) {
        return listKubeResources(resourceClass, namespaceName).stream().filter(it -> it.getMetadata().getName().startsWith(prefix)).toList();
    }

    public static <T extends HasMetadata> List<T> listKubeResourceInAllNamespaces(Class<T> resourceClass) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass).inAnyNamespace().list().getItems();
    }

    // ------
    // Replace
    // ------
    public static <T extends CustomResource> void replaceCustomResource(Class<T> resourceClass, String namespaceName, String resourceName, Consumer<T> consumer) {
        T toBeReplaced = getKubeResource(resourceClass, namespaceName, resourceName);
        consumer.accept(toBeReplaced);
        KubeResourceManager.get().updateResource(toBeReplaced);
    }
}
