package com.github.streamshub.systemtests.cluster;

public class KubeClusterResource {
    private KubeCluster kubeCluster;
    private static KubeClusterResource kubeClusterResource;

    private KubeClusterResource() { }

    public static KubeClusterResource getInstance() {
        if (kubeClusterResource == null) {
            kubeClusterResource = new KubeClusterResource();
        }
        return kubeClusterResource;
    }

    public KubeCluster cluster() {
        if (kubeCluster == null) {
            kubeCluster = KubeCluster.getInstance();
        }
        return kubeCluster;
    }
}
