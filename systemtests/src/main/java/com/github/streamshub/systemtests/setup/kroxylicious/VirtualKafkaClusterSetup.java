package com.github.streamshub.systemtests.setup.kroxylicious;

public class VirtualKafkaClusterSetup {
    private VirtualKafkaClusterSetup(){}

    private static VirtualKafkaClusterBuilder baseVirtualKafkaClusterCR(String namespace, String clusterName, String kroxyName, String clusterRefName,String ingressName) {
        return new VirtualKafkaClusterBuilder()
                .withNewMetadata()
                    .withName(clusterName)
                    .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                    .withTargetKafkaServiceRef(new KafkaServiceRefBuilder()
                            .withName(clusterRefName)
                            .build())
                    .withNewProxyRef()
                        .withName(proxyName)
                    .endProxyRef()
                .addNewIngress()
                    .withNewIngressRef()
                        .withName(ingressName)
                    .endIngressRef()
                .endIngress()
                .endSpec();
        // @formatter:on
    }
}
