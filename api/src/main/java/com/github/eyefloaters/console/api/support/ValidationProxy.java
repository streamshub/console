package com.github.eyefloaters.console.api.support;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class ValidationProxy {

    @Inject
    Validator validator;

    /**
     * Performs validation of a bean value using the platform-provided
     * {@linkplain Validator}. If constraint violations are found, this method will
     * throw a {@linkplain ConstraintViolationException} with the violations,
     * otherwise the input value is returned.
     */
    public <T> T validate(T value) {
        var violations = validator.validate(value);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        return value;
    }

}
