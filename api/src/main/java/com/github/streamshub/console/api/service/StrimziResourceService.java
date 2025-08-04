package com.github.streamshub.console.api.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.support.RootCause;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.quarkus.cache.CacheResult;
import io.strimzi.api.kafka.model.connect.KafkaConnect;
import io.strimzi.api.kafka.model.connector.KafkaConnector;
import io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2;

@ApplicationScoped
public class StrimziResourceService {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient k8s;

    @Inject
    ConsoleConfig consoleConfig;

    @CacheResult(cacheName = "strimzi-connect-custom-resource")
    public CompletionStage<Optional<KafkaConnect>> getKafkaConnect(String namespace, String name) {
        return getResource(KafkaConnect.class, namespace, name);
    }

    @CacheResult(cacheName = "strimzi-connector-custom-resource")
    public CompletionStage<Optional<KafkaConnector>> getKafkaConnector(String namespace, String name) {
        return getResource(KafkaConnector.class, namespace, name);
    }

    @CacheResult(cacheName = "strimzi-mm2-custom-resource")
    public CompletionStage<Optional<KafkaMirrorMaker2>> getKafkaMirrorMaker2(String namespace, String name) {
        return getResource(KafkaMirrorMaker2.class, namespace, name);
    }

    private <C extends HasMetadata> CompletionStage<Optional<C>> getResource(Class<C> type, String namespace, String name) {
        if (!consoleConfig.getKubernetes().isEnabled() || namespace == null) {
            return CompletableFuture.completedStage(Optional.empty());
        }

        return CompletableFuture.completedStage(null)
            .thenApplyAsync(nothing -> {
                try {
                    C resource = k8s.resources(type)
                            .inNamespace(namespace)
                            .withName(name)
                            .get();

                    return Optional.ofNullable(resource);
                } catch (KubernetesClientException e) {
                    logger.warnf("Failed to fetch Strimzi %s resource %s/%s: %s",
                            type.getSimpleName(),
                            namespace,
                            name,
                            RootCause.of(e).orElse(e));
                }

                return Optional.empty();
            });
    }
}
