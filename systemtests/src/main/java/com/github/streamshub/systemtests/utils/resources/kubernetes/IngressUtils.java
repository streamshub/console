package com.github.streamshub.systemtests.utils.resources.kubernetes;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;

public class IngressUtils {

    // -------------
    // Client
    // -------------
    private static MixedOperation<Ingress, IngressList, Resource<Ingress>> ingressClient() {
        return KubeResourceManager.getKubeClient().getClient().network().v1().ingresses();
    }
}
