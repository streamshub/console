package com.github.streamshub.systemtests.unit.cluster;

import com.github.streamshub.systemtests.cluster.KubeCluster;
import com.github.streamshub.systemtests.cluster.KubernetesCluster;
import com.github.streamshub.systemtests.cluster.MinikubeCluster;
import com.github.streamshub.systemtests.cluster.OpenshiftCluster;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.clients.KubeClusterException;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.executor.ExecResult;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.List;

import static com.github.streamshub.systemtests.unit.UnitTestUtils.removeEnv;
import static com.github.streamshub.systemtests.unit.UnitTestUtils.setEnv;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class KubeClusterTest {

    private static final Logger LOGGER = LogWrapper.getLogger(KubeCluster.class);
    private MockedStatic<Exec> mockedExec;

    @BeforeEach
    void setup() {
        mockedExec = mockStatic(Exec.class);
    }

    @AfterEach
    void tearDown() {
        mockedExec.close(); // Ensures static mock is deregistered after each test
    }

    @ParameterizedTest
    @ValueSource(strings = {"minikube", "oc", "kubectl"})
    void testClusterDetection(String cmd) throws Exception {
        setEnv("TEST_CLUSTER_TYPE", cmd);
        assertEquals(System.getenv("TEST_CLUSTER_TYPE"), cmd);
        testClusterDetection(cmd,  true);

        removeEnv("TEST_CLUSTER_TYPE");
        assertNull(System.getenv("TEST_CLUSTER_TYPE"));
        testClusterDetection(cmd, false);
    }

    @Test
    void testIsClusterUpFail() {
        setCmdPresent("oc", true);
        setCmdPresent("kubectl", true);
        setCmdPresent("minikube", true);
        // kubectl return exit status but fail contains

        mockedExec.when(() -> Exec.exec(List.of("kubectl", "get", "nodes", "-o", "jsonpath='{.items[*].metadata.labels}'")))
            .thenThrow(new KubeClusterException(new Throwable("fail")));
        KubeCluster cluster = new MinikubeCluster();
        assertNotNull(cluster);
        assertFalse(cluster.isClusterUp());

        mockedExec.when(() -> Exec.exec(List.of("oc", "status", "-n", "default"))).thenThrow(new KubeClusterException(new Throwable("fail")));
        cluster = new OpenshiftCluster();
        assertNotNull(cluster);
        assertFalse(cluster.isClusterUp());

        mockedExec.when(() -> Exec.exec(List.of("kubectl", "cluster-info"))).thenThrow(new KubeClusterException(new Throwable("fail")));
        cluster = new KubernetesCluster();
        assertNotNull(cluster);
        assertFalse(cluster.isClusterUp());

    }

    @Test
    void testUnsupportedKubeClusterNameInEnv() throws Exception {
        setEnv("TEST_CLUSTER_TYPE", "random");
        assertThrows(KubeClusterException.class, KubeCluster::getInstance);
    }


    void testClusterDetection(String cmd, boolean setByEnv) throws Exception {
        if (setByEnv) {
            setEnv("TEST_CLUSTER_TYPE", cmd);
        }
        // Cluster is up
        testClusterDetection(cmd, true, true);
        // Cluster is down - throw
        if (!cmd.equals("random")) {
            assertThrows(KubeClusterException.class, () -> testClusterDetection(cmd, true, false));
        }
    }

    void testClusterDetection(String cmd, boolean cmdOnPath, boolean clusterUp) throws KubeClusterException {
        setCmdPresent(cmd, cmdOnPath);
        setClusterPresent(cmd, clusterUp);

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

    private void setClusterPresent(String cmd, boolean clusterUp) {
        switch (cmd) {
            case "oc" -> {
                mockedExec.when(() -> Exec.exec(List.of(cmd, "status", "-n", "default")))
                    .thenReturn(new ExecResult(clusterUp ? 0 : 1, "", ""));
                mockedExec.when(() -> Exec.exec(List.of(cmd, "api-versions")))
                    .thenReturn(new ExecResult(clusterUp ? 0 : 1, "openshift.io", ""));
            }
            case "minikube" ->
                mockedExec.when(() -> Exec.exec(List.of("kubectl", "get", "nodes", "-o", "jsonpath='{.items[*].metadata.labels}'")))
                    .thenReturn(new ExecResult(clusterUp ? 0 : 1, "minikube.k8s.io", ""));
            case "kubectl" ->  {
                mockedExec.when(() -> Exec.exec(List.of(cmd, "cluster-info")))
                    .thenReturn(new ExecResult(clusterUp ? 0 : 1, "", ""));
                mockedExec.when(() -> Exec.exec(List.of(cmd, "api-versions")))
                    .thenReturn(new ExecResult(clusterUp ? 0 : 1, "", ""));
            }
            default -> LOGGER.info("Unexpected cluster type, will throw error");
        }
    }
}
