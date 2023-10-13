package com.github.eyefloaters.console.api.errors.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.api.support.ErrorCategory.Source;

@Provider
@ApplicationScoped
public class ConstraintViolationHandler extends AbstractClientExceptionHandler<ConstraintViolationException> {

    private static final Logger LOGGER = Logger.getLogger(ConstraintViolationHandler.class);

    public ConstraintViolationHandler() {
        super(ErrorCategory.UncategorizedBadRequest.class, null, null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof ConstraintViolationException;
    }

    @Override
    protected List<Error> buildErrors(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(violation -> {
                Error error = getCategory(violation)
                    .map(category -> category.createError(violation.getMessage(), null, getSourceProperty(violation, category)))
                    .orElseGet(() -> super.category.createError(violation.getMessage(), null, null));

                LOGGER.debugf("error=%s", error);

                return error;
            })
            .toList();
    }

    Optional<ErrorCategory> getCategory(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor().getPayload()
            .stream()
            .map(ErrorCategory::get)
            .filter(ErrorCategory.class::isInstance)
            .map(ErrorCategory.class::cast)
            .findFirst();
    }

    String getSourceProperty(ConstraintViolation<?> violation, ErrorCategory category) {
        return Optional.ofNullable(violation.getConstraintDescriptor().getAttributes().get("source"))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(Predicate.not(String::isBlank))
            .orElseGet(() -> extractPath(violation.getPropertyPath(), category));
    }

    String extractPath(Path propertyPath, ErrorCategory category) {
        List<String> segments = new ArrayList<>();

        StreamSupport.stream(propertyPath.spliterator(), false)
            .map(node -> {
                if (node.isInIterable()) {
                    // Prefer the key (map entry) and default to the index (array entry)
                    return Optional.ofNullable(node.getKey())
                        .orElseGet(node::getIndex)
                        .toString();
                }

                return node.getName();
            })
            .forEach(segments::add);

        if (category.getSource() == Source.POINTER) {
            String rawPointer = "/" + String.join("/", segments);
            return rawPointer.substring(rawPointer.indexOf("/data"));
        }

        return segments.get(segments.size() - 1);
    }
}
