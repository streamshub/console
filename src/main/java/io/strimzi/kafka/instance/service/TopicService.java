package io.strimzi.kafka.instance.service;

import static org.apache.kafka.clients.admin.NewPartitions.increaseTo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.strimzi.kafka.instance.model.Error;
import io.strimzi.kafka.instance.model.NewPartitions;
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

    public CompletionStage<List<Topic>> listTopics(boolean listInternal, List<String> includes) {
        Admin adminClient = clientSupplier.get();

        return adminClient.listTopics(new ListTopicsOptions().listInternal(listInternal))
                .listings()
                .thenApply(list -> list.stream().map(Topic::fromTopicListing).toList())
                .toCompletionStage()
                .thenCompose(list -> augmentList(adminClient, list, includes))
                .thenApply(list -> list.stream().sorted(Comparator.comparing(Topic::getName)).toList());
    }

    public CompletionStage<Topic> describeTopic(String topicName, List<String> includes) {
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

                if (includes.contains("offsets")) {
                    // TODO
                }

                return promise;
            });
    }

    public CompletionStage<Map<String, ConfigEntry>> describeConfigs(String topicName) {
        return configService.describeConfigs(ConfigResource.Type.TOPIC, topicName);
    }

    public CompletionStage<Map<String, ConfigEntry>> alterConfigs(String topicName, Map<String, ConfigEntry> configs) {
        return configService.alterConfigs(ConfigResource.Type.TOPIC, topicName, configs);
    }

    public CompletionStage<Void> createPartitions(String topicName, NewPartitions partitions) {
        Admin adminClient = clientSupplier.get();

        int totalCount = partitions.getTotalCount();
        var newAssignments = partitions.getNewAssignments();

        org.apache.kafka.clients.admin.NewPartitions newPartitions;

        if (newAssignments != null) {
            newPartitions = increaseTo(totalCount, newAssignments);
        } else {
            newPartitions = increaseTo(totalCount);
        }

        return adminClient.createPartitions(Map.of(topicName, newPartitions))
                .all()
                .toCompletionStage();
    }

    public CompletionStage<Map<String, Error>> deleteTopics(String... topicNames) {
        Admin adminClient = clientSupplier.get();
        Map<String, Error> errors = new HashMap<>();

        var pendingDeletes = adminClient.deleteTopics(Arrays.asList(topicNames))
                .topicNameValues()
                .entrySet()
                .stream()
                .map(entry -> entry.getValue().whenComplete((nothing, thrown) -> {
                    if (thrown != null) {
                        errors.put(entry.getKey(), new Error("Unable to delete topic", thrown.getMessage(), thrown));
                    }
                }))
                .map(KafkaFuture::toCompletionStage)
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingDeletes).thenApply(nothing -> errors);
    }

    CompletionStage<List<Topic>> augmentList(Admin adminClient, List<Topic> list, List<String> includes) {
        Map<String, Topic> topics = list.stream().collect(Collectors.toMap(Topic::getName, Function.identity()));
        CompletableFuture<Void> configPromise = maybeDescribeConfigs(adminClient, topics, includes);
        CompletableFuture<Void> describePromise = maybeDescribeTopics(adminClient, topics, includes);
        // TODO: maybeListOffsets - only if `includes` contains `partitions`

        return CompletableFuture.allOf(configPromise, describePromise).thenApply(nothing -> list);
    }

    CompletableFuture<Void> maybeDescribeConfigs(Admin adminClient, Map<String, Topic> topics, List<String> includes) {
        CompletableFuture<Void> promise = new CompletableFuture<>();

        if (includes.contains("configs")) {
            List<ConfigResource> keys = topics.keySet().stream().map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name)).toList();

            configService.describeConfigs(adminClient, keys)
                .thenApply(configs -> {
                    configs.forEach((name, either) -> topics.get(name).addConfigs(either));
                    promise.complete(null);
                    return true; // Match `completeExceptionally`
                })
                .exceptionally(promise::completeExceptionally);
        } else {
            promise.complete(null);
        }

        return promise;
    }

    CompletableFuture<Void> maybeDescribeTopics(Admin adminClient, Map<String, Topic> topics, List<String> includes) {
        CompletableFuture<Void> promise = new CompletableFuture<>();

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
                    promise.complete(null);
                    return true; // Match `completeExceptionally`
                })
                .exceptionally(promise::completeExceptionally);
        } else {
            promise.complete(null);
        }

        return promise;
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
