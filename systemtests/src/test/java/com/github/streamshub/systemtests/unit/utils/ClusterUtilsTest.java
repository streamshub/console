package com.github.streamshub.systemtests.unit.utils;

import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.executor.ExecResult;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ClusterUtilsTest {

    private static final KubeCmdClient MOCKED_CMD_CLIENT = mock(KubeCmdClient.class);
    private static final ExecResult MOCKED_EXEC_RESULT = mock(ExecResult.class);
    private static MockedStatic<KubeResourceManager> staticMockedKubeResourceManager;
    private static final KubeResourceManager MOCKED_KUBE_RESOURCE_MANAGER = mock(KubeResourceManager.class);

    @BeforeEach
    void setup() {
        staticMockedKubeResourceManager = mockStatic(KubeResourceManager.class);
        staticMockedKubeResourceManager.when(KubeResourceManager::get).thenReturn(MOCKED_KUBE_RESOURCE_MANAGER);
        when(MOCKED_KUBE_RESOURCE_MANAGER.kubeCmdClient()).thenReturn(MOCKED_CMD_CLIENT);
        when(MOCKED_CMD_CLIENT.exec(false, "cluster-info")).thenReturn(MOCKED_EXEC_RESULT);
    }

    @AfterEach
    void tearDown() {
        staticMockedKubeResourceManager.close();
    }

    static Stream<Arguments> clusterHealthTestCases() {
        return Stream.of(
            Arguments.of(true, "Kubernetes control plane is running at https://127.0.0.1", false),
            Arguments.of(false, "Kubernetes control plane is running at localhost", true),
            Arguments.of(true, "Some other output", true),
            Arguments.of(true, "Unhandled Error. Something went wrong", true),
            Arguments.of(true, "error: connection refused", true)
        );
    }

    @ParameterizedTest
    @MethodSource("clusterHealthTestCases")
    void testCheckClusterHealth(boolean exitStatus, String output, boolean shouldThrow) {
        when(MOCKED_EXEC_RESULT.exitStatus()).thenReturn(exitStatus);
        when(MOCKED_EXEC_RESULT.out()).thenReturn(output);

        if (shouldThrow) {
            assertThrows(ClusterUnreachableException.class, ClusterUtils::checkClusterHealth);
        } else {
            assertDoesNotThrow(ClusterUtils::checkClusterHealth);
        }
    }

    @Test
    void testIsOcp() {
        ExecResult methodCall = KubeResourceManager.get().kubeCmdClient().exec(false, "api-versions");
        when(methodCall).thenReturn(new ExecResult(0, "openshift.io version 1.2.3", ""));
        assertTrue(ClusterUtils.isOcp());

        when(methodCall).thenReturn(new ExecResult(0, "not openshift", ""));
        assertFalse(ClusterUtils.isOcp());

        when(methodCall).thenReturn(new ExecResult(1, "not openshift", ""));
        assertFalse(ClusterUtils.isOcp());
    }
}
