package org.bf2.admin.kafka.admin.handlers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MismatchedInputExceptionMapper implements ExceptionMapper<com.fasterxml.jackson.databind.exc.MismatchedInputException> {

    @Override
    public Response toResponse(com.fasterxml.jackson.databind.exc.MismatchedInputException exception) {
        return CommonHandler.processFailure(exception).build();
    }
}
