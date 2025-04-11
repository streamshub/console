package com.github.streamshub.systemtests.utils;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class ResourceUtils {

    private ResourceUtils() {}

    // ------
    // Get
    // ------
    public static <T extends HasMetadata> MixedOperation<T, KubernetesResourceList<T>, Resource<T>> getKubeResourceClient(Class<T> resourceClass) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass);
    }

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

    // --------------------
    // Resources kind cast
    // --------------------
    public static <T> Stream<T> getResourcesStreamFromListOfResources(List<HasMetadata> resources, Class<T> type) {
        return resources.stream()
            .filter(type::isInstance)
            .map(type::cast);
    }

    public static <T> T getResourceFromListOfResources(List<HasMetadata> resources, Class<T> type) {
        return getResourcesStreamFromListOfResources(resources, type)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No resources of type " + type.getSimpleName() + " found."));
    }
}
