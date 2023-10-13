package com.github.eyefloaters.console.api.errors.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.ws.rs.ext.Provider;

import com.github.eyefloaters.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
@Named("fallbackMapper")
public class UnhandledThrowableHandler extends AbstractServerExceptionHandler<Throwable> {

    public UnhandledThrowableHandler() {
        super(ErrorCategory.ServerError.class);
    }

    /**
     * Determines whether this ExceptionMapper handles the Throwable
     *
     * Since this class is a fallback mapper for any unhandled exceptions, no
     * {@code Throwable}s are reported as handled by this method and an instance of
     * {@code UnhandledThrowableHandler} is referenced directly when necessary.
     *
     * @param thrown a Throwable to potentially handle
     * @return always false
     */
    @Override
    public boolean handlesException(Throwable thrown) {
        return false;
    }

}
