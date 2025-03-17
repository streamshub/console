package com.github.streamshub.systemtests.exceptions;

/**
 * Exception indicating that the Kubernetes cluster may not be responding and is likely broken.
 * This could be caused by network issues or out-of-memory problems.
 */
public class KubernetesClusterUnstableException extends RuntimeException {
    public KubernetesClusterUnstableException(String message) {
        super(message);
    }
}

