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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestExecutionWatcherUT {
    private static final Logger LOGGER = LogManager.getLogger(TestExecutionWatcherUT.class);
    private final TestExecutionWatcher watcher = new TestExecutionWatcher();
    private final TestLogCollector mockLogCollector = mock(TestLogCollector.class);
    private final ExtensionContext mockContext = mock(ExtensionContext.class);

    @BeforeEach
    void setUp(){
    }

    @Test
    void shouldCollectLogsOnTestExecutionException() throws Throwable {
        when(mockContext.getRequiredTestClass()).thenReturn((Class) String.class);
        when(mockContext.getRequiredTestMethod()).thenReturn(String.class.getDeclaredMethod("toString"));

        Throwable testException = new RuntimeException("Test error happened during some ST");

        try {
            watcher.handleTestExecutionException(mockContext, testException);
        } catch (Throwable ignored) {
            LOGGER.debug("ignoring exception");
        }

    }

    @Test
    void shouldNotCollectLogsForTestAbortedException() throws Throwable {
        when(mockContext.getRequiredTestClass()).thenReturn((Class) String.class);
        when(mockContext.getRequiredTestMethod()).thenReturn(String.class.getDeclaredMethod("toString"));

        Throwable abortedException = new TestAbortedException("Test aborted");

        try {
            watcher.handleTestExecutionException(mockContext, abortedException);
        } catch (Throwable ignored) {
            LOGGER.debug("ignoring exception");
        }

        verify(mockLogCollector, never()).collectLogs(anyString(), anyString());
    }

    @Test
    void shouldNotCollectLogsForKubeClusterException() throws Throwable {
        when(mockContext.getRequiredTestClass()).thenReturn((Class) String.class);
        when(mockContext.getRequiredTestMethod()).thenReturn(String.class.getDeclaredMethod("toString"));

        Throwable kubeException = new KubeClusterException(new Throwable("kubernetes error"));

        try {
            watcher.handleTestExecutionException(mockContext, kubeException);
        } catch (Throwable ignored) {
            LOGGER.debug("ignoring exception");
        }

        verify(mockLogCollector, never()).collectLogs(anyString(), anyString());
    }
}

