package com.github.streamshub.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import com.github.streamshub.console.api.model.jsonapi.JsonApiErrors;
import com.github.streamshub.console.api.support.ErrorCategory;

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
                .entity(new JsonApiErrors(buildErrors(exception)));

        exception.getResponse().getHeaders().forEach((k, v) ->
            responseBuilder.header(k, exception.getResponse().getHeaderString(k)));

        return responseBuilder.build();
    }

}
