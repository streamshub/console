package com.github.eyefloaters.console.api.errors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.github.eyefloaters.console.api.errors.client.InvalidPageCursorException;
import com.github.eyefloaters.console.api.errors.client.InvalidPageCursorExceptionHandler;
import com.github.eyefloaters.console.api.errors.client.NotFoundExceptionHandler;
import com.github.eyefloaters.console.api.errors.client.UnknownTopicIdExceptionHandler;
import com.github.eyefloaters.console.api.errors.client.UnknownTopicOrPartitionExceptionHandler;
import com.github.eyefloaters.console.api.errors.server.TimeoutExceptionHandler;
import com.github.eyefloaters.console.api.errors.server.UnhandledThrowableHandler;

@Provider
public class CompletionExceptionHandler implements ExceptionMapper<CompletionException> {

    static final Map<Class<? extends Throwable>, ExceptionMapper<? extends Throwable>> MAPPERS = new HashMap<>();
    static final ExceptionMapper<Throwable> FALLBACK_MAPPER = new UnhandledThrowableHandler();

    static {
        MAPPERS.put(NotFoundException.class, new NotFoundExceptionHandler());
        MAPPERS.put(org.apache.kafka.common.errors.UnknownTopicIdException.class, new UnknownTopicIdExceptionHandler());
        MAPPERS.put(org.apache.kafka.common.errors.UnknownTopicOrPartitionException.class, new UnknownTopicOrPartitionExceptionHandler());
        MAPPERS.put(InvalidPageCursorException.class, new InvalidPageCursorExceptionHandler());
        MAPPERS.put(org.apache.kafka.common.errors.TimeoutException.class, new TimeoutExceptionHandler());
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
