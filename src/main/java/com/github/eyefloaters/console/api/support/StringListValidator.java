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

public class StringListValidator implements ConstraintValidator<StringListValidator.ValidStringList, List<String>> {

    final Set<String> allowedValues = new HashSet<>();

    @Override
    public void initialize(ValidStringList annotation) {
        allowedValues.addAll(Arrays.asList(annotation.allowedValues()));
    }

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        return Optional.ofNullable(value)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .allMatch(allowedValues::contains);
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = StringListValidator.class)
    @Documented
    public @interface ValidStringList {
        String message() default "list contains an invalid value";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        ErrorCategory category() default ErrorCategory.INVALID_QUERY_PARAMETER;

        String[] allowedValues();

        String source() default "";
    }
}
