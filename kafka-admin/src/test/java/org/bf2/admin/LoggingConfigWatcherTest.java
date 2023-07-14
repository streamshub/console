package org.bf2.admin;

import org.jboss.logmanager.LogContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingConfigWatcherTest {

    LogContext context = LogContext.getLogContext();
    File override;
    LoggingConfigWatcher watcher;

    @BeforeEach
    void setup() {
        context.getLogger("").setLevel(Level.INFO);
        context.getLogger("org.bf2").setLevel(null);
        override = createTempFile();
        watcher = buildWatcher(override);
    }

    @ParameterizedTest
    @CsvSource({
        "'',        quarkus.log.level, INFO, DEBUG",
        "'org.bf2', quarkus.log.category.\"org.bf2\".level, INHERIT, DEBUG"
    })
    void testOriginalLevelRestoredWhenOverrideDeleted(String logger, String property, String originalLevel, String newLevel) throws Exception {
        Level originalLevelValue = "INHERIT".equals(originalLevel) ? null : context.getLevelForName(originalLevel);
        context.getLogger(logger).setLevel(originalLevelValue);
        AtomicInteger loopCount = new AtomicInteger(0);

        boolean dirExists = watcher.watchConfigOverride(override.toPath(), ws -> {
            switch (loopCount.incrementAndGet()) {
                case 1:
                    // Trigger a modification event
                    writeString(override, String.format("%s=%s\nignored=anything\\n", property, newLevel));
                    break;
                case 2:
                    delete(override);
                    break;
                default:
                    return null;
            }

            // Longer poll time to account for potentially slow CI environment
            return ws.poll(15, TimeUnit.SECONDS);
        });

        assertTrue(dirExists);
        assertEquals(3, loopCount.get());
        assertFalse(override.exists());
        assertTrue(watcher.overriddenLoggers.isEmpty(), () -> "overriddenLoggers not empty: " + watcher.overriddenLoggers);
        assertEquals(originalLevelValue, context.getLogger(logger).getLevel());
    }
    @ParameterizedTest
    @CsvSource({
        "'',        quarkus.log.level",
        "'org.bf2', quarkus.log.category.\"org.bf2\".level"
    })
    void testLevelUpdatedAndOriginalLevelPreserved(String logger, String property) throws Exception {
        context.getLogger(logger).setLevel(Level.INFO);
        AtomicInteger loopCount = new AtomicInteger(0);

        boolean dirExists = watcher.watchConfigOverride(override.toPath(), ws -> {
            if (loopCount.incrementAndGet() > 1) {
                return null;
            }

            // Trigger a modification event
            writeString(override, String.format("%s=%s\nignored=anything\n", property, "DEBUG"));
            return ws.poll(15, TimeUnit.SECONDS);
        });

        assertTrue(dirExists);
        assertEquals("DEBUG", context.getLogger(logger).getLevel().getName());
        assertEquals(Level.INFO, watcher.overriddenLoggers.get(logger));
    }

    @Test
    void testRunnableCompletesWhenWatcherStopped() throws Exception {
        AtomicInteger loopCount = new AtomicInteger(0);

        boolean dirExists = watcher.watchConfigOverride(override.toPath(), ws -> {
            loopCount.incrementAndGet();
            watcher.stop(null); // Argument not used
            return ws.poll(1, TimeUnit.SECONDS);
        });

        assertTrue(dirExists);
        assertEquals(1, loopCount.get());
        assertTrue(watcher.shutdown);
        assertThrows(ClosedWatchServiceException.class, watcher.watchService::poll);
    }

    static LoggingConfigWatcher buildWatcher(File override) {
        LoggingConfigWatcher watcher = new LoggingConfigWatcher();
        watcher.loggingConfigOverride = Optional.of(override.getAbsolutePath());
        try {
            watcher.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return watcher;
    }

    static File createTempFile() {
        File override;

        try {
            override = File.createTempFile("log-config-", ".properties");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        override.deleteOnExit();
        return override;
    }

    static void writeString(File file, String value) {
        try {
            Files.writeString(file.toPath(), value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void delete(File file) {
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
