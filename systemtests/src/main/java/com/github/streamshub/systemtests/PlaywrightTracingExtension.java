package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Ensures every {@code @Test} method gets its own tracing segment.
 *
 * <p>Runs before each test method body (after any {@code @BeforeAll}/{@code @BeforeEach}), and
 * discards whatever tracing has accumulated so far - whether that's {@code @BeforeAll} setup
 * actions or a previous, already-handled test in the same class - before starting a fresh
 * segment for the upcoming test. This keeps a failing test's saved trace scoped to that test
 * alone, even when the {@link TestCaseConfig} (and its underlying browser/page) is shared across
 * every test in the class.</p>
 */
public class PlaywrightTracingExtension implements BeforeTestExecutionCallback {
    private static final Logger LOGGER = LogWrapper.getLogger(PlaywrightTracingExtension.class);

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        LOGGER.debug("Scoping tracing segment to test [{}]", extensionContext.getDisplayName());
        Utils.findTestCaseConfig(extensionContext).ifPresent(TestCaseConfig::restartTracing);
    }
}
