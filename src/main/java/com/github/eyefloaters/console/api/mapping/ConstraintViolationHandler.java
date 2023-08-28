package com.github.eyefloaters.console.api.mapping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Path.Node;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorResponse;
import com.github.eyefloaters.console.api.model.ErrorSource;
import com.github.eyefloaters.console.api.support.ErrorCategory;

@Provider
public class ConstraintViolationHandler implements ExceptionMapper<ConstraintViolationException> {

    Logger logger = Logger.getLogger("com.github.eyefloaters.console.api.errors.client");

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<Error> errors = exception.getConstraintViolations()
            .stream()
            .map(violation -> {
                Error error;

                error = Optional.ofNullable(violation.getConstraintDescriptor().getAttributes().get("category"))
                    .filter(ErrorCategory.class::isInstance)
                    .map(ErrorCategory.class::cast)
                    .map(category -> {
                        Error e = new Error(category.getTitle(), violation.getMessage(), null);
                        e.setCode(category.getCode());
                        e.setStatus(String.valueOf(category.getHttpStatus().getStatusCode()));
                        e.setSource(getSource(category, getSourceProperty(violation)));
                        return e;
                    })
                    .orElseGet(() -> new Error("Invalid value", violation.getMessage(), null));

                String id = UUID.randomUUID().toString();
                error.setId(id);
                logger.debugf("id=%s title='%s' detail='%s' source=%s", id, error.getTitle(), error.getDetail(), error.getSource());

                return error;
            })
            .toList();

        return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(errors)).build();
    }

    String getSourceProperty(ConstraintViolation<?> violation) {
        return Optional.ofNullable(violation.getConstraintDescriptor().getAttributes().get("source"))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(Predicate.not(String::isBlank))
            .orElseGet(() -> lastNode(violation.getPropertyPath()));
    }

    String lastNode(Path propertyPath) {
        return StreamSupport.stream(propertyPath.spliterator(), false)
            .reduce((first, second) -> second)
            .map(Node::toString)
            .orElse("");
    }

    ErrorSource getSource(ErrorCategory category, String property) {
        return category.getSource().errorSource(property);
    }
}
