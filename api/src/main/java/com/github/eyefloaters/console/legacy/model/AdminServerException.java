package com.github.eyefloaters.console.legacy.model;

public class AdminServerException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final ErrorType error;

    public AdminServerException(ErrorType error, Throwable cause) {
        super(cause);
        this.error = error;
    }

    public AdminServerException(ErrorType error) {
        this.error = error;
    }

    public AdminServerException(ErrorType error, String message) {
        super(message);
        this.error = error;
    }

    public ErrorType getError() {
        return error;
    }
}
