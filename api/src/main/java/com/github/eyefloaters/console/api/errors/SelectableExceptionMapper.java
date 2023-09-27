package com.github.eyefloaters.console.api.errors;

import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Custom interface used to select CDI bean instances of ExceptionMapper.
 * This avoids maintenance of a hard-coded mapping in {@link CompletionExceptionHandler}
 * where the cause is extracted and handled indirectly.
 *
 * @param <E> the type of throwable handled by the mapper
 */
public interface SelectableExceptionMapper<E extends Throwable> extends ExceptionMapper<E> {
}
