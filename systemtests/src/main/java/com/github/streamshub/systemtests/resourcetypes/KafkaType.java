package com.github.streamshub.systemtests.resourcetypes;

import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaType implements ResourceType<Kafka> {

    public static MixedOperation<Kafka, KubernetesResourceList<Kafka>, Resource<Kafka>> kafkaClient() {
        return ResourceUtils.getKubeResourceClient(Kafka.class);
    }

    @Override
    public Long getTimeoutForResourceReadiness() {
        return TestFrameConstants.GLOBAL_TIMEOUT;
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaClient();
    }

    @Override
    public String getKind() {
        return Kafka.RESOURCE_KIND;
    }

    @Override
    public void create(Kafka kafka) {
        kafkaClient().inNamespace(kafka.getMetadata().getNamespace()).resource(kafka).create();
    }

    @Override
    public void update(Kafka kafka) {
        kafkaClient().inNamespace(kafka.getMetadata().getNamespace()).resource(kafka).update();
    }

    @Override
    public void delete(Kafka kafka) {
        kafkaClient().resource(kafka).delete();
    }

    @Override
    public void replace(Kafka kafka, Consumer<Kafka> consumer) {
        Kafka toBeReplaced = kafkaClient().inNamespace(kafka.getMetadata().getNamespace()).withName(kafka.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(Kafka kafka) {
        KafkaStatus kafkaStatus = kafkaClient().inNamespace(kafka.getMetadata().getNamespace()).resource(kafka).get().getStatus();
        Optional<Condition> readyCondition = kafkaStatus.getConditions().stream().filter(condition -> condition.getType().equals("Ready")).findFirst();
        return readyCondition.map(condition -> condition.getStatus().equals("True")).orElse(false);
    }

    @Override
    public boolean isDeleted(Kafka kafka) {
        return kafka == null;
    }
}
