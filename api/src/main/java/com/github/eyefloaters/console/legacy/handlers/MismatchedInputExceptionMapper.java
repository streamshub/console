package com.github.eyefloaters.console.legacy.handlers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MismatchedInputExceptionMapper implements ExceptionMapper<com.fasterxml.jackson.databind.exc.MismatchedInputException> {

    @Override
    public Response toResponse(com.fasterxml.jackson.databind.exc.MismatchedInputException exception) {
        return CommonHandler.processFailure(exception).build();
    }
}
