package com.github.streamshub.systemtests.exceptions;

public class PullSecretNotFound extends RuntimeException {

    public PullSecretNotFound(String message) {
        super(message);
    }
}
