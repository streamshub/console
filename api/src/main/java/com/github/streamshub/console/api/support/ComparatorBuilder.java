package com.github.streamshub.console.api.support;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

public class ComparatorBuilder<T> {

    final BiFunction<String, Boolean, Comparator<T>> comparatorSource;
    final Comparator<T> defaultComparator;

    public ComparatorBuilder(BiFunction<String, Boolean, Comparator<T>> comparatorSource,
            Comparator<T> defaultComparator) {
        this.comparatorSource = comparatorSource;
        this.defaultComparator = defaultComparator;
    }

    public Comparator<T> fromSort(List<String> sortEntries) {
        return sortEntries.stream()
                .map(this::fieldToComparator)
                .filter(Objects::nonNull)
                // Reduce to a single composite comparator
                .reduce(Comparator::thenComparing)
                // Always sort by the default comparator (by ID) for stability
                .map(comparator -> comparator.thenComparing(defaultComparator))
                // When no sort requested, order by default comparator (ID) for stability
                .orElse(defaultComparator);
    }

    /**
     * Filter out any unknown sort keys from the given list
     */
    public List<String> knownSortKeys(List<String> sortEntries) {
        return sortEntries.stream()
                .filter(field -> fieldToComparator(field) != null)
                .toList();
    }

    Comparator<T> fieldToComparator(String field) {
        boolean desc = field.startsWith("-");

        if (desc) {
            field = field.substring(1);
        }

        return comparatorSource.apply(field, desc);
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

    public static <T, U extends Comparable<U>> Comparator<T> nullable(Function<T, U> source) {
        return comparing(source, Comparator.nullsLast(Comparable::compareTo));
    }
}
