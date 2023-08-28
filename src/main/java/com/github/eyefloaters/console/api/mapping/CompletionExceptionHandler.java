package com.github.eyefloaters.console.api.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.kafka.common.errors.TimeoutException;

@Provider
public class CompletionExceptionHandler implements ExceptionMapper<CompletionException> {

    static final Map<Class<? extends Throwable>, ExceptionMapper<? extends Throwable>> MAPPERS = new HashMap<>();
    static final ExceptionMapper<Throwable> FALLBACK_MAPPER = new UnhandledThrowableHandler();

    static {
        MAPPERS.put(NotFoundException.class, new NotFoundExceptionHandler());
        MAPPERS.put(org.apache.kafka.common.errors.UnknownTopicOrPartitionException.class, new UnknownTopicOrPartitionExceptionHandler());
        MAPPERS.put(TimeoutException.class, new TimeoutExceptionHandler());
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> ExceptionMapper<T> getMapper(T cause) {
        return (ExceptionMapper<T>) MAPPERS.getOrDefault(cause.getClass(), FALLBACK_MAPPER);
    }

    @Override
    public Response toResponse(CompletionException exception) {
        Throwable cause = exception.getCause();

        return getMapper(cause).toResponse(cause);
    }

}
