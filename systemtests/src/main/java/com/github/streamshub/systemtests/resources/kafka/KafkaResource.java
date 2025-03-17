package com.github.streamshub.systemtests.resources.kafka;

import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.resources.ResourceOperation;
import com.github.streamshub.systemtests.utils.resources.ResourceManagerUtils;
import com.github.streamshub.systemtests.utils.resources.kafka.KafkaUtils;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.kafka.Kafka;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

import static com.github.streamshub.systemtests.utils.resources.kafka.KafkaUtils.kafkaClient;


public class KafkaResource implements ResourceType<Kafka> {
    private static final Logger LOGGER = LogManager.getLogger(KafkaResource.class);

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return KafkaUtils.kafkaClient();
    }

    @Override
    public String getKind() {
        return ResourceKinds.KAFKA;
    }

    @Override
    public void create(Kafka resource) {
        kafkaClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(Kafka resource) {
        kafkaClient().inNamespace(resource.getMetadata().getNamespace()).withName(
            resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    @Override
    public void replace(Kafka kafka, Consumer<Kafka> consumer) {

    }

    @Override
    public boolean isReady(Kafka kafka) {
        ResourceManagerUtils.waitForResourceStatusReady(kafkaClient(), kafka, ResourceOperation.getTimeoutForResourceReadiness(ResourceKinds.KAFKA));
        return true;
    }

    @Override
    public boolean isDeleted(Kafka kafka) {
        return KafkaUtils.getKafka(kafka.getMetadata().getNamespace(), kafka.getMetadata().getName()) == null;
    }

    @Override
    public void update(Kafka resource) {
        kafkaClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }
}
