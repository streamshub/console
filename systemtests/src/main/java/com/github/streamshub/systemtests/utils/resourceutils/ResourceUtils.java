package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ResourceUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ResourceUtils.class);

    private ResourceUtils() {}

    // ------
    // Get
    // ------
    public static <T extends HasMetadata> MixedOperation<T, KubernetesResourceList<T>, Resource<T>> getKubeResourceClient(Class<T> resourceClass) {
        return KubeResourceManager.get().kubeClient().getClient().resources(resourceClass);
    }

    public static <T extends HasMetadata> T getKubeResource(Class<T> resourceClass, String namespaceName, String resourceName) {
        LOGGER.debug("Fetching {} {}/{}", resourceClass.getSimpleName(), namespaceName, resourceName);
        return getKubeResourceClient(resourceClass).inNamespace(namespaceName).withName(resourceName).get();
    }

    public static <T extends HasMetadata> T getKubeResource(Class<T> resourceClass, String resourceName) {
        LOGGER.debug("Fetching cluster-scoped {} {}", resourceClass.getSimpleName(), resourceName);
        return getKubeResourceClient(resourceClass).withName(resourceName).get();
    }

    // ------
    // List
    // ------
    public static <T extends HasMetadata> List<T> listKubeResources(Class<T> resourceClass, String namespaceName) {
        LOGGER.debug("Listing {} resources in namespace {}", resourceClass.getSimpleName(), namespaceName);
        return getKubeResourceClient(resourceClass).inNamespace(namespaceName).list().getItems();
    }

    public static <T extends HasMetadata> List<T> listKubeResourcesByPrefix(Class<T> resourceClass, String namespaceName, String prefix) {
        LOGGER.debug("Listing {} resources in namespace {} with name prefix '{}'", resourceClass.getSimpleName(), namespaceName, prefix);
        return listKubeResources(resourceClass, namespaceName).stream().filter(it -> it.getMetadata().getName().startsWith(prefix)).toList();
    }

    public static <T extends HasMetadata> List<T> listKubeResourcesByLabelSelector(Class<T> resourceClass, String namespaceName, LabelSelector labelSelector) {
        LOGGER.debug("Listing {} resources in namespace {} with label selector {}", resourceClass.getSimpleName(), namespaceName, labelSelector);
        return getKubeResourceClient(resourceClass).inNamespace(namespaceName).withLabelSelector(labelSelector).list().getItems();
    }
}
