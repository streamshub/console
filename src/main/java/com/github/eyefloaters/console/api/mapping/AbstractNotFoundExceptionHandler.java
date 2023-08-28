package com.github.eyefloaters.console.api.mapping;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

abstract class AbstractNotFoundExceptionHandler<T extends Throwable> implements ExceptionMapper<T> {

    Logger logger = Logger.getLogger("com.github.eyefloaters.console.api.errors.client");
    private static final ErrorCategory CATEGORY = ErrorCategory.RESOURCE_NOT_FOUND;

    @Override
    public Response toResponse(T exception) {
        String id = UUID.randomUUID().toString();
        Error error = new Error(CATEGORY.getTitle(), exception.getMessage(), exception);
        error.setId(id);
        error.setStatus(String.valueOf(CATEGORY.getHttpStatus().getStatusCode()));
        error.setCode(CATEGORY.getCode());

        logger.debugf("id=%s title='%s' detail='%s' source=%s", id, error.getTitle(), error.getDetail(), error.getSource());

        return Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)))
                .build();
    }

}
