package com.github.streamshub.systemtests.cluster;

import com.github.streamshub.systemtests.logs.LogWrapper;
import org.apache.logging.log4j.Logger;

public class KubeClusterResource {
    private static final Logger LOGGER = LogWrapper.getLogger(KubeClusterResource.class);
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
