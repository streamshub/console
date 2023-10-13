package com.github.eyefloaters.console.api.errors;

import java.util.concurrent.CompletionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class CompletionExceptionHandler implements ExceptionMapper<CompletionException> {

    @Inject
    @Named("fallbackMapper")
    SelectableExceptionMapper<Throwable> fallbackMapper;

    @Inject
    Instance<SelectableExceptionMapper<? extends Throwable>> availableMappers;

    @SuppressWarnings("unchecked")
    @Override
    public Response toResponse(CompletionException exception) {
        Throwable cause = exception.getCause();

        return availableMappers.stream()
            .filter(mapper -> mapper.handlesException(cause))
            .findFirst()
            .map(mapper -> (SelectableExceptionMapper<Throwable>) mapper)
            .orElse(fallbackMapper)
            .toResponse(cause);
    }

}
