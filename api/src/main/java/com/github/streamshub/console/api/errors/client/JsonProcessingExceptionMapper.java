package com.github.streamshub.console.api.errors.client;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.streamshub.console.api.model.jsonapi.JsonApiError;
import com.github.streamshub.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class JsonProcessingExceptionMapper extends AbstractClientExceptionHandler<JsonProcessingException> {

    private static final Logger LOGGER = Logger.getLogger(JsonProcessingExceptionMapper.class);

    public JsonProcessingExceptionMapper() {
        super(ErrorCategory.InvalidResource.class, "Unable to parse JSON document", (String) null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof JsonProcessingException;
    }

    @Override
    public List<JsonApiError> buildErrors(JsonProcessingException exception) {
        var errorLocation = exception.getLocation();
        JsonApiError error;
        if (errorLocation != null) {
            error = category.createError("Unable to process JSON at line %d, column %d"
                    .formatted(errorLocation.getLineNr(), errorLocation.getColumnNr()), exception, null);
        } else {
            error = category.createError("Unable to process JSON", exception, null);
        }
        LOGGER.debugf("error=%s, exception=%s", error, exception.getMessage());
        return List.of(error);
    }

}
