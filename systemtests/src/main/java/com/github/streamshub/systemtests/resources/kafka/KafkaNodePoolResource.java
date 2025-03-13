package com.github.streamshub.systemtests.resources.kafka;

import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.utils.resources.kafka.KafkaNodePoolUtils;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;

import java.util.function.Consumer;

import static com.github.streamshub.systemtests.utils.resources.kafka.KafkaNodePoolUtils.kafkaNodePoolClient;

public class KafkaNodePoolResource implements ResourceType<KafkaNodePool> {

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return null;
    }

    @Override
    public String getKind() {
        return ResourceKinds.KAFKA_NODE_POOL;
    }

    @Override
    public void create(KafkaNodePool resource) {
        kafkaNodePoolClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(KafkaNodePool resource) {
        kafkaNodePoolClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).delete();
    }

    @Override
    public void replace(KafkaNodePool kafkaNodePool, Consumer<KafkaNodePool> consumer) {

    }

    @Override
    public boolean isReady(KafkaNodePool resource) {
        return KafkaNodePoolUtils.getKafkaNodePool(resource.getMetadata().getNamespace(), resource.getMetadata().getName()) != null;
    }

    @Override
    public boolean isDeleted(KafkaNodePool kafkaNodePool) {
        return false;
    }

    @Override
    public void update(KafkaNodePool resource) {
        kafkaNodePoolClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }
}
