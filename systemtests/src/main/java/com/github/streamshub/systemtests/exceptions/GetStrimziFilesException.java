package com.github.streamshub.systemtests.exceptions;

public class GetStrimziFilesException extends RuntimeException {
    public GetStrimziFilesException(String message) {
        super(message);
    }

    public GetStrimziFilesException(String message, Throwable cause) {
        super(message, cause);
    }
}
