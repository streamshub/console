package io.streamshub.systemtests.exceptions;

public class SetupException extends RuntimeException {
    public SetupException(String message) {
        super(message);
    }

    public SetupException(String message, Throwable cause) {
        super(message, cause);
    }
}
