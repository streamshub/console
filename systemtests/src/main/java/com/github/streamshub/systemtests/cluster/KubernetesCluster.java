package com.github.streamshub.systemtests.cluster;

import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.Exec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class KubernetesCluster implements KubeCluster {

    public static final String CMD = "kubectl";
    private static final Logger LOGGER = LogManager.getLogger(KubernetesCluster.class);

    @Override
    public boolean isAvailable() {
        return Exec.isExecutableOnPath(CMD);
    }

    @Override
    public boolean isClusterUp() {
        List<String> cmd = List.of(CMD, "cluster-info");
        try {
            return Exec.exec(cmd).exitStatus() && !Exec.exec(List.of(CMD, "api-versions")).out().contains("openshift.io");
        } catch (KubeClusterException e) {
            LOGGER.debug("[{}] failed. Please double check connectivity to your cluster! {}", String.join(" ", cmd), e);
            return false;
        }
    }

    public String toString() {
        return CMD;
    }
}
