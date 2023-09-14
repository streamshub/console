package com.github.eyefloaters.console.api.errors.client;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.api.support.InvalidPageCursorException;

@Provider
public class InvalidPageCursorExceptionHandler implements ExceptionMapper<InvalidPageCursorException> {

    Logger logger = Logger.getLogger(InvalidPageCursorExceptionHandler.class);
    private static final ErrorCategory CATEGORY = ErrorCategory.get(ErrorCategory.InvalidQueryParameter.class);

    @Override
    public Response toResponse(InvalidPageCursorException exception) {
        List<Error> errors = exception.getSources()
            .stream()
            .map(source -> {
                Error error = CATEGORY.createError(exception.getMessage(), exception, source);
                logger.debugf(exception, "error=%s", error);
                return error;
            })
            .toList();

        return Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(errors))
                .build();
    }

}
