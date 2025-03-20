package com.github.streamshub.systemtests.unit.cluster;

import com.github.streamshub.systemtests.cluster.KubeCluster;
import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.executor.ExecResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.github.streamshub.systemtests.unit.UnitTestUtils.removeEnv;
import static com.github.streamshub.systemtests.unit.UnitTestUtils.setEnv;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class KubeClusterUT {

    private static final Logger LOGGER = LogManager.getLogger(KubeCluster.class);
    private MockedStatic<Exec> mockedExec = mockStatic(Exec.class);

    @ParameterizedTest
    @ValueSource(strings = {"minikube", "oc", "kubectl", "random"})
    void testClusterDetection(String cmd) throws Exception {
        LOGGER.info("Test cluster type with set env");
        setEnv("TEST_CLUSTER_TYPE", cmd);
        assertEquals(System.getenv("TEST_CLUSTER_TYPE"), cmd);
        testClusterDetection(cmd, true);

        LOGGER.info("Now test cluster types without the env");
        removeEnv("TEST_CLUSTER_TYPE");
        assertNull(System.getenv("TEST_CLUSTER_TYPE"));
        testClusterDetection(cmd, false);

        mockedExec.close();
    }

    void testClusterDetection(String cmd, boolean setByEnv) throws Exception {
        if (setByEnv) {
            setEnv("TEST_CLUSTER_TYPE", cmd);
        }

        LOGGER.info("Detect {} cluster is up", cmd);
        testClusterDetection(cmd, true, true);

        LOGGER.info("Now fail to detect {} cluster is up", cmd);
        // Unknown clusters will start throwing right at the isAvailable, so there is no point of exhausting parameter
        if (!cmd.equals("random")) {
            assertThrows(KubeClusterException.class, () -> testClusterDetection(cmd, true, false));
        }
    }

    void testClusterDetection(String cmd, boolean cmdOnPath, boolean clusterPresent) throws KubeClusterException {
        setCmdPresent(cmd, cmdOnPath);
        setClusterPresent(cmd, clusterPresent);
        if (cmd.equals("random")) {
            Throwable th = assertThrows(KubeClusterException.class, KubeCluster::getInstance);
            assertTrue(th.getMessage().contains("Unable to find a cluster type") || th.getMessage().contains("is not a supported cluster type"));
        } else {
            KubeCluster cluster = KubeCluster.getInstance();
            assertNotNull(cluster);
            assertTrue(cluster.isClusterUp());
        }
    }

    private void setCmdPresent(String cmd, boolean present) {
        mockedExec.when(() -> Exec.isExecutableOnPath(cmd)).thenReturn(present);
    }

    private void setClusterPresent(String cmd, boolean present) {
        switch (cmd) {
            case "oc" -> {
                mockedExec.when(() -> Exec.exec(List.of(cmd, "status", "-n", "default")))
                    .thenReturn(new ExecResult(present ? 0 : 1, "", ""));
                mockedExec.when(() -> Exec.exec(List.of(cmd, "api-versions")))
                    .thenReturn(new ExecResult(present ? 0 : 1, "openshift.io", ""));
            }
            case "minikube" ->
                mockedExec.when(() -> Exec.exec(List.of("kubectl", "get", "nodes", "-o", "jsonpath='{.items[*].metadata.labels}'")))
                    .thenReturn(new ExecResult(present ? 0 : 1, "minikube.k8s.io", ""));
            case "kubectl" ->  {
                mockedExec.when(() -> Exec.exec(List.of(cmd, "cluster-info")))
                    .thenReturn(new ExecResult(present ? 0 : 1, "", ""));
                mockedExec.when(() -> Exec.exec(List.of(cmd, "api-versions")))
                    .thenReturn(new ExecResult(present ? 0 : 1, "", ""));
            }
            default -> LOGGER.info("Unexpected cluster type, will throw error");
        }
    }
}
