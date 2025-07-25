package com.github.streamshub.console.api.support;

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
@Constraint(validatedBy = {
    StringEnumeration.StringValidator.class,
    StringEnumeration.StringListValidator.class
})
@Documented
public @interface StringEnumeration {
    String message() default "list contains an invalid value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] allowedValues() default {};

    Class<? extends Enum<?>> enumeration() default VoidEnum.class; // NOSONAR

    String source() default "";

    enum VoidEnum {
    }

    abstract static class Validator<T> implements ConstraintValidator<StringEnumeration, T> {
        final Set<String> allowedValues = new HashSet<>();

        @Override
        public void initialize(StringEnumeration annotation) {
            for (Enum<?> e : annotation.enumeration().getEnumConstants()) {
                allowedValues.add(e.toString());
            }

            if (allowedValues.isEmpty()) {
                allowedValues.addAll(Arrays.asList(annotation.allowedValues()));
            }
        }
    }

    static class StringListValidator extends Validator<List<String>> {
        @Override
        public boolean isValid(List<String> value, ConstraintValidatorContext context) {
            return Optional.ofNullable(value)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .allMatch(allowedValues::contains);
        }
    }

    static class StringValidator extends Validator<String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return Optional.ofNullable(value)
                .map(allowedValues::contains)
                // Nulls are valid
                .orElse(Boolean.TRUE);
        }
    }
}
