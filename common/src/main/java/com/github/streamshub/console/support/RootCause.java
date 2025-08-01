package com.github.streamshub.console.support;

import java.util.Optional;

/**
 * Utility to find the root cause of a throwable
 */
public final class RootCause {

    private RootCause() {
    }

    /**
     * Utility to find the root cause of a throwable.
     *
     * @param thrown the Throwable, possibly null
     * @return an optional containing the root cause of {@code thrown}, or empty
     *         when it was {@code null}.
     */
    public static Optional<Throwable> of(Throwable thrown) {
        if (thrown == null) {
            return Optional.empty();
        }

        Throwable rootCause = thrown;

        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        return Optional.of(rootCause);
    }
}
