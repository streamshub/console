package com.github.streamshub.systemtests.resourcetypes;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.api.kafka.model.connect.KafkaConnect;
import io.strimzi.api.kafka.model.connect.KafkaConnectStatus;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaConnectType implements ResourceType<KafkaConnect> {
    public static MixedOperation<KafkaConnect, KubernetesResourceList<KafkaConnect>, Resource<KafkaConnect>> kafkaConnectClient() {
        return ResourceUtils.getKubeResourceClient(KafkaConnect.class);
    }
    
    public String getKind() {
        return KafkaConnect.RESOURCE_KIND;
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaConnectClient();
    }
    
    @Override
    public void create(KafkaConnect kafkaConnect) {
        kafkaConnectClient().resource(kafkaConnect).create();
    }

    @Override
    public void update(KafkaConnect kafkaConnect) {
        kafkaConnectClient().resource(kafkaConnect).update();
    }

    @Override
    public void delete(KafkaConnect kafkaConnect) {
        kafkaConnectClient().resource(kafkaConnect).delete();
    }

    @Override
    public void replace(KafkaConnect kafkaConnect, Consumer<KafkaConnect> consumer) {
        KafkaConnect toBeReplaced = kafkaConnectClient().withName(kafkaConnect.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaConnect kafkaConnect) {
        KafkaConnectStatus kafkaConnectStatus = kafkaConnectClient().resource(kafkaConnect).get().getStatus();
        Optional<Condition> readyCondition = kafkaConnectStatus.getConditions().stream().filter(condition -> condition.getType().equals("Ready")).findFirst();
        return readyCondition.map(condition -> condition.getStatus().equals("True")).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaConnect kafkaConnect) {
        return kafkaConnectClient().inNamespace(kafkaConnect.getMetadata().getNamespace()).withName(kafkaConnect.getMetadata().getName()).get() == null;
    }
}
