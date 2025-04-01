package com.github.streamshub.systemtests.resources;

import com.github.streamshub.console.api.v1alpha1.Console;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.function.Consumer;

public class ConsoleType implements ResourceType<Console> {

    private final MixedOperation<Console, KubernetesResourceList<Console>, Resource<Console>> client;

    public ConsoleType() {
        this.client = KubeResourceManager.get().kubeClient().getClient().resources(Console.class);
    }


    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return client;
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(Console.class);
    }

    @Override
    public void create(Console resource) {
        client.inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void update(Console resource) {
        client.inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    @Override
    public void delete(Console resource) {
        this.client.inNamespace(resource.getMetadata().getNamespace()).resource(resource).delete();
    }

    @Override
    public void replace(Console resource, Consumer<Console> editor) {
        Console toBeUpdated = client.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    @Override
    public boolean isReady(Console resource) {
        // TODO: check readiness
        return resource != null;
    }

    @Override
    public boolean isDeleted(Console resource) {
        return resource == null;
    }
}
