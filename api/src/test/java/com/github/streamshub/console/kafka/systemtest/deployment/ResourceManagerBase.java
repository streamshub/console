package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.testcontainers.containers.Network;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class ResourceManagerBase implements QuarkusTestResourceLifecycleManager {

    private static final Map<Class<?>, CompletableFuture<Map<String, String>>> PROPERTIES = new ConcurrentHashMap<>();
    public static final String DEPENDENCIES = ResourceManagerBase.class.getName() + ".dependencies";

    protected static final Network SHARED_NETWORK = Network.builder()
            .driver("bridge")
            .build();

    protected Map<String, String> initArgs;

    private final Set<Class<?>> dependencies = new HashSet<>();

    protected ResourceManagerBase() {
    }

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = Map.copyOf(initArgs);

        PROPERTIES.put(getClass(), new CompletableFuture<>());

        String dependsOn = initArgs.get(DEPENDENCIES);

        if (dependsOn != null) {
            Stream.of(dependsOn.split(",")).map(fqcn -> {
                try {
                    return Class.forName(fqcn);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).forEach(dependencies::add);
        }
    }

    @Override
    public Map<String, String> start() {
        Awaitility.await().atMost(Duration.ofSeconds(60)).until(() -> {
            return dependencies.stream()
                .map(dep -> getFutureProperties(dep))
                .allMatch(CompletableFuture::isDone);
        });

        Map<Class<?>, Map<String, String>> dependencyProperties = dependencies.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        dep -> getProperties(dep)));

        Map<String, String> additionalProperties;

        try {
            additionalProperties = start(dependencyProperties);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        putProperties(getClass(), additionalProperties);

        return additionalProperties;
    }

    protected Consumer<com.github.dockerjava.api.command.CreateContainerCmd> setContainerName(String name) {
        return cmd -> cmd.withName(name + '-' + UUID.randomUUID().toString());
    }

    protected abstract Map<String, String> start(Map<Class<?>, Map<String, String>> dependencyProperties)
        throws IOException;

    static Map<String, String> getProperties(Class<?> testResourceType) {
        var properties = getFutureProperties(testResourceType);
        return properties != null ? properties.join() : Collections.emptyMap();
    }

    static CompletableFuture<Map<String, String>> getFutureProperties(Class<?> testResourceType) {
        return PROPERTIES.get(testResourceType);
    }

    static void putProperties(Class<?> testResourceType, Map<String, String> properties) {
        PROPERTIES.get(testResourceType).complete(properties);
    }

}
