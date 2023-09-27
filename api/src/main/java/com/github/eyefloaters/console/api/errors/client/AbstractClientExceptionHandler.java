package com.github.eyefloaters.console.api.errors.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.errors.SelectableExceptionMapper;
import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

import static java.util.Objects.requireNonNullElseGet;

abstract class AbstractClientExceptionHandler<T extends Throwable> implements SelectableExceptionMapper<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractClientExceptionHandler.class);

    protected final ErrorCategory category;
    protected final String message;
    protected final String property;

    AbstractClientExceptionHandler(Class<? extends ErrorCategory> categoryType, String message, String property) {
        this.category = ErrorCategory.get(categoryType);
        this.message = message;
        this.property = property;
    }

    protected List<Error> buildErrors(T exception) {
        Error error = category.createError(requireNonNullElseGet(message, exception::getMessage), exception, property);
        LOGGER.debugf("error=%s", error);
        return List.of(error);
    }

    @Override
    public Response toResponse(T exception) {
        List<Error> errors = buildErrors(exception);

        // Find the most frequently occurring status from the list of errors
        Status status = errors.stream()
            .collect(Collectors.groupingBy(Error::getStatus, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .map(Integer::valueOf)
            .map(Status::fromStatusCode)
            .orElseGet(category::getHttpStatus);

        return Response.status(status)
                .entity(new ErrorResponse(errors))
                .build();
    }

}
