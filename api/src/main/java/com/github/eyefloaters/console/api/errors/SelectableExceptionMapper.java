package com.github.eyefloaters.console.api.errors;

import java.util.List;

import jakarta.ws.rs.ext.ExceptionMapper;

import com.github.eyefloaters.console.api.model.Error;

/**
 * Custom interface used to select CDI bean instances of ExceptionMapper.
 * This avoids maintenance of a hard-coded mapping in {@link CompletionExceptionHandler}
 * where the cause is extracted and handled indirectly.
 *
 * @param <E> the type of throwable handled by the mapper
 */
public interface SelectableExceptionMapper<E extends Throwable> extends ExceptionMapper<E> {

    /**
     * Determines whether this ExceptionMapper handles the Throwable
     *
     * @param thrown a Throwable to potentially handle
     * @return true if this mapper handles the Throwable, otherwise false
     */
    boolean handlesException(Throwable thrown);

    /**
     * Construct a list of one or more errors based on the thrown
     * Exception/Throwable.
     *
     * @param exception the exception thrown by the application
     * @return list of errors derived from the exception/throwable
     */
    List<Error> buildErrors(E exception);

}
