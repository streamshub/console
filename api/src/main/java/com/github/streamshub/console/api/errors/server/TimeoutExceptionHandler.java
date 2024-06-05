package com.github.streamshub.console.api.errors.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.kafka.common.errors.TimeoutException;

import com.github.streamshub.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class TimeoutExceptionHandler extends AbstractServerExceptionHandler<TimeoutException> implements ExceptionMapper<TimeoutException> {

    public TimeoutExceptionHandler() {
        super(ErrorCategory.BackendTimeout.class);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof TimeoutException;
    }
}
