package io.strimzi.kafka.instance;

import java.util.concurrent.Future;
import java.util.function.Supplier;

public class BlockingSupplier {

    private BlockingSupplier() {
        // No instances
    }

    public static <T> T get(Supplier<Future<T>> source) {
        try {
            return source.get().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
