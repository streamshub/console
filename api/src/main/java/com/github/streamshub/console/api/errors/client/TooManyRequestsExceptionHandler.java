package com.github.streamshub.console.api.errors.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import com.github.streamshub.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class TooManyRequestsExceptionHandler extends AbstractClientExceptionHandler<TooManyRequestsException> {

    public TooManyRequestsExceptionHandler() {
        super(ErrorCategory.TooManyRequests.class, null, (String) null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof TooManyRequestsException;
    }
}
