package org.bf2.admin;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;
import org.jboss.logmanager.LogContext;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class LoggingConfigWatcher {

    private static final Logger LOGGER = Logger.getLogger(LoggingConfigWatcher.class);

    private static final String RECONFIG_FILE_ADDED = "Log configuration override found: %s";
    private static final String RECONFIG_FILE_MODIFIED = "Reconfiguration triggered; reason: log configuration has been modified: %s";
    private static final String RECONFIG_FILE_MISSING = "Reconfiguration triggered; reason: log configuration file is no longer present: %s";

    private static final String ROOT_CONFIG = "quarkus.log.level";
    private static final Pattern CATEGORY_CONFIG = Pattern.compile("^quarkus\\.log\\.category\\.\"([^\"]+?)\"\\.level$");

    @FunctionalInterface
    interface WatchKeyLookup {
        WatchKey apply(WatchService source) throws InterruptedException;
    }

    private static class Inherit extends Level {
        private static final long serialVersionUID = 1L;
        Inherit() {
            super("INHERIT", -1);
        }
    }

    private static final Level INHERIT = new Inherit();

    @Inject
    ManagedExecutor executor;

    @ConfigProperty(name = "logging.config.override")
    Optional<String> loggingConfigOverride;

    volatile boolean shutdown = false;

    WatchService watchService;

    Map<String, Level> overriddenLoggers = new HashMap<>();

    public void start(@Observes StartupEvent event) {
        loggingConfigOverride.ifPresentOrElse(this::startFileWatch, () -> LOGGER.info("No log config files set to monitor"));
    }

    public void stop(@Observes ShutdownEvent event) {
        this.shutdown = true;

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOGGER.warn("Exception closing WatchService: {}", e.getMessage(), e);
            }
        }
    }

    void startFileWatch(String loggingConfigOverride) {
        Path configOverride = Path.of(loggingConfigOverride);
        LOGGER.infof("Monitoring logging configuration override file: %s", configOverride);

        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        executor.submit(() -> {
            while (!shutdown) {
                try {
                    if (!watchConfigOverride(configOverride, WatchService::take)) {
                        Thread.sleep(Duration.ofSeconds(30).toMillis());
                    }
                }  catch (InterruptedException e) {
                    LOGGER.warnf("WatchService interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    boolean watchConfigOverride(Path configOverride, WatchKeyLookup lookup) throws InterruptedException {
        Path directory = configOverride.getParent();
        Path fileName = configOverride.getFileName();
        File watchedDirectory = directory.toFile();

        if (!watchedDirectory.isDirectory()) {
            return false;
        }

        try {
            directory.register(watchService,
                          StandardWatchEventKinds.ENTRY_CREATE,
                          StandardWatchEventKinds.ENTRY_DELETE,
                          StandardWatchEventKinds.ENTRY_MODIFY);

            if (configOverride.toFile().exists()) {
                handleFileEvent(StandardWatchEventKinds.ENTRY_CREATE, configOverride);
                handleFileEvent(StandardWatchEventKinds.ENTRY_MODIFY, configOverride);
            }

            WatchKey key;

            while (watchedDirectory.isDirectory() && (key = lookup.apply(watchService)) != null) {
                key.pollEvents()
                    .stream()
                    .filter(event -> fileName.equals(event.context()))
                    .forEach(event -> handleFileEvent(event.kind(), configOverride));

                key.reset();
            }
        } catch (NoSuchFileException e) {
            LOGGER.debugf("Logging configuration override directory does not exist: {}", e.getFile());
        } catch (ClosedWatchServiceException e) {
            LOGGER.info("WatchService closed");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return watchedDirectory.isDirectory();
    }

    void handleFileEvent(Kind<?> kind, Path configOverride) {
        if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
            LOGGER.infof(RECONFIG_FILE_ADDED, configOverride);
        } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
            LOGGER.infof(RECONFIG_FILE_MISSING, configOverride);
            delete();
        } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
            LOGGER.infof(RECONFIG_FILE_MODIFIED, configOverride);
            modify(configOverride);
        }
    }

    private void delete() {
        LogContext context = LogContext.getLogContext();

        overriddenLoggers.forEach((category, originalLevel) -> {
            org.jboss.logmanager.Logger logger = context.getLogger(category);
            LOGGER.infof("Restoring original log level for logger %s: %s",
                     category.isEmpty() ? "ROOT" : category,
                     originalLevel);

            if (originalLevel instanceof Inherit) {
                originalLevel = null;
            }

            logger.setLevel(originalLevel);
        });

        overriddenLoggers.clear();
    }

    private void modify(Path file) {
        Properties properties = new Properties();

        try (InputStream stream = Files.newInputStream(file)) {
            properties.load(stream);
        } catch (IOException e) {
            LOGGER.warn("File {} cannot be read", file, e);
            return;
        }

        LogContext context = LogContext.getLogContext();

        properties.entrySet()
            .stream()
            .filter(this::isLogLevelConfiguration)
            .map(entry -> toLoggerLevel(context, entry.getKey().toString(), entry.getValue().toString()))
            .forEach(override -> {
                org.jboss.logmanager.Logger logger = override.getKey();
                String category = logger.getName();
                Level level = override.getValue();
                Level originalLevel = logger.getLevel() == null ? INHERIT : logger.getLevel();

                LOGGER.infof("Overriding log level for category %s: %s => %s",
                         category.isEmpty() ? "ROOT" : category,
                         originalLevel,
                         level);

                // Save the original so that it can be restored if the override is removed
                overriddenLoggers.computeIfAbsent(category, k -> originalLevel);

                // TODO -- min-level handling? min-level is compile-time optimization
                // See https://github.com/quarkusio/quarkus/blob/2.3.0.Final/core/runtime/src/main/java/io/quarkus/runtime/logging/LoggingSetupRecorder.java#L92
                // TODO -- Handle other category properties (besides .level)
                // See https://quarkus.io/guides/logging#logging-categories
                logger.setLevel(level);
            });
    }

    private boolean isLogLevelConfiguration(Map.Entry<Object, Object> property) {
        Object key = property.getKey();

        if (key instanceof String && property.getValue() instanceof String) {
            String propertyName = (String) key;

            if (ROOT_CONFIG.equals(propertyName)) {
                return true;
            }

            return CATEGORY_CONFIG.matcher(propertyName).matches();
        }

        return false;
    }

    private Map.Entry<org.jboss.logmanager.Logger, Level> toLoggerLevel(LogContext context, String key, String levelName) {
        String category;

        if (ROOT_CONFIG.equals(key)) {
            category = "";
        } else {
            Matcher m = CATEGORY_CONFIG.matcher(key);
            m.find();
            category = m.group(1);
        }

        Level level = context.getLevelForName(levelName);
        return Map.entry(context.getLogger(category), level);
    }
}
