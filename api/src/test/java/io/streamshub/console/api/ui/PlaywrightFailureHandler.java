package io.streamshub.console.api.ui;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.lang.reflect.Field;

class PlaywrightFailureHandler implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        markFailed(context);
        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
        markFailed(context);
        throw throwable;
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
        markFailed(context);
        throw throwable;
    }

    private static void markFailed(ExtensionContext context) {
        context.getTestInstance().ifPresent(instance -> {
            for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Field field = type.getDeclaredField("testFailed");
                    field.setAccessible(true);
                    field.setBoolean(instance, true);
                    return;
                } catch (NoSuchFieldException e) {
                    // keep looking in the superclass
                } catch (IllegalAccessException e) {
                    return;
                }
            }
        });
    }
}
