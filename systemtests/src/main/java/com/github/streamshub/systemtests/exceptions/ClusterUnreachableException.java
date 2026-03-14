package com.github.streamshub.systemtests.exceptions;

import io.skodjob.kubetest4j.clients.KubeClusterException;
import io.skodjob.kubetest4j.executor.ExecResult;

public class ClusterUnreachableException extends KubeClusterException {
    public ClusterUnreachableException(ExecResult result) {
        super(result,
            "Cluster is currently unreachable. This may be due to the cluster being unstable or down. Please check the connection.: " + result.out());
    }

    public ClusterUnreachableException(ExecResult result, String message) {
        super(result, message);
    }

    public ClusterUnreachableException(String message) {
        super(new Throwable(message));
    }
}
