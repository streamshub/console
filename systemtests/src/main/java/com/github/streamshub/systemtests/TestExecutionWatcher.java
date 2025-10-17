package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.logs.TestLogCollector;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
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
        LOGGER.error("{} - Exception [{}] has been thrown in @Test. Going to collect logs from components.",
            extensionContext.getRequiredTestClass().getSimpleName(), throwable);

        // In case of test failure, make screenshot of the last page state

        // First try if the testCase has it's own TestCaseConfig stored
        TestCaseConfig tcc = KubeResourceManager.get().getTestContext()
            .getStore(ExtensionContext.Namespace.GLOBAL)
            .get(KubeResourceManager.get().getTestContext().getUniqueId(), TestCaseConfig.class);

        // If not check in testClass for shared TestCaseConfig in beforeAll
        Optional<ExtensionContext> parentContext = extensionContext.getParent();
        if (tcc == null && parentContext.isPresent()) {
            tcc = KubeResourceManager.get().getTestContext()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .get(parentContext.get().getUniqueId(), TestCaseConfig.class);
        }

        if (tcc != null) {
            LOGGER.error("Exception has been thrown. Last known page url {}", tcc.page().url());
            PwUtils.screenshot(tcc, tcc.kafkaName(), "exception");
            PwUtils.saveTracing(tcc.context());
        } else {
            LOGGER.error("Exception has been thrown, but no TestCaseConfig instance was stored in the ExtensionContext");
        }

        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            LOG_COLLECTOR.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[BeforeAll@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();

            LOG_COLLECTOR.collectLogs(testClass);
        }
        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[BeforeEach@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof TestAbortedException || throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            LOG_COLLECTOR.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[AfterEach@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();
            final String testMethod = extensionContext.getRequiredTestMethod().getName();

            LOG_COLLECTOR.collectLogs(testClass, testMethod);
        }
        throw throwable;
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("[AfterAll@{}] Thrown Exception [{}]. Going to collect logs from components.", extensionContext.getRequiredTestClass().getSimpleName(), throwable);
        if (!(throwable instanceof ClusterUnreachableException)) {
            final String testClass = extensionContext.getRequiredTestClass().getName();

            LOG_COLLECTOR.collectLogs(testClass);
        }
        throw throwable;
    }
}
