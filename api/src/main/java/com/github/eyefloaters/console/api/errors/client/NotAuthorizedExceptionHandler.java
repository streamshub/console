package com.github.eyefloaters.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class NotAuthorizedExceptionHandler extends AbstractClientExceptionHandler<NotAuthorizedException> {

    public NotAuthorizedExceptionHandler() {
        super(ErrorCategory.NotAuthenticated.class, "Authentication credentials missing or invalid", (String) null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof NotAuthorizedException;
    }

    @Override
    public Response toResponse(NotAuthorizedException exception) {
        var responseBuilder = Response.status(category.getHttpStatus())
                .entity(new ErrorResponse(buildErrors(exception)));

        exception.getResponse().getHeaders().forEach((k, v) ->
            responseBuilder.header(k, exception.getResponse().getHeaderString(k)));

        return responseBuilder.build();
    }

}
