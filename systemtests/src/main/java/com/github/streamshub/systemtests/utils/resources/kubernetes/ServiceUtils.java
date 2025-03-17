package com.github.streamshub.systemtests.utils.resources.kubernetes;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.skodjob.testframe.resources.KubeResourceManager;

/**
 * Resource type implementation for managing Service resources.
 */
public class ServiceUtils {

    // -------------
    // Client
    // -------------
    private static MixedOperation<Service, ServiceList, io.fabric8.kubernetes.client.dsl.ServiceResource<Service>> serviceClient() {
        return KubeResourceManager.getKubeClient().getClient().services();
    }

    // -------------
    // Get
    // -------------
    public static Service getService(String namespace, String name) {
        return serviceClient().inNamespace(namespace).withName(name).get();
    }
}
