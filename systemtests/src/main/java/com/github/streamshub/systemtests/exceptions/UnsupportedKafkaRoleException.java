package com.github.streamshub.systemtests.exceptions;

public class UnsupportedKafkaRoleException extends RuntimeException {
    public UnsupportedKafkaRoleException(String message) {
        super(message);
    }
}
