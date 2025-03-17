package com.github.streamshub.systemtests.cluster.exception;

/**
 * Exception thrown when no cluster is available.
 */
public class NoClusterException extends RuntimeException {

    public NoClusterException(String message) {
        super(message);
    }
}
