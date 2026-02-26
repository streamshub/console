package com.github.streamshub.systemtests.resourcetypes.kroxy;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kroxylicious.kubernetes.api.common.Condition;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Optional;
import java.util.function.Consumer;

public class VirtualKafkaClusterType implements ResourceType<VirtualKafkaCluster> {

    public static MixedOperation<VirtualKafkaCluster, KubernetesResourceList<VirtualKafkaCluster>, Resource<VirtualKafkaCluster>> virtualKafkaClusterClient() {
        return ResourceUtils.getKubeResourceClient(VirtualKafkaCluster.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return virtualKafkaClusterClient();
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(VirtualKafkaCluster.class);
    }

    @Override
    public void create(VirtualKafkaCluster virtualKafka) {
        virtualKafkaClusterClient().resource(virtualKafka).create();
    }

    @Override
    public void update(VirtualKafkaCluster virtualKafka) {
        virtualKafkaClusterClient().resource(virtualKafka).update();
    }

    @Override
    public void delete(VirtualKafkaCluster virtualKafka) {
        virtualKafkaClusterClient().resource(virtualKafka).delete();
    }

    @Override
    public void replace(VirtualKafkaCluster virtualKafka, Consumer<VirtualKafkaCluster> consumer) {
        VirtualKafkaCluster toBeReplaced = virtualKafkaClusterClient().inNamespace(virtualKafka.getMetadata().getNamespace()).withName(virtualKafka.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(VirtualKafkaCluster virtualKafka) {
        VirtualKafkaCluster deployedVirtualKafka = ResourceUtils.getKubeResource(VirtualKafkaCluster.class, virtualKafka.getMetadata().getNamespace(), virtualKafka.getMetadata().getName());
        Optional<Condition> condition = deployedVirtualKafka.getStatus().getConditions().stream().filter(c -> c.getType().equals(Condition.Type.Accepted)).findFirst();
        return condition.map(c -> c.getStatus().equals(Condition.Status.TRUE)).orElse(false);
    }

    @Override
    public boolean isDeleted(VirtualKafkaCluster virtualKafka) {
        return ResourceUtils.getKubeResource(VirtualKafkaCluster.class, virtualKafka.getMetadata().getNamespace(), virtualKafka.getMetadata().getName()) == null;
    }
}
