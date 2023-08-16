package com.github.eyefloaters.console.api.support;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;

@Provider
public class UnhandledThrowableHandler implements ExceptionMapper<Throwable> {

    Logger logger = Logger.getLogger("com.github.eyefloaters.console.api.errors.server");
    private static final ErrorCategory CATEGORY = ErrorCategory.SERVER_ERROR;

    @Override
    public Response toResponse(Throwable exception) {
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
