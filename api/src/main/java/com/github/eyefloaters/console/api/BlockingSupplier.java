package com.github.eyefloaters.console.api;

import java.util.function.Supplier;

import org.apache.kafka.common.KafkaFuture;

public class BlockingSupplier {

    private BlockingSupplier() {
        // No instances
    }

    public static <T> T get(Supplier<KafkaFuture<T>> source) {
        return source.get()
                .toCompletionStage()
                .toCompletableFuture()
                .join();
    }

}
