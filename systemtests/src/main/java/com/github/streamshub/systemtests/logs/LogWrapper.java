package com.github.streamshub.systemtests.logs;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class LogWrapper {
    private static final String STDOUT = "STDOUT";
    private static final String ROLLING_FILE = "RollingFile";

    private LogWrapper() {}

    /**
     * Configures the file appenders. Takes its settings as parameters rather than
     * reading them from {@code Environment} directly, so that {@code Environment} controls when
     * this runs
     */
    public static void configure(String testLogDir, String buildId, String consoleLogLevel, String fileLogLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setName(STDOUT)
                .setLayout(PatternLayout.newBuilder()
                    .setPattern("%d{yyyy-MM-dd HH:mm:ss}{GMT} [%thread] %highlight{%-5p} [%c{1}:%L] %m%n")
                    .build())
                .build();
        consoleAppender.start();

        RollingFileAppender rollingAppender = RollingFileAppender.newBuilder()
                .setName(ROLLING_FILE)
                .setFileName(testLogDir + "/streamshub-debug-" + buildId + ".log")
                .setFilePattern(testLogDir + "/streamshub-debug-%d{yyyy-MM-dd-HH-mm-ss}-%i.log.gz")
                .setPolicy(SizeBasedTriggeringPolicy.createPolicy("100MB"))
                .setStrategy(DefaultRolloverStrategy.newBuilder().setMax("5").build())
                .setLayout(PatternLayout.newBuilder().setPattern("%d{yyyy-MM-dd HH:mm:ss}{GMT} %-5p [%c{1}:%L] %m%n").build())
                .build();
        rollingAppender.start();

        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        loggerConfig.addAppender(consoleAppender, Level.toLevel(consoleLogLevel, Level.INFO), null);
        loggerConfig.addAppender(rollingAppender, Level.toLevel(fileLogLevel, Level.DEBUG), null);

        // Add a specific logger config to silence Netty’s internal debug
        LoggerConfig nettyLoggerConfig = new LoggerConfig("io.netty", Level.WARN, true);
        config.addLogger("io.netty", nettyLoggerConfig);

        // Silence all of Vert.x
        LoggerConfig vertxLoggerConfig = new LoggerConfig("io.vertx", Level.WARN, true);
        config.addLogger("io.vertx", vertxLoggerConfig);

        ctx.updateLoggers();

        // Root level needs to be the highest level in order to not cap file and stdout output log levels
        Configurator.setRootLevel(Level.TRACE);
    }

    public static org.apache.logging.log4j.Logger getLogger(Class<?> clazz) {
        return  LogManager.getLogger(clazz);
    }
}
