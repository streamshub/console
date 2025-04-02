package com.github.streamshub.systemtests.exceptions;

public class FileOperationException extends SetupException {
    public FileOperationException(String message) {
        super(message);
    }

    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
