package com.github.streamshub.systemtests.cluster;

import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.Exec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MinikubeCluster implements KubeCluster {
    public static final String CMD = "minikube";
    private static final Logger LOGGER = LogManager.getLogger(MinikubeCluster.class);

    @Override
    public boolean isAvailable() {
        return Exec.isExecutableOnPath(CMD);
    }

    @Override
    public boolean isClusterUp() {
        List<String> cmd = List.of(KubernetesCluster.CMD, "get", "nodes", "-o", "jsonpath='{.items[*].metadata.labels}'");
        try {
            return Exec.exec(cmd).exitStatus() && Exec.exec(cmd).out().contains("minikube.k8s.io");
        } catch (KubeClusterException e) {
            LOGGER.debug("{} failed. Please double check connectivity to your cluster! {}",  String.join(" ", cmd), e);
            return false;
        }
    }

    public String toString() {
        return CMD;
    }
}
