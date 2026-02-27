package com.github.streamshub.systemtests.resourcetypes.kroxy;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.common.Condition;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaProxyType implements ResourceType<KafkaProxy> {

    public static MixedOperation<KafkaProxy, KubernetesResourceList<KafkaProxy>, Resource<KafkaProxy>> kafkaProxyClient() {
        return ResourceUtils.getKubeResourceClient(KafkaProxy.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaProxyClient();
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(KafkaProxy.class);
    }

    @Override
    public void create(KafkaProxy kafkaProxy) {
        kafkaProxyClient().resource(kafkaProxy).create();
    }

    @Override
    public void update(KafkaProxy kafkaProxy) {
        kafkaProxyClient().resource(kafkaProxy).update();
    }

    @Override
    public void delete(KafkaProxy kafkaProxy) {
        kafkaProxyClient().resource(kafkaProxy).delete();
    }

    @Override
    public void replace(KafkaProxy kafkaProxy, Consumer<KafkaProxy> consumer) {
        KafkaProxy toBeReplaced = kafkaProxyClient().inNamespace(kafkaProxy.getMetadata().getNamespace()).withName(kafkaProxy.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaProxy kafkaProxy) {
        KafkaProxy deployedKafkaProxy = ResourceUtils.getKubeResource(KafkaProxy.class, kafkaProxy.getMetadata().getNamespace(), kafkaProxy.getMetadata().getName());
        Optional<Condition> condition = deployedKafkaProxy.getStatus().getConditions().stream().filter(c -> c.getType().equals(Condition.Type.Ready)).findFirst();
        return condition.map(c -> c.getStatus().equals(Condition.Status.TRUE)).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaProxy kafkaProxy) {
        return ResourceUtils.getKubeResource(KafkaProxy.class, kafkaProxy.getMetadata().getNamespace(), kafkaProxy.getMetadata().getName()) == null;
    }
}
