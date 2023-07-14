package com.github.eyefloaters.console.api.model;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonValue;

@Schema(hidden = true)
public class Either<P, A> {

    Optional<P> primary;
    A alternate;

    Either(Optional<P> primary, A alternate) {
        this.primary = primary;
        this.alternate = alternate;
    }

    Either(P primary, A alternate) {
        this(Optional.ofNullable(primary), alternate);
    }

    public static <P, A> Either<P, A> of(Optional<P> primary, A alternate) {
        return new Either<>(primary, alternate);
    }

    public static <P, A> Either<P, A> of(P primary) {
        return new Either<>(primary, null);
    }

    public static <P, A> Either<P, A> ofAlternate(A alternate) {
        Objects.nonNull(alternate);
        return new Either<>(null, alternate);
    }

    @JsonValue
    public Object getValue() {
        return primary.map(Object.class::cast).orElse(alternate);
    }

    public P getPrimary() {
        return primary.get();
    }

    public A getAlternate() {
        return alternate;
    }

    public boolean isPrimaryPresent() {
        return primary.isPresent();
    }

    public boolean isPrimaryEmpty() {
        return primary.isEmpty();
    }

}
