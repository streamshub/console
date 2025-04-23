package com.github.streamshub.systemtests.resourcetypes;

import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicStatus;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaTopicType implements ResourceType<KafkaTopic> {

    public static MixedOperation<KafkaTopic, KubernetesResourceList<KafkaTopic>, Resource<KafkaTopic>> kafkaTopicClient() {
        return ResourceUtils.getKubeResourceClient(KafkaTopic.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaTopicClient();
    }

    @Override
    public String getKind() {
        return KafkaTopic.RESOURCE_KIND;
    }

    @Override
    public void create(KafkaTopic kafkaTopic) {
        kafkaTopicClient().resource(kafkaTopic).create();
    }

    @Override
    public void update(KafkaTopic kafkaTopic) {
        kafkaTopicClient().resource(kafkaTopic).update();
    }

    @Override
    public void delete(KafkaTopic kafkaTopic) {
        kafkaTopicClient().resource(kafkaTopic).delete();
    }

    @Override
    public void replace(KafkaTopic kafkaTopic, Consumer<KafkaTopic> consumer) {
        KafkaTopic toBeReplaced = kafkaTopicClient().inNamespace(kafkaTopic.getMetadata().getNamespace()).withName(kafkaTopic.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaTopic kafkaTopic) {
        KafkaTopicStatus kafkaTopicStatus = kafkaTopicClient().inNamespace(kafkaTopic.getMetadata().getNamespace()).resource(kafkaTopic).get().getStatus();
        Optional<Condition> readyCondition = kafkaTopicStatus.getConditions().stream().filter(condition -> condition.getType().equals("Ready")).findFirst();
        return readyCondition.map(condition -> condition.getStatus().equals("True")).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaTopic kafkaTopic) {
        return kafkaTopicClient().inNamespace(kafkaTopic.getMetadata().getNamespace()).withName(kafkaTopic.getMetadata().getName()).get() == null;
    }
}
