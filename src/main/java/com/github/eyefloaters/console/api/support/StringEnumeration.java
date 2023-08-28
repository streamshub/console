package com.github.eyefloaters.console.api.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StringEnumeration.Validator.class)
@Documented
public @interface StringEnumeration {
    String message() default "list contains an invalid value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    ErrorCategory category();

    String[] allowedValues();

    String source() default "";

    static class Validator implements ConstraintValidator<StringEnumeration, List<String>> {

        final Set<String> allowedValues = new HashSet<>();

        @Override
        public void initialize(StringEnumeration annotation) {
            allowedValues.addAll(Arrays.asList(annotation.allowedValues()));
        }

        @Override
        public boolean isValid(List<String> value, ConstraintValidatorContext context) {
            return Optional.ofNullable(value)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .allMatch(allowedValues::contains);
        }
    }
}
