package com.github.eyefloaters.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class NotFoundExceptionHandler extends AbstractNotFoundExceptionHandler<NotFoundException> {
    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof NotFoundException;
    }
}
