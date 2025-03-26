package com.github.streamshub.systemtests.exceptions;

import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.ExecResult;

public class ClusterUnreachableException extends KubeClusterException {
    public ClusterUnreachableException(ExecResult result) {
        super(result,
            "Cluster is currently unreachable. This may be due to the cluster being unstable or down. Please check the connection.");
    }

    public ClusterUnreachableException(ExecResult result, String message) {
        super(result, message);
    }

    public ClusterUnreachableException(String message) {
        super(new Throwable(message));
    }
}
