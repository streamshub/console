package com.github.eyefloaters.console.api.errors;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;

import static com.github.eyefloaters.console.api.errors.AbstractExceptionHandler.maxOccurringStatus;

/**
 * Base {@linkplain ExceptionMapper} that "unwraps" the cause of the caught
 * exception and delegates handling to one of the
 * {@linkplain SelectableExceptionMapper}s injected and known to the
 * application.
 *
 * @param <T> the type of Throwable handled by the handler
 */
abstract class UnwrappingExceptionHandler<T extends Throwable> implements ExceptionMapper<T> {

    @Inject
    @Named("fallbackMapper")
    SelectableExceptionMapper<Throwable> fallbackMapper;

    @Inject
    Instance<SelectableExceptionMapper<? extends Throwable>> availableMappers;

    @Override
    public Response toResponse(T exception) {
        Throwable cause = exception.getCause();
        List<Throwable> suppressed = Arrays.asList(exception.getSuppressed());

        if (suppressed.isEmpty()) {
            return selectMapper(cause).toResponse(cause);
        }

        List<Error> errors = suppressed.stream()
            .map(error -> selectMapper(error).buildErrors(error))
            .flatMap(Collection::stream)
            .toList();

        Status status = maxOccurringStatus(errors, () -> Status.INTERNAL_SERVER_ERROR);

        return Response.status(status)
                .entity(new ErrorResponse(errors))
                .build();
    }

    @SuppressWarnings("unchecked")
    SelectableExceptionMapper<Throwable> selectMapper(Throwable cause) {
        return availableMappers.stream()
            .filter(mapper -> mapper.handlesException(cause))
            .findFirst()
            .map(mapper -> (SelectableExceptionMapper<Throwable>) mapper)
            .orElse(fallbackMapper);
    }
}
