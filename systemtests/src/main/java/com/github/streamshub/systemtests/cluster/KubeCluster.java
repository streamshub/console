package com.github.streamshub.systemtests.cluster;

import io.skodjob.testframe.clients.KubeClusterException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Locale;

public interface KubeCluster {

    String TEST_CLUSTER_TYPE_ENV = "TEST_CLUSTER_TYPE";

    boolean isAvailable();
    boolean isClusterUp();

    static KubeCluster getInstance() {
        Logger logger = LogManager.getLogger(KubeCluster.class);
        KubeCluster[] clusters = null;
        String clusterName = System.getenv(TEST_CLUSTER_TYPE_ENV);
        // In case the env is specified
        if (clusterName != null) {
            clusters = switch (clusterName.toLowerCase(Locale.ENGLISH)) {
                case "oc" -> new KubeCluster[]{new OpenshiftCluster()};
                case "minikube" -> new KubeCluster[]{new MinikubeCluster()};
                case "kubectl" -> new KubeCluster[]{new KubernetesCluster()};
                default ->
                    throw new KubeClusterException(new Throwable(TEST_CLUSTER_TYPE_ENV + "=" + clusterName + " is not a supported cluster type"));
            };
        }
        // In case env was not specified thus clusters are null
        if (clusters == null) {
            clusters = new KubeCluster[]{new MinikubeCluster(), new OpenshiftCluster(), new KubernetesCluster()};
        }

        // Set cluster type
        KubeCluster cluster = null;
        for (KubeCluster kc : clusters) {
            if (kc.isAvailable()) {
                logger.debug("Cluster type [{}] is installed", kc);
                if (kc.isClusterUp()) {
                    logger.debug("Cluster type [{}] is running", kc);
                    cluster = kc;
                    break;
                } else {
                    logger.debug("Cluster type [{}] is not running!", kc);
                }
            } else {
                logger.debug("Cluster type [{}] is not installed!", kc);
            }
        }

        if (cluster == null) {
            throw new KubeClusterException(new Throwable("Unable to find a cluster type, tried: " + Arrays.toString(clusters)));
        }
        logger.info("Using cluster type {}", cluster);
        return cluster;
    }
}
