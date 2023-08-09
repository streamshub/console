package com.github.eyefloaters.console.api.support;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionHandler extends AbstractNotFoundExceptionHandler<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        return super.toResponse(exception);
    }

}
