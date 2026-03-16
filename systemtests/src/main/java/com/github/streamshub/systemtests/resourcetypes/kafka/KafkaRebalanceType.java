package com.github.streamshub.systemtests.resourcetypes.kafka;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;

import java.util.function.Consumer;

public class KafkaRebalanceType implements ResourceType<KafkaRebalance> {

    public static MixedOperation<KafkaRebalance, KubernetesResourceList<KafkaRebalance>, Resource<KafkaRebalance>> kafkaRebalanceClient() {
        return ResourceUtils.getKubeResourceClient(KafkaRebalance.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaRebalanceClient();
    }

    @Override
    public String getKind() {
        return KafkaRebalance.RESOURCE_KIND;
    }

    @Override
    public void create(KafkaRebalance kafkaRebalance) {
        kafkaRebalanceClient().resource(kafkaRebalance).create();
    }

    @Override
    public void update(KafkaRebalance kafkaRebalance) {
        kafkaRebalanceClient().resource(kafkaRebalance).update();
    }

    @Override
    public void delete(KafkaRebalance kafkaRebalance) {
        kafkaRebalanceClient().resource(kafkaRebalance).delete();
    }

    @Override
    public void replace(KafkaRebalance kafkaRebalance, Consumer<KafkaRebalance> consumer) {
        KafkaRebalance toBeReplaced = kafkaRebalanceClient().inNamespace(kafkaRebalance.getMetadata().getNamespace()).withName(kafkaRebalance.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaRebalance kafkaRebalance) {
        return kafkaRebalanceClient().inNamespace(kafkaRebalance.getMetadata().getNamespace()).withName(kafkaRebalance.getMetadata().getName()).get() != null;
    }

    @Override
    public boolean isDeleted(KafkaRebalance kafkaRebalance) {
        return kafkaRebalanceClient().inNamespace(kafkaRebalance.getMetadata().getNamespace()).withName(kafkaRebalance.getMetadata().getName()).get() == null;
    }
}
