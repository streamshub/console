package com.github.streamshub.systemtests.cluster.exception;


import io.skodjob.testframe.executor.ExecResult;

public class KubeClusterException extends RuntimeException {
    public final ExecResult result;

    public KubeClusterException(ExecResult result, String s) {
        super(s);
        this.result = result;
    }

    public KubeClusterException(String s) {
        super(s);
        this.result = null;
    }

    public KubeClusterException(Throwable cause) {
        super(cause);
        this.result = null;
    }

    public KubeClusterException(String message, Throwable cause) {
        super(message, cause);
        this.result = null;
    }

    public static class NotFound extends KubeClusterException {
        public NotFound(ExecResult result, String s) {
            super(result, s);
        }

        public NotFound(String s) {
            super(s);
        }
    }

    public static class AlreadyExists extends KubeClusterException {
        public AlreadyExists(ExecResult result, String s) {
            super(result, s);
        }
    }

    public static class InvalidResource extends KubeClusterException {
        public InvalidResource(ExecResult result, String s) {
            super(result, s);
        }
    }

    public static class UnstableCluster extends KubeClusterException {
        public UnstableCluster() {
            super("Cluster is not stable");
        }
    }
}
