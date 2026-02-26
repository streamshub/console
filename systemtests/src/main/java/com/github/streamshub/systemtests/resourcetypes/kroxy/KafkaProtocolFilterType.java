package com.github.streamshub.systemtests.resourcetypes.kroxy;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProtocolFilter;
import io.kroxylicious.kubernetes.api.common.Condition;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaProtocolFilterType implements ResourceType<KafkaProtocolFilter> {

    public static MixedOperation<KafkaProtocolFilter, KubernetesResourceList<KafkaProtocolFilter>, Resource<KafkaProtocolFilter>> kafkaProtocolClient() {
        return ResourceUtils.getKubeResourceClient(KafkaProtocolFilter.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaProtocolClient();
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(KafkaProtocolFilter.class);
    }

    @Override
    public void create(KafkaProtocolFilter kafkaProtocolFilter) {
        kafkaProtocolClient().resource(kafkaProtocolFilter).create();
    }

    @Override
    public void update(KafkaProtocolFilter kafkaProtocolFilter) {
        kafkaProtocolClient().resource(kafkaProtocolFilter).update();
    }

    @Override
    public void delete(KafkaProtocolFilter kafkaProtocolFilter) {
        kafkaProtocolClient().resource(kafkaProtocolFilter).delete();
    }

    @Override
    public void replace(KafkaProtocolFilter kafkaProtocolFilter, Consumer<KafkaProtocolFilter> consumer) {
        KafkaProtocolFilter toBeReplaced = kafkaProtocolClient().inNamespace(kafkaProtocolFilter.getMetadata().getNamespace()).withName(kafkaProtocolFilter.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaProtocolFilter kafkaProtocolFilter) {
        KafkaProtocolFilter deployedkafkaProtocolFilter = ResourceUtils.getKubeResource(KafkaProtocolFilter.class, kafkaProtocolFilter.getMetadata().getNamespace(), kafkaProtocolFilter.getMetadata().getName());
        Optional<Condition> condition = deployedkafkaProtocolFilter.getStatus().getConditions().stream().filter(c -> c.getType().equals(Condition.Type.ResolvedRefs)).findFirst();
        return condition.map(c -> c.getStatus().equals(Condition.Status.TRUE)).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaProtocolFilter kafkaProtocolFilter) {
        return ResourceUtils.getKubeResource(KafkaProtocolFilter.class, kafkaProtocolFilter.getMetadata().getNamespace(), kafkaProtocolFilter.getMetadata().getName()) == null;
    }
}
