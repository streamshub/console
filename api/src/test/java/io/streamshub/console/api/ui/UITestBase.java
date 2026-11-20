package io.streamshub.console.api.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;

import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@ExtendWith(PlaywrightFailureHandler.class)
abstract class UITestBase {

    static final Path TRACE_DIR = Path.of("target", "playwright-traces");

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL home;

    /** Set (by reflection) from {@link PlaywrightFailureHandler} when the test fails. */
    volatile boolean testFailed;

    private boolean tracing;

    @BeforeEach
    void playwrightSetup() {
        // Sets context.setDefaultTimeout(..) and PlaywrightAssertions.setDefaultAssertionTimeout(..)
        PlaywrightTimeouts.configure(context);

        testFailed = false;
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true));
        tracing = true;
    }

    @AfterEach
    void playwrightTeardown(TestInfo info) {
        if (!tracing) {
            return;
        }
        tracing = false;

        Tracing.StopOptions stop = new Tracing.StopOptions();
        if (testFailed) {
            stop.setPath(traceFile(info));
        }
        // Without a path the trace is discarded
        context.tracing().stop(stop);
    }

    private static Path traceFile(TestInfo info) {
        String name = info.getTestClass().map(Class::getSimpleName).orElse("UnknownClass")
                + "-" + info.getTestMethod().map(Method::getName).orElse("unknownMethod");
        try {
            Files.createDirectories(TRACE_DIR);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return TRACE_DIR.resolve(name.replaceAll("[^A-Za-z0-9._-]", "_") + ".zip");
    }

    static Pattern ci(String text) {
        return Pattern.compile(text, Pattern.CASE_INSENSITIVE);
    }
}
