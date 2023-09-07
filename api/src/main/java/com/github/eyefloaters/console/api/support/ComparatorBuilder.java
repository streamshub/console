package com.github.eyefloaters.console.api.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ComparatorBuilder<T> {

    final BiFunction<String, Boolean, Comparator<T>> comparatorSource;
    final Comparator<T> defaultComparator;

    public ComparatorBuilder(BiFunction<String, Boolean, Comparator<T>> comparatorSource,
            Comparator<T> defaultComparator) {
        this.comparatorSource = comparatorSource;
        this.defaultComparator = defaultComparator;
    }

    public Comparator<T> fromSort(String sortParam) {
        return Optional.ofNullable(sortParam)
                .map(sort -> sort.split(","))
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(field -> {
                    boolean desc = field.startsWith("-");

                    if (desc) {
                        field = field.substring(1);
                    }

                    return comparatorSource.apply(field, desc);
                })
                .filter(Objects::nonNull)
                // Reduce to a single composite comparator
                .reduce(Comparator::thenComparing)
                // Always sort by the default comparator (by ID) for stability
                .map(comparator -> comparator.thenComparing(defaultComparator))
                // When no sort requested, order by default comparator (ID) for stability
                .orElse(defaultComparator);
    }

    public static <T> Map<String, Map<Boolean, Comparator<T>>> bidirectional(Map<String, Comparator<T>> ascending) {
        return ascending.entrySet()
            .stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> Map.ofEntries(
                        Map.entry(Boolean.FALSE, entry.getValue()),
                        Map.entry(Boolean.TRUE, entry.getValue().reversed()))));
    }
}
