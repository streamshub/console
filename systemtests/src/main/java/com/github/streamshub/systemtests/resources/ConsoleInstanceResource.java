package com.github.streamshub.systemtests.resources;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.utils.resources.kubernetes.ConsoleInstanceUtils;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.skodjob.testframe.interfaces.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

import static com.github.streamshub.systemtests.utils.resources.kubernetes.ConsoleInstanceUtils.consoleInstanceClient;

public class ConsoleInstanceResource implements ResourceType<Console> {

    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceResource.class);

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return ConsoleInstanceUtils.consoleInstanceClient();
    }

    @Override
    public String getKind() {
        return ResourceKinds.CONSOLE_INSTANCE;
    }

    @Override
    public void create(Console resource) {
        consoleInstanceClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(Console resource) {
        removeFinalizers(resource);
        consoleInstanceClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).delete();
    }

    @Override
    public void update(Console resource) {
        consoleInstanceClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    public void replace(Console resource, Consumer<Console> editor) {
        Console toBeReplaced = consoleInstanceClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).get();
        editor.accept(toBeReplaced);
        this.update(toBeReplaced);
    }

    @Override
    public boolean isReady(Console resource) {
        ConsoleInstanceUtils.waitForConsoleReady(resource.getMetadata().getNamespace());
        return true;
    }

    @Override
    public boolean isDeleted(Console resource) {
        return ConsoleInstanceUtils.getConsole(resource.getMetadata().getNamespace(), resource.getMetadata().getName()) == null;
    }
    
    // -------------
    // Finalizers
    // -------------
    public void removeFinalizers(Console resource) {
        replace(resource, console -> console.getMetadata().setFinalizers(null));
    }
}
