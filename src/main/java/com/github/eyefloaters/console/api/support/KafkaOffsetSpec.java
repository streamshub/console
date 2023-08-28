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

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = KafkaOffsetSpec.Validator.class)
@Documented
public @interface KafkaOffsetSpec {

    public static final String EARLIEST = "earliest";
    public static final String LATEST = "latest";
    public static final String MAX_TIMESTAMP = "maxTimestamp";

    String message() default "must be one of [ earliest, latest, maxTimestamp ] or a valid UTC ISO timestamp.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    ErrorCategory category();

    String source() default "";

    static class Validator implements ConstraintValidator<KafkaOffsetSpec, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }

            return switch (value) {
                case EARLIEST      -> true;
                case LATEST        -> true;
                case MAX_TIMESTAMP -> true;
                default            -> validTimestamp(value);
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
    }
}
