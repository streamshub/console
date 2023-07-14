package org.bf2.admin.kafka.systemtest.listeners;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class TestExceptionCallbackListener implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(TestExceptionCallbackListener.class);

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.errorf(throwable, "Test failed at %s : %s", "Test execution", throwable.getMessage());
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.errorf(throwable, "Test failed at %s : %s", "Test before all", throwable.getMessage());
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.errorf(throwable, "Test failed at %s : %s", "Test before each", throwable.getMessage());
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.errorf(throwable, "Test failed at %s : %s", "Test after each", throwable.getMessage());
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.errorf(throwable, "Test failed at %s : %s", "Test after all", throwable.getMessage());
    }
}
