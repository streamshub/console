package com.github.streamshub.systemtests.cluster.exception;

/**
 * Exception thrown to indicate an issue related to waiting.
 */
public class WaitException extends RuntimeException {

    public WaitException(String message) {
        super(message);
    }

    public WaitException(Throwable cause) {
        super(cause);
    }
}
