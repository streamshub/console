package com.github.streamshub.systemtests.utils.resourceutils.kroxy;

public class KroxyUtils {
    private KroxyUtils() {}

    public static String getKroxyBootstrapServer(String namespace, String virtualClusterName, String kafkaProxyIngressName) {
        return virtualClusterName + "-" + kafkaProxyIngressName + "-bootstrap" + "." + namespace + ".svc.cluster.local:9292";
    }
}
