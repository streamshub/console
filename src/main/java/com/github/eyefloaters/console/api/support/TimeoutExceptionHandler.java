package com.github.eyefloaters.console.api.support;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.apache.kafka.common.errors.TimeoutException;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;

public class TimeoutExceptionHandler implements ExceptionMapper<TimeoutException> {

    private static final ErrorCategory CATEGORY = ErrorCategory.BACKEND_TIMEOUT;

    @Override
    public Response toResponse(TimeoutException exception) {
        Error error = new Error(CATEGORY.getTitle(), exception.getMessage(), exception);
        error.setStatus(String.valueOf(CATEGORY.getHttpStatus().getStatusCode()));
        error.setCode(CATEGORY.getCode());

        return Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)))
                .build();
    }

}
