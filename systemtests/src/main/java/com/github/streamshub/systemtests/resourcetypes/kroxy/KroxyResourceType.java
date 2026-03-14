package com.github.streamshub.systemtests.resourcetypes.kroxy;

import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kroxylicious.kubernetes.api.common.Condition;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProtocolFilter;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngress;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaService;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.skodjob.kubetest4j.interfaces.ResourceType;

import java.util.List;
import java.util.function.Consumer;

public class KroxyResourceType<T extends HasMetadata> implements ResourceType<T> {

    private final Class<T> resourceClass;

    public KroxyResourceType(Class<T> resourceClass) {
        this.resourceClass = resourceClass;
    }

    public MixedOperation<T, KubernetesResourceList<T>, Resource<T>> kroxyResourceClient() {
        return ResourceUtils.getKubeResourceClient(resourceClass);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return kroxyResourceClient();
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(resourceClass);
    }

    @Override
    public void create(T kroxyResource) {
        kroxyResourceClient().resource(kroxyResource).create();
    }

    @Override
    public void update(T kroxyResource) {
        kroxyResourceClient().resource(kroxyResource).update();
    }

    @Override
    public void delete(T kroxyResource) {
        kroxyResourceClient().resource(kroxyResource).delete();
    }

    @Override
    public void replace(T kroxyResource, Consumer<T> consumer) {
        T toBeReplaced = kroxyResourceClient().inNamespace(kroxyResource.getMetadata().getNamespace()).withName(kroxyResource.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(T kroxyResource) {
        T resource = ResourceUtils.getKubeResource(resourceClass, kroxyResource.getMetadata().getNamespace(), kroxyResource.getMetadata().getName());

        Condition.Type conditionType;
        List<Condition> conditions;

        switch (resource) {
            case KafkaProxy r -> {
                conditionType = Condition.Type.Ready;
                conditions = r.getStatus().getConditions();
            }
            case KafkaService r -> {
                conditionType = Condition.Type.ResolvedRefs;
                conditions = r.getStatus().getConditions();
            }
            case KafkaProtocolFilter r -> {
                conditionType = Condition.Type.ResolvedRefs;
                conditions = r.getStatus().getConditions();
            }
            case KafkaProxyIngress r -> {
                conditionType = Condition.Type.ResolvedRefs;
                conditions = r.getStatus().getConditions();
            }
            case VirtualKafkaCluster r -> {
                conditionType = Condition.Type.Accepted;
                conditions = r.getStatus().getConditions();
            }
            case null, default -> throw new SetupException("Cannot get status for KroxyResourceType: " + resourceClass);
        }

        return conditions.stream()
            .filter(c -> c.getType().equals(conditionType))
            .findFirst()
            .map(c -> c.getStatus().equals(Condition.Status.TRUE))
            .orElse(false);
    }

    @Override
    public boolean isDeleted(T kroxyResource) {
        return ResourceUtils.getKubeResource(resourceClass, kroxyResource.getMetadata().getNamespace(), kroxyResource.getMetadata().getName()) == null;
    }
}
