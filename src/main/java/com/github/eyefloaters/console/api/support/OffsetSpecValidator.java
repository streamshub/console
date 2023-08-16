package com.github.eyefloaters.console.api.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

public class OffsetSpecValidator implements ConstraintValidator<OffsetSpecValidator.ValidOffsetSpec, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return switch (value) {
            case "earliest"     -> true;
            case "latest"       -> true;
            case "maxTimestamp" -> true;
            default             -> validTimestamp(value);
        };
    }

    boolean validTimestamp(String value) {
        try {
            Instant.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = OffsetSpecValidator.class)
    @Documented
    public @interface ValidOffsetSpec {
        String message() default "must be one of [ earliest, latest, maxTimestamp ] or a valid UTC ISO timestamp.";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        ErrorCategory category() default ErrorCategory.INVALID_QUERY_PARAMETER;

        String source() default "";
    }
}
