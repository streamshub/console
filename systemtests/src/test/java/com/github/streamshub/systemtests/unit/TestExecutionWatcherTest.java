package com.github.streamshub.systemtests.unit;

import com.github.streamshub.systemtests.TestExecutionWatcher;
import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.logs.TestLogCollector;
import io.skodjob.testframe.clients.KubeClusterException;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestExecutionWatcherTest {
    private static final Logger LOGGER = LogWrapper.getLogger(TestExecutionWatcherTest.class);
    private final TestExecutionWatcher mockWatcher = new TestExecutionWatcher();
    private final TestLogCollector mockTestLogCollector = mock(TestLogCollector.class);
    private final ExtensionContext mockContext = mock(ExtensionContext.class);

    private static final Throwable RUNTIME_EXCEPTION = new RuntimeException("Test error happened during runtime");
    private static final Throwable KUBE_CLUSTER_EXCEPTION = new KubeClusterException(new Throwable("Some KubeClusterException occurred"));
    private static final Throwable UNREACHABLE_CLUSTER_EXCEPTION = new ClusterUnreachableException("Failed to check cluster health");
    private static final Throwable TEST_ABORTED_EXCEPTION = new TestAbortedException("Test was aborted");

    @BeforeEach
    void setUp() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        UnitTestUtils.setField(mockWatcher, "logCollector", mockTestLogCollector);
        when(mockContext.getRequiredTestClass()).thenReturn((Class) String.class);
        when(mockContext.getRequiredTestMethod()).thenReturn(String.class.getDeclaredMethod("toString"));
        doNothing().when(mockTestLogCollector).collectLogs(anyString(), anyString());
    }

    @Test
    void testHandleTestExecutionException() throws Exception {
        final String methodName = "handleTestExecutionException";
        testHandleMethod(methodName, RUNTIME_EXCEPTION, true, 2);
        testHandleMethod(methodName, TEST_ABORTED_EXCEPTION, false, 2);
        testHandleMethod(methodName, KUBE_CLUSTER_EXCEPTION, true, 2);
        testHandleMethod(methodName, UNREACHABLE_CLUSTER_EXCEPTION, false, 2);
    }

    @Test
    void testHandleBeforeAllMethodExecutionException() throws Exception {
        final String methodName = "handleBeforeAllMethodExecutionException";
        testHandleMethod(methodName, RUNTIME_EXCEPTION, true, 1);
        testHandleMethod(methodName, TEST_ABORTED_EXCEPTION, false, 1);
        testHandleMethod(methodName, KUBE_CLUSTER_EXCEPTION, true, 1);
        testHandleMethod(methodName, UNREACHABLE_CLUSTER_EXCEPTION, false, 1);
    }

    @Test
    void testHandleBeforeEachMethodExecutionException() throws Exception {
        final String methodName = "handleBeforeEachMethodExecutionException";
        testHandleMethod(methodName, RUNTIME_EXCEPTION, true, 2);
        testHandleMethod(methodName, TEST_ABORTED_EXCEPTION, false, 2);
        testHandleMethod(methodName, KUBE_CLUSTER_EXCEPTION, true, 2);
        testHandleMethod(methodName, UNREACHABLE_CLUSTER_EXCEPTION, false, 2);
    }

    @Test
    void testHandleAfterEachMethodExecutionException() throws Exception {
        final String methodName = "handleAfterEachMethodExecutionException";
        testHandleMethod(methodName, RUNTIME_EXCEPTION, true, 2);
        testHandleMethod(methodName, TEST_ABORTED_EXCEPTION, true, 2);
        testHandleMethod(methodName, KUBE_CLUSTER_EXCEPTION, true, 2);
        testHandleMethod(methodName, UNREACHABLE_CLUSTER_EXCEPTION, false, 2);
    }

    @Test
    void testHandleAfterAllMethodExecutionException() throws Exception {
        final String methodName = "handleAfterAllMethodExecutionException";
        testHandleMethod(methodName, RUNTIME_EXCEPTION, true, 1);
        testHandleMethod(methodName, TEST_ABORTED_EXCEPTION, true, 1);
        testHandleMethod(methodName, KUBE_CLUSTER_EXCEPTION, true, 1);
        testHandleMethod(methodName, UNREACHABLE_CLUSTER_EXCEPTION, false, 1);
    }

    private void testHandleMethod(String methodName, Throwable exception, boolean shouldCollect, Integer parameterCount) throws Exception {
        Method method = TestExecutionWatcher.class.getDeclaredMethod(methodName, ExtensionContext.class, Throwable.class);
        LOGGER.info("Testing method: {}", method.getName());
        try {
            method.invoke(mockWatcher, mockContext, exception);
        } catch (Throwable ignored) {
            LOGGER.debug("Ignoring expected exception");
        }

        switch (parameterCount == null ? 0 : parameterCount) {
            case 2: verify(mockTestLogCollector, times(shouldCollect ? 1 : 0)).collectLogs(any(), any());
                break;
            case 1: verify(mockTestLogCollector, times(shouldCollect ? 1 : 0)).collectLogs(any());
                break;
            case 0: verify(mockTestLogCollector, times(shouldCollect ? 1 : 0)).collectLogs();
                break;
            default: throw new RuntimeException("Unexpected parameter count for method collectLogs ->" + parameterCount);
        }
        clearInvocations(mockTestLogCollector);
    }
}