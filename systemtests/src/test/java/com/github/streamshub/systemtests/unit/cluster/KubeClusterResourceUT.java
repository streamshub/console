package com.github.streamshub.systemtests.unit.cluster;

import com.github.streamshub.systemtests.cluster.KubeCluster;
import com.github.streamshub.systemtests.cluster.KubeClusterResource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockStatic;

class KubeClusterResourceUT {

    @Test
    void testSingletonInstance() {
        KubeClusterResource instance1 = KubeClusterResource.getInstance();

        KubeClusterResource instance2 = KubeClusterResource.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "Singleton instances should be the same");
    }

    @Test
    void testClusterMethod() {
        try (MockedStatic<KubeCluster> mockedKubeCluster = mockStatic(KubeCluster.class)) {
            KubeCluster mockCluster = Mockito.mock(KubeCluster.class);
            mockedKubeCluster.when(KubeCluster::getInstance).thenReturn(mockCluster);

            KubeClusterResource instance = KubeClusterResource.getInstance();
            KubeCluster cluster = instance.cluster();

            assertNotNull(cluster);
            assertSame(mockCluster, cluster, "Expected the mocked KubeCluster instance");
        }
    }
}