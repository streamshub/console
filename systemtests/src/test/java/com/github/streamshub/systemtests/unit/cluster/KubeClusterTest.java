package com.github.streamshub.systemtests.unit.cluster;

import com.github.streamshub.systemtests.cluster.KubeCluster;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.executor.ExecResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.github.streamshub.systemtests.unit.cluster.UnitTestUtils.setEnv;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@TestVisualSeparator
class KubeClusterTest {

    private static final Logger LOGGER = LogManager.getLogger(KubeCluster.class);

    private MockedStatic<Exec> mockedExec;

    @BeforeEach
    void setUp() {
        mockedExec = mockStatic(Exec.class);
    }

    @AfterEach
    @ClearEnvironmentVariable(key = "TEST_CLUSTER_TYPE")
    void tearDown() {
        mockedExec.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"minikube", "oc", "kubectl", "random"})
    void testClusterDetectionWithEnv(String cmd) throws Exception {
        setEnv("TEST_CLUSTER_TYPE", cmd);

        LOGGER.info("Detect cluster is up");
        testClusterDetection(cmd, true, true);

        LOGGER.info("Now fail to detect cluster is up");
        // Unknown clusters will start throwing right at the isAvailable, so there is no point of exhausting parameter
        if (!cmd.equals("random")) {
            assertThrows(KubeClusterException.class, () -> testClusterDetection(cmd, true, false));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"minikube", "oc", "kubectl", "random"})
    void testClusterDetectionWithoutEnv(String cmd) throws Exception {
        LOGGER.info("Detect cluster is up");
        testClusterDetection(cmd, true, true);

        LOGGER.info("Now fail to detect cluster is up");
        // Unknown clusters will start throwing right at the isAvailable, so there is no point of exhausting parameter
        if (!cmd.equals("random")) {
            assertThrows(KubeClusterException.class, () -> testClusterDetection(cmd, true, false));
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
}
