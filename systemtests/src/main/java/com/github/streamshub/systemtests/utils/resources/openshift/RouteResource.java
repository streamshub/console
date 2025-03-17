package com.github.streamshub.systemtests.utils.resources.openshift;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.resources.KubeResourceManager;

public class RouteResource {

    // -------------
    // Client
    // -------------
    private static MixedOperation<Route, RouteList, Resource<Route>> routeClient() {
        return KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).routes();
    }

    // -------------
    // Get
    // -------------
    public static Route getRoute(String namespace, String name) {
        return routeClient().inNamespace(namespace).withName(name).get();
    }
}
