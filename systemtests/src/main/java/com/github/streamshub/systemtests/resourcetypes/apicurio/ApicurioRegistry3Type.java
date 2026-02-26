package com.github.streamshub.systemtests.resourcetypes.apicurio;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.apicurio.registry.operator.api.v1.ApicurioRegistry3;
import io.apicurio.registry.operator.api.v1.status.Condition;
import io.apicurio.registry.operator.api.v1.status.ConditionStatus;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Optional;
import java.util.function.Consumer;

public class ApicurioRegistry3Type implements ResourceType<ApicurioRegistry3> {

    public static MixedOperation<ApicurioRegistry3, KubernetesResourceList<ApicurioRegistry3>, Resource<ApicurioRegistry3>> apicurioRegistry3Client() {
        return ResourceUtils.getKubeResourceClient(ApicurioRegistry3.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return apicurioRegistry3Client();
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(ApicurioRegistry3.class);
    }

    @Override
    public void create(ApicurioRegistry3 apicurioRegistry3) {
        apicurioRegistry3Client().resource(apicurioRegistry3).create();
    }

    @Override
    public void update(ApicurioRegistry3 apicurioRegistry3) {
        apicurioRegistry3Client().resource(apicurioRegistry3).update();
    }

    @Override
    public void delete(ApicurioRegistry3 apicurioRegistry3) {
        apicurioRegistry3Client().resource(apicurioRegistry3).delete();
    }

    @Override
    public void replace(ApicurioRegistry3 apicurioRegistry3, Consumer<ApicurioRegistry3> consumer) {
        ApicurioRegistry3 toBeReplaced = apicurioRegistry3Client().inNamespace(apicurioRegistry3.getMetadata().getNamespace()).withName(apicurioRegistry3.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(ApicurioRegistry3 apicurioRegistry3) {
        ApicurioRegistry3 deployedApicurioRegistry3 = ResourceUtils.getKubeResource(ApicurioRegistry3.class, apicurioRegistry3.getMetadata().getNamespace(), apicurioRegistry3.getMetadata().getName());
        Optional<Condition> condition = deployedApicurioRegistry3.getStatus().getConditions().stream().filter(c -> c.getType().equals("Ready")).findFirst();
        return condition.map(c -> c.getStatus().equals(ConditionStatus.TRUE)).orElse(false);
    }

    @Override
    public boolean isDeleted(ApicurioRegistry3 apicurioRegistry3) {
        return ResourceUtils.getKubeResource(ApicurioRegistry3.class, apicurioRegistry3.getMetadata().getNamespace(), apicurioRegistry3.getMetadata().getName()) == null;
    }
}