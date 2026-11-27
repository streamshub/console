package io.streamshub.console.api.service;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;

import io.streamshub.console.api.support.Holder;
import io.streamshub.console.kafka.systemtest.KroxyliciousCrdTestProfile;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaClusterBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(KroxyliciousCrdTestProfile.class)
class KroxyliciousResourceServiceIT {

    @Inject
    KubernetesClient client;

    @Inject
    @Named("VirtualKafkaClusterInformer")
    Holder<SharedIndexInformer<VirtualKafkaCluster>> vkcInformer;

    @Test
    void informerPicksUpCreatedVirtualKafkaCluster() {
        var vkc = new VirtualKafkaClusterBuilder()
                .withNewMetadata().withName("test-vkc").withNamespace("default").endMetadata()
                .withNewSpec()
                    .withNewProxyRef().withName("test-proxy").endProxyRef()
                    .withNewTargetKafkaServiceRef().withName("test-service").endTargetKafkaServiceRef()
                    .addNewIngress().withNewIngressRef().withName("test-ingress").endIngressRef().endIngress()
                .endSpec()
                .build();

        client.resource(vkc).create();

        await().atMost(Duration.ofSeconds(10)).until(() ->
                vkcInformer.get().getStore().list().stream()
                        .anyMatch(v -> Cache.metaNamespaceKeyFunc(v).equals(Cache.metaNamespaceKeyFunc(vkc))));
    }
}
