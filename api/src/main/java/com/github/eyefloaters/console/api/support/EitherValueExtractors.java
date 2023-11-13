package com.github.eyefloaters.console.api.support;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

import com.github.eyefloaters.console.api.model.Either;

/**
 * Provides {@linkplain ValueExtractor} implementations for both the primary and
 * alternate sides of the {@linkplain Either} class. One or the other will be
 * invoked by the validation runtime depending on which side is being validated
 * based on the presence of a constraint annotation.
 */
public class EitherValueExtractors {

    private EitherValueExtractors() {
    }

    @ApplicationScoped
    public static class PrimaryEitherValueExtractor implements ValueExtractor<Either<@ExtractedValue ?, ?>> {
        @Override
        public void extractValues(Either<?, ?> originalValue, ValueReceiver receiver) {
            receiver.value(null, originalValue.getOptionalPrimary().orElse(null));
        }
    }

    @ApplicationScoped
    public static class AlternateEitherValueExtractor implements ValueExtractor<Either<?, @ExtractedValue ?>> {
        @Override
        public void extractValues(Either<?, ?> originalValue, ValueReceiver receiver) {
            receiver.value(null, originalValue.getAlternate());
        }
    }

}
