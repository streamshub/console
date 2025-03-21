package com.github.streamshub.systemtests.cluster;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.Exec;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class OpenshiftCluster implements KubeCluster {
    private static final String CMD = "oc";
    private static final Logger LOGGER = LogWrapper.getLogger(OpenshiftCluster.class);

    @Override
    public boolean isAvailable() {
        return Exec.isExecutableOnPath(CMD);
    }

    @Override
    public boolean isClusterUp() {
        List<String> cmd = List.of(CMD, "status", "-n", "default");
        try {
            return Exec.exec(cmd).exitStatus() && Exec.exec(List.of(CMD, "api-versions")).out().contains("openshift.io");
        } catch (KubeClusterException e) {
            LOGGER.debug("[{}] failed. Please double check connectivity to your cluster! {}", String.join(" ", cmd), e);
            return false;
        }
    }

    public String toString() {
        return CMD;
    }
}