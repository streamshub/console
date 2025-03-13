package com.github.streamshub.systemtests.utils.resources.openshift;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.config.v1.DNS;
import io.fabric8.openshift.api.model.config.v1.DNSList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.resources.KubeResourceManager;

public class DnsUtils {

    // -------------
    // Client
    // -------------
    private static NonNamespaceOperation<DNS, DNSList, Resource<DNS>> dnsClient() {
        return KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).config().dnses();
    }

    // -------------
    // Get
    // -------------
    public static DNS getClusterDns() {
        return get("cluster");
    }

    public static DNS get(String name) {
        return dnsClient().withName(name).get();
    }
}
