package com.github.streamshub.console.api.errors.client;

import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.github.streamshub.console.api.support.ErrorCategory;

/**
 * Maps a Jackson {@linkplain MismatchedInputExceptionMapper} to a response.
 * This instance exists to override the mapper provided by Quarkus,
 * {@linkplain io.quarkus.resteasy.reactive.jackson.runtime.mappers.DefaultMismatchedInputException}.
 */
@Provider
@ApplicationScoped
public class MismatchedInputExceptionMapper extends AbstractClientExceptionHandler<MismatchedInputException> {

    public MismatchedInputExceptionMapper() {
        super(ErrorCategory.InvalidResource.class, "JSON document does not match target schema",
                exception -> exception.getPath().stream()
                    .map(ref -> Objects.requireNonNullElseGet(ref.getFieldName(), ref::getIndex).toString())
                    .collect(Collectors.joining("/", "/", "")));
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof MismatchedInputException;
    }

}
