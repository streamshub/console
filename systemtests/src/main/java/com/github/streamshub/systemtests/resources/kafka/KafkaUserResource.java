package com.github.streamshub.systemtests.resources.kafka;

import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.utils.resources.ResourceManagerUtils;
import com.github.streamshub.systemtests.utils.resources.kafka.KafkaUserUtils;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.user.KafkaUser;

import java.util.function.Consumer;

import static com.github.streamshub.systemtests.utils.resources.kafka.KafkaUserUtils.kafkaUserClient;

public class KafkaUserResource implements ResourceType<KafkaUser> {

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return KafkaUserUtils.kafkaUserClient();
    }

    @Override
    public String getKind() {
        return ResourceKinds.KAFKA_USER;
    }

    @Override
    public void create(KafkaUser resource) {
        kafkaUserClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(KafkaUser resource) {
        kafkaUserClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName())
            .withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    @Override
    public void replace(KafkaUser kafkaUser, Consumer<KafkaUser> consumer) {

    }

    @Override
    public void update(KafkaUser resource) {
        kafkaUserClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    @Override
    public boolean isReady(KafkaUser resource) {
        ResourceManagerUtils.waitForResourceStatusReady(kafkaUserClient(), resource);
        return true;
    }

    @Override
    public boolean isDeleted(KafkaUser kafkaUser) {
        return KafkaUserUtils.getUser(kafkaUser.getMetadata().getNamespace(), kafkaUser.getMetadata().getName()) == null;
    }
}
