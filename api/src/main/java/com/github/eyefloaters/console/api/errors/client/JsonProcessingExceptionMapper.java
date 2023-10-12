package com.github.eyefloaters.console.api.errors.client;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.support.ErrorCategory;

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
    public List<Error> buildErrors(JsonProcessingException exception) {
        var errorLocation = exception.getLocation();
        Error error = category.createError("Unable to parse JSON at line %d, column %d"
                .formatted(errorLocation.getLineNr(), errorLocation.getColumnNr()), exception, null);
        LOGGER.debugf("error=%s", error);
        return List.of(error);
    }

}
