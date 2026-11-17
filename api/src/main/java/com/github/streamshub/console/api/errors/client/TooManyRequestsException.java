package com.github.streamshub.console.api.errors.client;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response.Status;

/**
 * Thrown when a request is rejected because a server-defined limit on the number
 * of concurrent operations of its kind has been reached (HTTP 429).
 */
public class TooManyRequestsException extends ClientErrorException {

    private static final long serialVersionUID = 1L;

    public TooManyRequestsException(String message) {
        super(message, Status.TOO_MANY_REQUESTS);
    }
}
