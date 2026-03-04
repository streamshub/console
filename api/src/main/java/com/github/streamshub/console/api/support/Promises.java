package com.github.streamshub.console.api.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Promises {

    private Promises() {
        // No instances
    }

    public static <F extends Object> Collector<CompletableFuture<F>, ?, CompletableFuture<Void>> awaitingAll() {
        return Collectors.collectingAndThen(Collectors.toList(), pending ->
            CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)));
    }

    public static <T extends Object> CompletableFuture<Void> allOf(Collection<CompletableFuture<T>> pending) {
        return joinFutures(pending).thenRun(() -> { /* No-op for a Void future */ });
    }

    public static <T extends Object> CompletionStage<List<T>> joinStages(Collection<CompletionStage<T>> pending) {
        return joinStages(pending.stream());
    }

    public static <T extends Object> CompletionStage<List<T>> joinStages(Stream<CompletionStage<T>> pending) {
        var futures = pending.map(CompletionStage::toCompletableFuture).toList();
        return joinFutures(futures);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> CompletableFuture<List<T>> joinFutures(Collection<CompletableFuture<T>> pending) {
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                .thenApply(nothing -> pending.stream()
                        .map(CompletableFuture::join)
                        .toList())
                .exceptionally(error -> (List<T>) handle(error, pending));
    }

    static <T extends Object> T handle(Throwable error, Collection<CompletableFuture<T>> pending) {
        Set<Throwable> suppressed = new LinkedHashSet<>();

        pending.stream()
            .filter(CompletableFuture::isCompletedExceptionally)
            .forEach(fut -> fut.exceptionally(ex -> {
                if (ex instanceof CompletionException ce) {
                    ex = ce.getCause();
                }
                suppressed.add(ex);
                return null;
            }));

        CompletionException aggregator = new CompletionException(
                "One or more errors occurred awaiting a collection of pending results",
                error);
        suppressed.forEach(aggregator::addSuppressed);

        throw aggregator;
    }
}
