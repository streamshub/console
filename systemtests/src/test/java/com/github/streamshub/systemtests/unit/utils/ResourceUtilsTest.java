package com.github.streamshub.systemtests.unit.utils;

import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceUtilsTest {

    private static MockedStatic<KubeResourceManager> staticMockKubeResourceManager;
    private static final KubeResourceManager MOCKED_KUBE_RESOURCE_MANAGER = mock(KubeResourceManager.class);
    private static final KubeClient MOCKED_KUBE_CLIENT = mock(KubeClient.class);



    @BeforeEach
    void setUp() {
        staticMockKubeResourceManager = mockStatic(KubeResourceManager.class);
        staticMockKubeResourceManager.when(KubeResourceManager::get).thenReturn(MOCKED_KUBE_RESOURCE_MANAGER);
        when(MOCKED_KUBE_RESOURCE_MANAGER.kubeClient()).thenReturn(MOCKED_KUBE_CLIENT);
    }

    @AfterEach
    void tearDown() {
        staticMockKubeResourceManager.close();
    }

    @Test
    void testGetNamespacedKubeResource() {
        final KubernetesClient mockedK8sClient = mock(KubernetesClient.class);
        final MixedOperation<Pod, KubernetesResourceList<Pod>, Resource<Pod>> mockedMixedOp = mock(MixedOperation.class);
        final NonNamespaceOperation<Pod, KubernetesResourceList<Pod>, Resource<Pod>> mockedNonNsOp = mock(NonNamespaceOperation.class);
        final Resource<Pod> mockedPodResource = mock(Resource.class);
        final Pod mockedPod = mock(Pod.class);

        when(MOCKED_KUBE_CLIENT.getClient()).thenReturn(mockedK8sClient);

        when(mockedK8sClient.resources(Pod.class)).thenReturn(mockedMixedOp);
        when(mockedMixedOp.inNamespace(anyString())).thenReturn(mockedNonNsOp);
        when(mockedNonNsOp.withName(anyString())).thenReturn(mockedPodResource);
        when(mockedPodResource.get()).thenReturn(mockedPod);

        // Call the method under test
        Pod result = ResourceUtils.getKubeResource(Pod.class, "my-namespace", "my-pod");

        // Verify the interactions and assertions
        verify(mockedK8sClient).resources(Pod.class);
        verify(mockedMixedOp).inNamespace("my-namespace");
        verify(mockedNonNsOp).withName("my-pod");
        verify(mockedPodResource).get();
        assertNotNull(result);
        assertEquals(mockedPod, result);
    }

    @Test
    void testGetNamespacelessKubeResource() {
        final KubernetesClient mockedK8sClient = mock(KubernetesClient.class);
        final MixedOperation<Namespace, KubernetesResourceList<Namespace>, Resource<Namespace>> mockedMixedOp = mock(MixedOperation.class);
        final Resource<Namespace> mockedNsResource = mock(Resource.class);
        final Namespace mockedNs = mock(Namespace.class);

        when(MOCKED_KUBE_CLIENT.getClient()).thenReturn(mockedK8sClient);

        when(mockedK8sClient.resources(Namespace.class)).thenReturn(mockedMixedOp);
        when(mockedMixedOp.withName(anyString())).thenReturn(mockedNsResource);
        when(mockedNsResource.get()).thenReturn(mockedNs);

        // Call the method under test
        Namespace result = ResourceUtils.getKubeResource(Namespace.class, "my-ns");

        // Verify the interactions and assertions
        verify(mockedK8sClient).resources(Namespace.class);
        verify(mockedMixedOp).withName("my-ns");
        verify(mockedNsResource).get();
        assertNotNull(result);
        assertEquals(mockedNs, result);
    }
}
