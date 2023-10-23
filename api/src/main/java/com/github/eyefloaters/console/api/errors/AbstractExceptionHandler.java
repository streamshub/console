package com.github.eyefloaters.console.api.errors;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

public abstract class AbstractExceptionHandler<T extends Throwable> implements SelectableExceptionMapper<T> {

    protected final ErrorCategory category;

    protected AbstractExceptionHandler(Class<? extends ErrorCategory> categoryType) {
        this.category = ErrorCategory.get(categoryType);
    }

    /**
     * Find the most frequently occurring status from the list of errors
     *
     * @param errors        list of errors to inspect
     * @param defaultStatus supplier of a default status if none are found in the
     *                      list
     * @return the most frequently occurring status or the default
     */
    public static Status maxOccurringStatus(List<Error> errors, Supplier<Status> defaultStatus) {
        return errors.stream()
            .collect(Collectors.groupingBy(Error::getStatus, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .map(Integer::valueOf)
            .map(Status::fromStatusCode)
            .orElseGet(defaultStatus);
    }

    @Override
    public Response toResponse(T exception) {
        List<Error> errors = buildErrors(exception);

        Status status = maxOccurringStatus(errors, category::getHttpStatus);

        return Response.status(status)
                .entity(new ErrorResponse(errors))
                .build();
    }

}
