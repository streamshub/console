package com.github.eyefloaters.console.api.support;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Path.Node;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorSource;

@Provider
public class ConstraintViolationHandler implements ExceptionMapper<ConstraintViolationException> {

    Logger logger = Logger.getLogger("com.github.eyefloaters.console.api.errors.client");

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<Error> errors = exception.getConstraintViolations()
            .stream()
            .map(violation -> {
                String occurrenceId = UUID.randomUUID().toString();
                ErrorCategory category = (ErrorCategory) violation.getConstraintDescriptor().getAttributes().get("category");
                Error error;

                if (category != null) {
                    error = new Error(category.getTitle(), violation.getMessage(), null);
                    error.setCode(category.getCode());
                    error.setStatus(String.valueOf(category.getHttpStatus().getStatusCode()));
                    error.setSource(getSource(category, lastNode(violation.getPropertyPath())));
                } else {
                    error = new Error("Invalid value", violation.getMessage(), null);
                }

                error.setId(occurrenceId);
                logger.fine(() -> "id=%s title='%s' detail='%s' source=%s".formatted(occurrenceId, error.getTitle(), error.getDetail(), error.getSource()));

                return error;
            })
            .toList();

        return Response.status(Status.BAD_REQUEST).entity(errors).build();
    }

    String lastNode(Path propertyPath) {
        return StreamSupport.stream(propertyPath.spliterator(), false)
            .reduce((first, second) -> second)
            .map(Node::toString)
            .orElse("");
    }

    ErrorSource getSource(ErrorCategory category, String property) {
        ErrorSource source = null;

        switch (category.getSource()) {
            case HEADER:
                source = new ErrorSource();
                source.setHeader(property);
                break;
            case PARAMETER:
                source = new ErrorSource();
                source.setParameter(property);
                break;
            case PAYLOAD:
                source = new ErrorSource();
                source.setPointer(property);
                break;
            default:
                break;
        }

        return source;
    }
}
