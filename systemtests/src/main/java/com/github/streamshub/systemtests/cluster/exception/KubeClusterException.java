package com.github.streamshub.systemtests.cluster.exception;


import io.skodjob.testframe.executor.ExecResult;

public class KubeClusterException extends RuntimeException {
    public final ExecResult result;

    public KubeClusterException(String s) {
        super(s);
        this.result = null;
    }
}
