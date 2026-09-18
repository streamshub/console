package com.github.streamshub.systemtests.utils.resourceutils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

import com.github.streamshub.systemtests.logs.LogWrapper;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.kubetest4j.resources.KubeResourceManager;

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
        return getKubeResourceClient(resourceClass).inNamespace(namespaceName).withName(resourceName).get();
    }

    public static <T extends HasMetadata> T editKubeResource(Class<T> resourceClass, String namespaceName, String resourceName, UnaryOperator<T> editor) {
        LOGGER.trace("Editing {} {}/{}", resourceClass.getSimpleName(), namespaceName, resourceName);
        var client = getKubeResourceClient(resourceClass).inNamespace(namespaceName).withName(resourceName);

        return Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .ignoreExceptionsMatching(thrown -> {
                    if (thrown instanceof KubernetesClientException kce) {
                        return Optional.ofNullable(kce.getStatus())
                            .map(Status::getCode)
                            .map(Integer.valueOf(409)::equals)
                            .orElse(false);
                    }

                    return false;
                })
                .until(
                    () -> client.edit(editor),
                    Objects::nonNull);
    }

    public static <T extends HasMetadata> T getKubeResource(Class<T> resourceClass, String resourceName) {
        return getKubeResourceClient(resourceClass).withName(resourceName).get();
    }

    // ------
    // List
    // ------
    public static <T extends HasMetadata> List<T> listKubeResources(Class<T> resourceClass, String namespaceName) {
        return getKubeResourceClient(resourceClass).inNamespace(namespaceName).list().getItems();
    }

    public static <T extends HasMetadata> List<T> listKubeResourcesByPrefix(Class<T> resourceClass, String namespaceName, String prefix) {
        return listKubeResources(resourceClass, namespaceName).stream().filter(it -> it.getMetadata().getName().startsWith(prefix)).toList();
    }

    public static <T extends HasMetadata> List<T> listKubeResourcesByLabelSelector(Class<T> resourceClass, String namespaceName, LabelSelector labelSelector) {
        return getKubeResourceClient(resourceClass).inNamespace(namespaceName).withLabelSelector(labelSelector).list().getItems();
    }
}
