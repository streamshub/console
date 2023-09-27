package com.github.eyefloaters.console.api.errors.server;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.errors.SelectableExceptionMapper;
import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

abstract class AbstractServerExceptionHandler<T extends Throwable> implements SelectableExceptionMapper<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractServerExceptionHandler.class);

    protected final ErrorCategory category;

    AbstractServerExceptionHandler(Class<? extends ErrorCategory> categoryType) {
        this.category = ErrorCategory.get(categoryType);
    }

    public Response toResponse(T exception) {
        Error error = category.createError(exception.getMessage(), exception, null);
        LOGGER.warnf(exception, "error=%s", error);

        return Response.status(category.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)))
                .build();
    }

}
