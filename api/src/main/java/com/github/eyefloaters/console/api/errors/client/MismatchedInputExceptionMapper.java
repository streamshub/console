package com.github.eyefloaters.console.api.errors.client;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.support.ErrorCategory;

@Provider
@ApplicationScoped
public class MismatchedInputExceptionMapper extends AbstractClientExceptionHandler<MismatchedInputException> {

    private static final Logger LOGGER = Logger.getLogger(MismatchedInputExceptionMapper.class);

    public MismatchedInputExceptionMapper() {
        super(ErrorCategory.InvalidResource.class, "JSON document does not match target schema", null);
    }

    @Override
    protected List<Error> buildErrors(MismatchedInputException exception) {
        String pointer = exception.getPath().stream()
                .map(ref -> Objects.requireNonNullElseGet(ref.getFieldName(), ref::getIndex).toString())
                .collect(Collectors.joining("/", "/", ""));

        Error error = category.createError(message, exception, pointer);
        LOGGER.debugf("error=%s", error);
        return List.of(error);
    }

}
