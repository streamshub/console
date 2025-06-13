package com.github.streamshub.console.api.errors.client;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.jsonapi.JsonApiError;
import com.github.streamshub.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class InvalidPageCursorExceptionHandler extends AbstractClientExceptionHandler<InvalidPageCursorException> {

    private static final Logger LOGGER = Logger.getLogger(InvalidPageCursorExceptionHandler.class);

    public InvalidPageCursorExceptionHandler() {
        super(ErrorCategory.InvalidQueryParameter.class, null, (String) null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof InvalidPageCursorException;
    }

    @Override
    public List<JsonApiError> buildErrors(InvalidPageCursorException exception) {
        return exception.getSources()
            .stream()
            .map(source -> {
                JsonApiError error = category.createError(exception.getMessage(), exception, source);
                LOGGER.debugf(exception, "error=%s", error);
                return error;
            })
            .toList();
    }

}
