package com.github.streamshub.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import com.github.streamshub.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class NotSupportedExceptionHandler extends AbstractClientExceptionHandler<NotSupportedException> {

    public NotSupportedExceptionHandler() {
        super(ErrorCategory.UnsupportedMediaType.class, "Content-type not supported", HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof NotSupportedException;
    }
}
