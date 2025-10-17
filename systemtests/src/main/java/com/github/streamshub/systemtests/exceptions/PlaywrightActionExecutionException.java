package com.github.streamshub.systemtests.exceptions;

public class PlaywrightActionExecutionException extends RuntimeException {

    public PlaywrightActionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlaywrightActionExecutionException(String message) {
        super(message);
    }
}