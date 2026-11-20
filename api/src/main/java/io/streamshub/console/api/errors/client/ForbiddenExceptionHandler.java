package io.streamshub.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import io.streamshub.console.api.model.jsonapi.JsonApiErrors;
import io.streamshub.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class ForbiddenExceptionHandler extends AbstractClientExceptionHandler<ForbiddenException> {

    public ForbiddenExceptionHandler() {
        super(ErrorCategory.NotAuthorized.class, "Insufficient permissions to resource or action", (String) null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof ForbiddenException;
    }

    @Override
    public Response toResponse(ForbiddenException exception) {
        var responseBuilder = Response.status(category.getHttpStatus())
                .entity(new JsonApiErrors(buildErrors(exception)));

        exception.getResponse().getHeaders().forEach((k, v) ->
            responseBuilder.header(k, exception.getResponse().getHeaderString(k)));

        return responseBuilder.build();
    }
}