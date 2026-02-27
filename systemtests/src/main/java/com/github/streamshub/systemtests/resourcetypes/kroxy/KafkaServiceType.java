package com.github.streamshub.systemtests.resourcetypes.kroxy;


import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kroxylicious.kubernetes.api.common.Condition;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaService;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaServiceType implements ResourceType<KafkaService> {

    public static MixedOperation<KafkaService, KubernetesResourceList<KafkaService>, Resource<KafkaService>> kafkaServiceClient() {
        return ResourceUtils.getKubeResourceClient(KafkaService.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaServiceClient();
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(KafkaService.class);
    }

    @Override
    public void create(KafkaService kafkaService) {
        kafkaServiceClient().resource(kafkaService).create();
    }

    @Override
    public void update(KafkaService kafkaService) {
        kafkaServiceClient().resource(kafkaService).update();
    }

    @Override
    public void delete(KafkaService kafkaService) {
        kafkaServiceClient().resource(kafkaService).delete();
    }

    @Override
    public void replace(KafkaService kafkaService, Consumer<KafkaService> consumer) {
        KafkaService toBeReplaced = kafkaServiceClient().inNamespace(kafkaService.getMetadata().getNamespace()).withName(kafkaService.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaService kafkaService) {
        KafkaService deployedKafkaService = ResourceUtils.getKubeResource(KafkaService.class, kafkaService.getMetadata().getNamespace(), kafkaService.getMetadata().getName());
        Optional<Condition> condition = deployedKafkaService.getStatus().getConditions().stream().filter(c -> c.getType().equals(Condition.Type.ResolvedRefs)).findFirst();
        return condition.map(c -> c.getStatus().equals(Condition.Status.TRUE)).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaService kafkaService) {
        return ResourceUtils.getKubeResource(KafkaService.class, kafkaService.getMetadata().getNamespace(), kafkaService.getMetadata().getName()) == null;
    }
}
