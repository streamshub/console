package com.github.streamshub.console.api.support;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Like {@linkplain java.util.Optional Optional}, but non-final so it may be
 * used as a CDI type.
 *
 * @param <T> the type of value
 * @see {@link java.util.Optional}
 */
public class Holder<T> implements Supplier<T> {

    private static final Holder<?> EMPTY = new Holder<>(null);
    private final T value;

    private Holder(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> Holder<T> empty() {
        return (Holder<T>) EMPTY;
    }

    public static <T> Holder<T> of(T value) {
        return new Holder<>(value);
    }

    /**
     * If a value is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a value is present, otherwise {@code false}
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @throws NullPointerException if value is present and the given action is
     *         {@code null}
     */
    public void ifPresent(Consumer<? super T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @apiNote
     * The preferred alternative to this method is {@link #orElseThrow()}.
     *
     * @return the non-{@code null} value described by this {@code Optional}
     * @throws NoSuchElementException if no value is present
     */
    @Override
    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * @see {@link java.util.Optional#map(Function)}
     */
    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(mapper.apply(value));
        }
    }
}
