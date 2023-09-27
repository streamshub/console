package com.github.eyefloaters.console.api.errors.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import com.github.eyefloaters.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class UnhandledThrowableHandler extends AbstractServerExceptionHandler<Throwable> {

    public UnhandledThrowableHandler() {
        super(ErrorCategory.ServerError.class);
    }

}
