package io.streamshub.console.api.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;

@ApplicationScoped
public class KroxyliciousResourceService {

    @Inject
    KubernetesClient k8s;

    public SharedIndexInformer<VirtualKafkaCluster> informVirtualKafkaClusters() {
        return k8s.resources(VirtualKafkaCluster.class).inAnyNamespace().inform();
    }
}
