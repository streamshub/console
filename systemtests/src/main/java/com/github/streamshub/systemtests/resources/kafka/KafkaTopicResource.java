package com.github.streamshub.systemtests.resources.kafka;

import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.utils.resources.ResourceManagerUtils;
import com.github.streamshub.systemtests.utils.resources.kafka.KafkaTopicUtils;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

import static com.github.streamshub.systemtests.utils.resources.kafka.KafkaTopicUtils.kafkaTopicClient;

public class KafkaTopicResource implements ResourceType<KafkaTopic> {

    private static final Logger LOGGER = LogManager.getLogger(KafkaTopicResource.class);

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return KafkaTopicUtils.kafkaTopicClient();
    }

    @Override
    public String getKind() {
        return ResourceKinds.KAFKA_TOPIC;
    }

    @Override
    public void create(KafkaTopic resource) {
        kafkaTopicClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(KafkaTopic resource) {
        setFinalizersInTopicToNull(resource);
        kafkaTopicClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    @Override
    public void replace(KafkaTopic kafkaTopic, Consumer<KafkaTopic> consumer) {

    }

    @Override
    public boolean isReady(KafkaTopic resource) {
        ResourceManagerUtils.waitForResourceStatusReady(kafkaTopicClient(), resource);
        return true;
    }

    @Override
    public boolean isDeleted(KafkaTopic resource) {
        return KafkaTopicUtils.getTopic(resource.getMetadata().getNamespace(), resource.getMetadata().getName()) == null;
    }

    @Override
    public void update(KafkaTopic resource) {
        kafkaTopicClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    public void setFinalizersInTopicToNull(KafkaTopic resource) {
        KafkaTopic topic = KafkaTopicUtils.getTopic(resource.getMetadata().getNamespace(), resource.getMetadata().getName());
        if (topic != null && !topic.getMetadata().getFinalizers().isEmpty()) {
            LOGGER.info("Setting finalizers in KafkaTopic: {}/{} to null", resource.getMetadata().getNamespace(), resource.getMetadata().getName());
            replace(resource, kt -> kt.getMetadata().setFinalizers(null));
        } else {
            LOGGER.warn("Topic is not available {}/{}", resource.getMetadata().getNamespace(), resource.getMetadata().getName());
        }
    }
}
