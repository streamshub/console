package com.github.streamshub.console.api.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

public interface UncheckedIO<R> {

    R call() throws IOException;

    static <R> R call(UncheckedIO<R> io, Supplier<String> exceptionMessage) {
        try {
            return io.call();
        } catch (IOException e) {
            throw new UncheckedIOException(exceptionMessage.get(), e);
        }
    }
}
