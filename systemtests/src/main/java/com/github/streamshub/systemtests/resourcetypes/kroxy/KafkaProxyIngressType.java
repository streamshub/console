package com.github.streamshub.systemtests.resourcetypes.kroxy;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kroxylicious.kubernetes.api.common.Condition;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngress;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaProxyIngressType implements ResourceType<KafkaProxyIngress> {

    public static MixedOperation<KafkaProxyIngress, KubernetesResourceList<KafkaProxyIngress>, Resource<KafkaProxyIngress>> kafkaProxyClient() {
        return ResourceUtils.getKubeResourceClient(KafkaProxyIngress.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kafkaProxyClient();
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(KafkaProxyIngress.class);
    }

    @Override
    public void create(KafkaProxyIngress kafkaProxyIngress) {
        kafkaProxyClient().resource(kafkaProxyIngress).create();
    }

    @Override
    public void update(KafkaProxyIngress kafkaProxyIngress) {
        kafkaProxyClient().resource(kafkaProxyIngress).update();
    }

    @Override
    public void delete(KafkaProxyIngress kafkaProxyIngress) {
        kafkaProxyClient().resource(kafkaProxyIngress).delete();
    }

    @Override
    public void replace(KafkaProxyIngress kafkaProxyIngress, Consumer<KafkaProxyIngress> consumer) {
        KafkaProxyIngress toBeReplaced = kafkaProxyClient().inNamespace(kafkaProxyIngress.getMetadata().getNamespace()).withName(kafkaProxyIngress.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaProxyIngress kafkaProxyIngress) {
        KafkaProxyIngress deployedKafkaProxyIngress = ResourceUtils.getKubeResource(KafkaProxyIngress.class, kafkaProxyIngress.getMetadata().getNamespace(), kafkaProxyIngress.getMetadata().getName());
        Optional<Condition> condition = deployedKafkaProxyIngress.getStatus().getConditions().stream().filter(c -> c.getType().equals(Condition.Type.ResolvedRefs)).findFirst();
        return condition.map(c -> c.getStatus().equals(Condition.Status.TRUE)).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaProxyIngress kafkaProxyIngress) {
        return ResourceUtils.getKubeResource(KafkaProxyIngress.class, kafkaProxyIngress.getMetadata().getNamespace(), kafkaProxyIngress.getMetadata().getName()) == null;
    }
}
