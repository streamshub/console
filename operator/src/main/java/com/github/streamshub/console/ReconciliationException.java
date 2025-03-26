package com.github.streamshub.console;

public class ReconciliationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReconciliationException(String message) {
        super(message);
    }

    public ReconciliationException(String message, Throwable cause) {
        super(message, cause);
    }
}
