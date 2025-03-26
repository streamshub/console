package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.logs.TestLogCollector;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

public class TestExecutionWatcher implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private final TestLogCollector logCollector = TestLogCollector.getInstance();
    private static final Logger LOGGER = LogWrapper.getLogger(TestExecutionWatcher.class);

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("{} - Exception {} has been thrown in @Test. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable.getMessage());
        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            logCollector.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[BeforeAll@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable.getMessage());
        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();

            logCollector.collectLogs(testClass);
        }
        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[BeforeEach@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable.getMessage());
        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            logCollector.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[AfterEach@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable.getMessage());
        if (!(throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            logCollector.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[AfterAll@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable.getMessage());
        if (!(throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();

            logCollector.collectLogs(testClass);
        }
        throw throwable;
    }
}
