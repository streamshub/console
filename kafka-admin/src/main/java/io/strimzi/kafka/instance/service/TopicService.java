package io.strimzi.kafka.instance.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;

import io.strimzi.kafka.instance.model.ConfigEntry;
import io.strimzi.kafka.instance.model.Either;
import io.strimzi.kafka.instance.model.NewTopic;
import io.strimzi.kafka.instance.model.Topic;

@ApplicationScoped
public class TopicService {

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    ConfigService configService;

    public CompletionStage<NewTopic> createTopic(NewTopic topic) {
        Admin adminClient = clientSupplier.get();
        String topicName = topic.getName();
        org.apache.kafka.clients.admin.NewTopic newTopic;

        if (topic.getReplicasAssignments() != null) {
            newTopic = new org.apache.kafka.clients.admin.NewTopic(
                    topicName,
                    topic.getReplicasAssignments());
        } else {
            newTopic = new org.apache.kafka.clients.admin.NewTopic(
                    topicName,
                    Optional.ofNullable(topic.getNumPartitions()),
                    Optional.ofNullable(topic.getReplicationFactor()));
        }

        newTopic.configs(topic.getConfigs());

        CreateTopicsResult result = adminClient.createTopics(List.of(newTopic));

        return result.all()
                .thenApply(nothing -> NewTopic.fromKafkaModel(topicName, result))
                .toCompletionStage();
    }

    public CompletionStage<List<Topic>> listTopics(boolean listInternal, Set<String> includes) {
        Admin adminClient = clientSupplier.get();

        return adminClient.listTopics(new ListTopicsOptions().listInternal(listInternal))
                .listings()
                .thenApply(list -> list.stream().map(Topic::fromTopicListing).toList())
                .toCompletionStage()
                .thenCompose(list -> augmentTopicList(adminClient, list, includes))
                .thenApply(list -> list.stream().sorted(Comparator.comparing(Topic::getName)).toList());
    }

    public CompletionStage<Topic> describeTopic(String topicName, Set<String> includes) {
        Admin adminClient = clientSupplier.get();

        return describeTopics(adminClient, List.of(topicName))
            .thenApply(result -> result.get(topicName))
            .thenApply(result -> {
                if (result.isPrimaryPresent()) {
                    return result.getPrimary();
                }
                throw new CompletionException(result.getAlternate());
            })
            .thenCompose(topic -> {
                CompletableFuture<Topic> promise = new CompletableFuture<>();

                if (includes.contains("configs")) {
                    List<ConfigResource> keys = List.of(new ConfigResource(ConfigResource.Type.TOPIC, topicName));
                    configService.describeConfigs(adminClient, keys)
                        .thenApply(configs -> {
                            configs.forEach((name, either) -> topic.addConfigs(either));
                            promise.complete(topic);
                            return true; // Match `completeExceptionally`
                        })
                        .exceptionally(promise::completeExceptionally);
                } else {
                    promise.complete(topic);
                }

                return promise;
            });
    }

    public CompletionStage<Map<String, ConfigEntry>> describeConfigs(String topicName) {
        return configService.describeConfigs(ConfigResource.Type.TOPIC, topicName);
    }

    public CompletionStage<Void> deleteTopics(String... topicNames) {
        Admin adminClient = clientSupplier.get();

        var pendingDeletes = adminClient.deleteTopics(Arrays.asList(topicNames))
                .topicNameValues()
                .values()
                .stream()
                .map(KafkaFuture::toCompletionStage)
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingDeletes);
    }

    CompletionStage<List<Topic>> augmentTopicList(Admin adminClient, List<Topic> list, Set<String> includes) {
        Map<String, Topic> topics = list.stream().collect(Collectors.toMap(Topic::getName, Function.identity()));
        CompletableFuture<Void> configPromise = new CompletableFuture<>();

        if (includes.contains("configs")) {
            List<ConfigResource> keys = topics.keySet().stream().map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name)).toList();
            configService.describeConfigs(adminClient, keys)
                .thenApply(configs -> {
                    configs.forEach((name, either) -> topics.get(name).addConfigs(either));
                    configPromise.complete(null);
                    return true; // Match `completeExceptionally`
                })
                .exceptionally(configPromise::completeExceptionally);
        } else {
            configPromise.complete(null);
        }

        CompletableFuture<Void> describePromise = new CompletableFuture<>();

        if (includes.contains("partitions") || includes.contains("authorizedOperations")) {
            describeTopics(adminClient, topics.keySet())
                .thenApply(descriptions -> {
                    descriptions.forEach((name, either) -> {
                        if (includes.contains("partitions")) {
                            topics.get(name).addPartitions(either);
                        }
                        if (includes.contains("authorizedOperations")) {
                            topics.get(name).addAuthorizedOperations(either);
                        }
                    });
                    describePromise.complete(null);
                    return true; // Match `completeExceptionally`
                })
                .exceptionally(describePromise::completeExceptionally);
        } else {
            describePromise.complete(null);
        }

        return CompletableFuture.allOf(configPromise, describePromise).thenApply(nothing -> list);
    }

    CompletionStage<Map<String, Either<Topic, Throwable>>> describeTopics(Admin adminClient, Collection<String> topicNames) {
        Map<String, Either<Topic, Throwable>> result = new LinkedHashMap<>(topicNames.size());

        var pendingDescribes = adminClient.describeTopics(topicNames)
                .topicNameValues()
                .entrySet()
                .stream()
                .map(entry ->
                    entry.getValue().whenComplete((description, error) ->
                        result.put(entry.getKey(), either(description, error))))
                .map(KafkaFuture::toCompletionStage)
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        var promise = new CompletableFuture<Map<String, Either<Topic, Throwable>>>();

        CompletableFuture.allOf(pendingDescribes)
                .thenApply(nothing -> promise.complete(result))
                .exceptionally(promise::completeExceptionally);

        return promise;
    }

    Either<Topic, Throwable> either(org.apache.kafka.clients.admin.TopicDescription description, Throwable error) {
        Either<Topic, Throwable> either;

        if (error != null) {
            either = Either.ofAlternate(error);
        } else {
            either = Either.of(Topic.fromTopicDescription(description));
        }

        return either;
    }

}
