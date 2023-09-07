package com.github.eyefloaters.console.api.mapping;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.apache.kafka.common.errors.TimeoutException;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.support.ErrorCategory;

public class TimeoutExceptionHandler implements ExceptionMapper<TimeoutException> {

    Logger logger = Logger.getLogger("com.github.eyefloaters.console.api.errors.server");
    private static final ErrorCategory CATEGORY = ErrorCategory.get(ErrorCategory.BackendTimeout.class);

    @Override
    public Response toResponse(TimeoutException exception) {
        String id = UUID.randomUUID().toString();
        Error error = new Error(CATEGORY.getTitle(), exception.getMessage(), exception);
        error.setId(id);
        error.setStatus(String.valueOf(CATEGORY.getHttpStatus().getStatusCode()));
        error.setCode(CATEGORY.getCode());

        logger.warnf(exception, "id=%s title='%s' detail='%s' source=%s", id, error.getTitle(), error.getDetail(), error.getSource());

        return Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)))
                .build();
    }

}
