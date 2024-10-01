package com.github.streamshub.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.ext.Provider;

import com.github.streamshub.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class NotAllowedExceptionHandler extends AbstractClientExceptionHandler<NotAllowedException> {

    public NotAllowedExceptionHandler() {
        super(ErrorCategory.MethodNotAllowed.class, "HTTP method not allowed for this resource", (String) null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof NotAllowedException;
    }
}
