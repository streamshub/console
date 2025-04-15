package com.github.streamshub.systemtests.resourcetypes;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;

import java.util.Optional;
import java.util.function.Consumer;

public class ConsoleType implements ResourceType<Console> {

    public static MixedOperation<Console, KubernetesResourceList<Console>, Resource<Console>> consoleClient() {
        return ResourceUtils.getKubeResourceClient(Console.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return consoleClient();
    }

    @Override
    public String getKind() {
        return Console.class.getSimpleName();
    }

    @Override
    public void create(Console console) {
        consoleClient().inNamespace(console.getMetadata().getNamespace()).resource(console).create();
    }

    @Override
    public void update(Console console) {
        consoleClient().inNamespace(console.getMetadata().getNamespace()).resource(console).update();
    }

    @Override
    public void delete(Console console) {
        consoleClient().inNamespace(console.getMetadata().getNamespace()).resource(console).update();
    }

    @Override
    public void replace(Console console, Consumer<Console> consumer) {
        Console toBeReplaced = consoleClient().inNamespace(console.getMetadata().getNamespace()).withName(console.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(Console console) {
        Console deployedConsole = ResourceUtils.getKubeResource(Console.class, console.getMetadata().getNamespace(), console.getMetadata().getName());
        Optional<Condition> condition = deployedConsole.getStatus().getConditions().stream().filter(c -> c.getType().equals("Ready")).findFirst();
        return condition.map(c -> c.getStatus().equals("True")).orElse(false);
    }

    @Override
    public boolean isDeleted(Console console) {
        return ResourceUtils.getKubeResource(Console.class, console.getMetadata().getNamespace(), console.getMetadata().getName()) == null;
    }
}
