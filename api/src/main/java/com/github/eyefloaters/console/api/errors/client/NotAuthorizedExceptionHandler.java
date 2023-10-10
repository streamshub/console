package com.github.eyefloaters.console.api.errors.client;

import java.util.List;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

@Provider
public class NotAuthorizedExceptionHandler implements ExceptionMapper<NotAuthorizedException> {

    private static final Logger LOGGER = Logger.getLogger(NotAuthorizedExceptionHandler.class);
    private static final ErrorCategory CATEGORY = ErrorCategory.get(ErrorCategory.NotAuthenticated.class);

    @Override
    public Response toResponse(NotAuthorizedException exception) {
        Error error = CATEGORY.createError("Authentication credentials missing or invalid", exception, null);
        LOGGER.debugf("error=%s", error);

        var responseBuilder = Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)));

        exception.getResponse().getHeaders().forEach((k, v) ->
            responseBuilder.header(k, exception.getResponse().getHeaderString(k)));

        return responseBuilder.build();
    }

}
