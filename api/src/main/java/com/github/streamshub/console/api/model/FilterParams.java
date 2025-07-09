package com.github.streamshub.console.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.streamshub.console.api.support.FetchFilterPredicate;

public abstract class FilterParams {

    private Map<Class<?>, List<Predicate<?>>> predicates = new HashMap<>();

    protected <T> void addPredicate(Class<T> type, Predicate<T> predicate) {
        predicates.computeIfAbsent(type, k -> new ArrayList<>()).add(predicate);
    }

    protected <T> void maybeAddPredicate(FetchFilter filter, Class<T> type, Function<String, Object> operandParser, Function<T, Object> fieldSource) {
        if (filter != null) {
            addPredicate(type, new FetchFilterPredicate<>(filter, operandParser, fieldSource));
        }
    }

    protected <T> void maybeAddPredicate(String name, FetchFilter filter, Class<T> type, Function<T, Object> fieldSource) {
        if (filter != null) {
            addPredicate(type, new FetchFilterPredicate<>(name, filter, fieldSource));
        }
    }

    protected <T> void maybeAddPredicate(FetchFilter filter, Class<T> type, Function<T, Object> fieldSource) {
        maybeAddPredicate(null, filter, type, fieldSource);
    }

    protected abstract void buildPredicates();

    @SuppressWarnings("unchecked")
    public <T> List<Predicate<T>> getPredicates(Class<T> type) {
        buildPredicates();

        return Optional.ofNullable(predicates.get(type))
            .map(List::stream)
            .orElseGet(Stream::empty)
            .map(p -> (Predicate<T>) p)
            .toList();
    }

    public static FilterParams none() {
        return new None();
    }

    private static class None extends FilterParams {
        @Override
        protected void buildPredicates() {
            // No-op
        }
    }
}
