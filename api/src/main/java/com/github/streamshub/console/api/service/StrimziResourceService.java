package com.github.streamshub.console.api.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.support.RootCause;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.cache.CacheResult;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.common.Constants;
import io.strimzi.api.kafka.model.connect.KafkaConnect;
import io.strimzi.api.kafka.model.connector.KafkaConnector;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.user.KafkaUser;

@ApplicationScoped
public class StrimziResourceService {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient k8s;

    @Inject
    ConsoleConfig consoleConfig;

    public SharedIndexInformer<Kafka> informKafkas() {
        return k8s.resources(Kafka.class).inAnyNamespace().inform();
    }

    public SharedIndexInformer<KafkaNodePool> informKafkaNodePools() {
        return k8s.resources(KafkaNodePool.class).inAnyNamespace().inform();
    }

    @SuppressWarnings("unchecked")
    public <R extends KafkaTopic> SharedIndexInformer<R> informKafkaTopics() {
        return (SharedIndexInformer<R>) k8s.resources(KafkaTopicV1Beta2.class).inAnyNamespace().inform();
    }

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

    @CacheResult(cacheName = "strimzi-user-custom-resources")
    public CompletionStage<List<KafkaUser>> getKafkaUsers(String namespace, String kafkaCluster) {
        return getResources(KafkaUserV1Beta2.class, namespace, ResourceLabels.STRIMZI_CLUSTER_LABEL, kafkaCluster)
                .thenApply(users -> users.stream()
                        .map(KafkaUser.class::cast)
                        .toList());
    }

    @CacheResult(cacheName = "strimzi-user-custom-resource")
    public CompletionStage<Optional<KafkaUser>> getKafkaUser(String namespace, String name, String kafkaCluster) {
        return getResource(KafkaUserV1Beta2.class, namespace, name)
                .thenApply(user -> user
                        .filter(byKafkaCluster(kafkaCluster))
                        .map(KafkaUser.class::cast));
    }

    private <C extends HasMetadata> CompletionStage<Optional<C>> getResource(Class<C> type, String namespace, String name) {
        if (!consoleConfig.getKubernetes().isEnabled() || namespace == null) {
            return CompletableFuture.completedStage(Optional.empty());
        }

        return CompletableFuture.supplyAsync(() -> {
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
                        RootCause.of(e).orElse(e).getMessage());
            }

            return Optional.empty();
        });
    }

    private <C extends HasMetadata> CompletionStage<List<C>> getResources(Class<C> type, String namespace, String... labels) {
        if (!consoleConfig.getKubernetes().isEnabled() || namespace == null) {
            return CompletableFuture.completedStage(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return k8s.resources(type)
                        .inNamespace(namespace)
                        .withLabelSelector(fromLabels(labels))
                        .list()
                        .getItems();
            } catch (KubernetesClientException e) {
                logger.warnf("Failed to fetch Strimzi %s resources in namespace %s: %s",
                        type.getSimpleName(),
                        namespace,
                        RootCause.of(e).orElse(e).getMessage());
            }

            return Collections.emptyList();
        });
    }

    private LabelSelector fromLabels(String... labels) {
        var builder = new LabelSelectorBuilder();

        for (int i = 0; i < labels.length; i += 2) {
            builder.addToMatchLabels(labels[i], labels[i + 1]);
        }

        return builder.build();
    }

    private <T extends HasMetadata> Predicate<T> byKafkaCluster(String kafkaCluster) {
        return resource -> {
            String ownedByCluster = resource.getMetadata().getLabels().get(ResourceLabels.STRIMZI_CLUSTER_LABEL);
            return Objects.equals(ownedByCluster, kafkaCluster);
        };
    }

    /**
     * Extends KafkaTopic but overrides the Kube API version to v1beta2
     */
    @Kind("KafkaTopic")
    @Version(Constants.V1BETA2)
    @Group(Constants.RESOURCE_GROUP_NAME)
    private static class KafkaTopicV1Beta2 extends KafkaTopic {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Extends KafkaUser but overrides the Kube API version to v1beta2
     */
    @Kind("KafkaUser")
    @Version(Constants.V1BETA2)
    @Group(Constants.RESOURCE_GROUP_NAME)
    private static class KafkaUserV1Beta2 extends KafkaUser {
        private static final long serialVersionUID = 1L;
    }
}
