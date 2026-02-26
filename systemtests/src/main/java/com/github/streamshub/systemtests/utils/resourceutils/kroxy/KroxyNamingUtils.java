package com.github.streamshub.systemtests.utils.resourceutils.kroxy;

import com.github.streamshub.systemtests.constants.Constants;

import static com.github.streamshub.systemtests.utils.Utils.hashStub;

public class KroxyNamingUtils {
    private KroxyNamingUtils() {}

    public static String kafkaProxyName(String namespaceName) {
        return Constants.KAFKA_PROXY_PREFIX + "-" + hashStub(namespaceName);
    }

    public static String virtualKafkaClusterName(String proxyName) {
        return Constants.VIRTUAL_KAFKA_CLUSTER_PREFIX + "-" + hashStub(proxyName);
    }

    public static String kafkaServiceName(String proxyName) {
        return Constants.KAFKA_SERVICE_PREFIX + "-" + hashStub(proxyName);
    }

    public static String kafkaProxyIngressName(String proxyName) {
        return Constants.KAFKA_PROXY_INGRESS_PREFIX + "-" + hashStub(proxyName);
    }

    public static String kafkaProtocolFilterName(String proxyName) {
        return Constants.KAFKA_PROTOCOL_FILTER_PREFIX + "-" + hashStub(proxyName);
    }

    public static String aclConfigMapName(String proxyName) {
        return Constants.CONFIG_MAP_PREFIX + "-acl-" + hashStub(proxyName);
    }
}
