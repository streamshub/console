package com.github.eyefloaters.console.api.mapping;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

abstract class AbstractNotFoundExceptionHandler<T extends Throwable> implements ExceptionMapper<T> {

    Logger logger = Logger.getLogger("com.github.eyefloaters.console.api.errors.client");
    private static final ErrorCategory CATEGORY = ErrorCategory.get(ErrorCategory.ResourceNotFound.class);

    @Override
    public Response toResponse(T exception) {
        Error error = CATEGORY.createError(exception.getMessage(), exception, null);
        logger.debugf("error=%s", error);

        return Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)))
                .build();
    }

}
