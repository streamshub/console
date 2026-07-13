package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.logs.TestLogCollector;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import java.util.Optional;

public class TestExecutionWatcher implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private static final TestLogCollector LOG_COLLECTOR = TestLogCollector.getInstance();
    private static final Logger LOGGER = LogWrapper.getLogger(TestExecutionWatcher.class);

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("Test class [{}] failed with exception [{}]",
            extensionContext.getRequiredTestClass().getSimpleName(), throwable);

        // In case of test failure, make screenshot of the last page state and save its trace
        Optional<TestCaseConfig> tcc = Utils.findTestCaseConfig(extensionContext);

        if (tcc.isPresent()) {
            LOGGER.error("Exception has been thrown. Last known page url {}", tcc.get().page().url());
            LOGGER.info("Capturing failure screenshot and saving browser trace for test [{}]", extensionContext.getDisplayName());
            PwUtils.screenshot(tcc.get(), tcc.get().kafkaName(), "exception");
            tcc.get().stopAndSaveTracing();
        } else {
            LOGGER.warn("Exception has been thrown, but no TestCaseConfig instance was stored in the ExtensionContext - skipping screenshot/trace capture");
        }

        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            LOGGER.info("Collecting component logs for failed test [{}#{}]", testClass, testMethod);
            LOG_COLLECTOR.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[BeforeAll@{}] Exception thrown [{}]", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();

            LOGGER.info("Collecting component logs for test class [{}] after @BeforeAll failure", testClass);
            LOG_COLLECTOR.collectLogs(testClass);
        }
        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[BeforeEach@{}] Exception thrown [{}]", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            LOGGER.info("Collecting component logs for test [{}#{}] after @BeforeEach failure", testClass, testMethod);
            LOG_COLLECTOR.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[AfterEach@{}] Exception thrown [{}]", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            LOGGER.info("Collecting component logs for test [{}#{}] after @AfterEach failure", testClass, testMethod);
            LOG_COLLECTOR.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[AfterAll@{}] Exception thrown [{}]", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();

            LOGGER.info("Collecting component logs for test class [{}] after @AfterAll failure", testClass);
            LOG_COLLECTOR.collectLogs(testClass);
        }
        throw throwable;
    }
}
