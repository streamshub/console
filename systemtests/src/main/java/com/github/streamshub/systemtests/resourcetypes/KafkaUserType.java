package com.github.streamshub.systemtests.resourcetypes;

import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserStatus;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaUserType implements ResourceType<KafkaUser> {

    public static MixedOperation<KafkaUser, KubernetesResourceList<KafkaUser>, Resource<KafkaUser>> getKafkaUserClient() {
        return ResourceUtils.getKubeResourceClient(KafkaUser.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return getKafkaUserClient();
    }

    @Override
    public String getKind() {
        return KafkaUser.RESOURCE_KIND;
    }

    @Override
    public void create(KafkaUser kafkaUser) {
        getKafkaUserClient().inNamespace(kafkaUser.getMetadata().getNamespace()).resource(kafkaUser).create();
    }

    @Override
    public void update(KafkaUser kafkaUser) {
        getKafkaUserClient().inNamespace(kafkaUser.getMetadata().getNamespace()).resource(kafkaUser).update();
    }

    @Override
    public void delete(KafkaUser kafkaUser) {
        getKafkaUserClient().inNamespace(kafkaUser.getMetadata().getNamespace()).resource(kafkaUser).delete();
    }

    @Override
    public void replace(KafkaUser kafkaUser, Consumer<KafkaUser> consumer) {
        KafkaUser toBeReplaced = getKafkaUserClient().inNamespace(kafkaUser.getMetadata().getNamespace()).withName(kafkaUser.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaUser kafkaUser) {
        KafkaUserStatus kafkaUserStatus = getKafkaUserClient().inNamespace(kafkaUser.getMetadata().getNamespace()).resource(kafkaUser).get().getStatus();
        Optional<Condition> readyCondition = kafkaUserStatus.getConditions().stream().filter(condition -> condition.getType().equals("Ready")).findFirst();
        return readyCondition.map(condition -> condition.getStatus().equals("True")).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaUser kafkaUser) {
        return kafkaUser == null;
    }
}