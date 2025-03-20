package com.github.streamshub.systemtests.unit;

import com.github.streamshub.systemtests.TestExecutionWatcher;
import com.github.streamshub.systemtests.logs.TestLogCollector;
import io.skodjob.testframe.clients.KubeClusterException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestExecutionWatcherUT {
    private static final Logger LOGGER = LogManager.getLogger(TestExecutionWatcherUT.class);
    private final TestExecutionWatcher mockWatcher = new TestExecutionWatcher();
    private final TestLogCollector mockTestLogCollector = mock(TestLogCollector.class);
    private final ExtensionContext mockContext = mock(ExtensionContext.class);

    private static final Throwable RUNTIME_EXCEPTION = new RuntimeException("Test error happened during runtime");
    private static final Throwable KUBECLUSTER_EXCEPTION = new KubeClusterException(new Throwable("Some KubeClusterException occurred"));
    private static final Throwable TEST_ABORTED_EXCEPTION = new TestAbortedException("Test was aborted");
    private static final Throwable ARRAY_OOB_EXCEPTION = new ArrayIndexOutOfBoundsException("Array index out of bounds exception occurred");

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        ReflectionTestUtils.setField(mockWatcher, "logCollector", mockTestLogCollector);
        when(mockContext.getRequiredTestClass()).thenReturn((Class) String.class);
        when(mockContext.getRequiredTestMethod()).thenReturn(String.class.getDeclaredMethod("toString"));
        doNothing().when(mockTestLogCollector).collectLogs(anyString(), anyString());
    }

    @Test
    void testHandleTestExecutionException() throws Exception {
        testHandleMethod("handleTestExecutionException", RUNTIME_EXCEPTION, true, 2);
        testHandleMethod("handleTestExecutionException", ARRAY_OOB_EXCEPTION, true, 2);
        testHandleMethod("handleTestExecutionException", KUBECLUSTER_EXCEPTION, false, 2);
        testHandleMethod("handleTestExecutionException", TEST_ABORTED_EXCEPTION, false, 2);
    }

    @Test
    void testHandleBeforeAllMethodExecutionException() throws Exception {
        testHandleMethod("handleBeforeAllMethodExecutionException", RUNTIME_EXCEPTION, true, 1);
        testHandleMethod("handleBeforeAllMethodExecutionException", ARRAY_OOB_EXCEPTION, true, 1);
        testHandleMethod("handleBeforeAllMethodExecutionException", TEST_ABORTED_EXCEPTION, false, 1);
        testHandleMethod("handleBeforeAllMethodExecutionException", KUBECLUSTER_EXCEPTION, false, 1);
    }

    @Test
    void testHandleBeforeEachMethodExecutionException() throws Exception {
        testHandleMethod("handleBeforeEachMethodExecutionException", RUNTIME_EXCEPTION, true, 2);
        testHandleMethod("handleBeforeEachMethodExecutionException", KUBECLUSTER_EXCEPTION, false, 2);
    }

    @Test
    void testHandleAfterEachMethodExecutionException() throws Exception {
        testHandleMethod("handleAfterEachMethodExecutionException", RUNTIME_EXCEPTION, true, 2);
        testHandleMethod("handleAfterEachMethodExecutionException", KUBECLUSTER_EXCEPTION, false, 2);
    }

    @Test
    void testHandleAfterAllMethodExecutionException() throws Exception {
        testHandleMethod("handleAfterAllMethodExecutionException", RUNTIME_EXCEPTION, true, 1);
        testHandleMethod("handleAfterAllMethodExecutionException", KUBECLUSTER_EXCEPTION, false, 1);
    }

    private void testHandleMethod(String methodName, Throwable exception, boolean shouldCollect, Integer parameterCount) throws Exception {
        Method method = TestExecutionWatcher.class.getDeclaredMethod(methodName, ExtensionContext.class, Throwable.class);
        LOGGER.info("Testing method: " + method.getName());
        try {
            method.invoke(mockWatcher, mockContext, exception);
        } catch (Throwable ignored) {
            LOGGER.debug("Ignoring expected exception");
        }

        switch (parameterCount == null ? 0 : parameterCount) {
            case 0: verify(mockTestLogCollector, times(shouldCollect ? 1 : 0)).collectLogs();
                break;
            case 1: verify(mockTestLogCollector, times(shouldCollect ? 1 : 0)).collectLogs(any());
                break;
            case 2: verify(mockTestLogCollector, times(shouldCollect ? 1 : 0)).collectLogs(any(), any());
                break;
        }
        clearInvocations(mockTestLogCollector);
    }
}