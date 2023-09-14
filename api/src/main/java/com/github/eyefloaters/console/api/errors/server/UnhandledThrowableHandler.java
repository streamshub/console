package com.github.eyefloaters.console.api.errors.server;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

@Provider
public class UnhandledThrowableHandler implements ExceptionMapper<Throwable> {

    Logger logger = Logger.getLogger(UnhandledThrowableHandler.class);
    private static final ErrorCategory CATEGORY = ErrorCategory.get(ErrorCategory.ServerError.class);

    @Override
    public Response toResponse(Throwable exception) {
        Error error = CATEGORY.createError(exception.getMessage(), exception, null);
        logger.warnf(exception, "error=%s", error);

        return Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)))
                .build();
    }

}
