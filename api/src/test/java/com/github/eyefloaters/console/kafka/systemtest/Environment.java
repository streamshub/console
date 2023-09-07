package com.github.eyefloaters.console.kafka.systemtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Environment {

    private static final String LOG_DIR_ENV = "LOG_DIR";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    public static final String TEST_CONTAINER_LABEL = "systemtest";

    public static final String SUITE_ROOT = System.getProperty("user.dir");
    public static final Path LOG_DIR = (System.getenv(LOG_DIR_ENV) == null ?
            Paths.get(SUITE_ROOT, "target", "logs") : Paths.get(System.getenv(LOG_DIR_ENV)))
            .resolve("test-run-" + DATE_FORMAT.format(LocalDateTime.now()));

    public static final Properties CONFIG;

    static {
        CONFIG = new Properties();

        try (InputStream stream = Environment.class.getResourceAsStream("/systemtests-config.properties")) {
            CONFIG.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
