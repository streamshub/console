package com.github.streamshub.console.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogCapture {

    private static final Logger ROOT;

    private final InMemoryLogHandler inMemoryLogHandler;

    static {
        ROOT = LogManager.getLogManager().getLogger("");
    }

    public static LogCapture none() {
        return new LogCapture();
    }

    public static LogCapture with(Predicate<LogRecord> predicate) {
        return LogCapture.with(predicate, Level.INFO);
    }

    public static LogCapture with(Predicate<LogRecord> predicate, Level logLevel) {
        LogCapture capture = new LogCapture(predicate);
        return capture.setLevel(logLevel);
    }

    private LogCapture() {
        // Capture nothing by default
        inMemoryLogHandler = new InMemoryLogHandler(r -> false);
    }

    private LogCapture(Predicate<LogRecord> predicate) {
        inMemoryLogHandler = new InMemoryLogHandler(predicate);
    }

    public void register() {
        ROOT.addHandler(inMemoryLogHandler);
    }

    public void deregister() {
        ROOT.removeHandler(inMemoryLogHandler);
    }

    private LogCapture setLevel(Level newLevel) {
        ROOT.setLevel(newLevel);
        inMemoryLogHandler.setLevel(newLevel);
        return this;
    }

    public List<LogRecord> records() {
        return inMemoryLogHandler.records;
    }

    private static class InMemoryLogHandler extends Handler {

        InMemoryLogHandler(Predicate<LogRecord> predicate) {
            if (predicate == null) {
                throw new IllegalArgumentException("Parameter 'predicate' may not be null");
            }
            setFilter(predicate::test);
            setLevel(Level.FINE);
        }

        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord rec) {
            if (!isLoggable(rec)) {
                return;
            }

            records.add(rec);
        }

        @Override
        public void flush() {
            // Nothing to flush
        }

        @Override
        public void close() throws SecurityException {
            records.clear();
        }
    }
}
