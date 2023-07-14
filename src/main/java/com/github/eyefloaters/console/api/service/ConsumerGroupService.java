package com.github.eyefloaters.console.api.service;

import java.util.Arrays;
import java.util.Collection;
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
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.common.KafkaFuture;

import com.github.eyefloaters.console.api.model.ConsumerGroup;
import com.github.eyefloaters.console.api.model.Either;
import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.OffsetAndMetadata;

@ApplicationScoped
public class ConsumerGroupService {

    @Inject
    Supplier<Admin> clientSupplier;

    public CompletionStage<List<ConsumerGroup>> listConsumerGroups(List<String> includes) {
        Admin adminClient = clientSupplier.get();

        return adminClient.listConsumerGroups()
            .valid()
            .toCompletionStage()
            .thenApply(groups -> groups.stream().map(ConsumerGroup::fromListing).toList())
            .thenCompose(groups -> augmentList(adminClient, groups, includes));
    }

    public CompletionStage<ConsumerGroup> describeConsumerGroup(String groupId, List<String> includes) {
        Admin adminClient = clientSupplier.get();

        return describeConsumerGroups(adminClient, List.of(groupId))
            .thenApply(groups -> groups.get(groupId))
            .thenApply(result -> {
                if (result.isPrimaryPresent()) {
                    return result.getPrimary();
                }
                throw new CompletionException(result.getAlternate());
            })
            .thenCompose(group -> {
                if (includes.contains("offsets")) {
                    return adminClient.listConsumerGroupOffsets(groupId)
                        .partitionsToOffsetAndMetadata()
                        .whenComplete((offsets, thrown) -> addOffsets(group, offsets, thrown))
                        .thenApply(offsets -> group)
                        .toCompletionStage();
                } else {
                    return CompletableFuture.completedStage(group);
                }
            });
    }

    public CompletionStage<Map<String, Map<Integer, Error>>> alterConsumerGroupOffsets(String groupId, Map<String, Map<Integer, OffsetAndMetadata>> topicOffsets) {
        var offsets = topicOffsets.entrySet()
                .stream()
                .flatMap(partitionOffsets -> {
                    String topicName = partitionOffsets.getKey();

                    return partitionOffsets.getValue().entrySet().stream()
                        .map(partitionOffset ->
                            Map.entry(
                                    new org.apache.kafka.common.TopicPartition(topicName, partitionOffset.getKey()),
                                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(
                                            partitionOffset.getValue().getOffset(),
                                            Optional.ofNullable(partitionOffset.getValue().getLeaderEpoch()),
                                            partitionOffset.getValue().getMetadata())));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Admin adminClient = clientSupplier.get();
        var results = adminClient.alterConsumerGroupOffsets(groupId, offsets);
        Map<org.apache.kafka.common.TopicPartition, CompletableFuture<Void>> offsetResults;

        offsetResults = offsets.keySet()
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        partition -> results.partitionResult(partition).toCompletionStage().toCompletableFuture()));

        Map<String, Map<Integer, Error>> errors = new LinkedHashMap<>();

        return CompletableFuture.allOf(offsetResults.values().toArray(CompletableFuture[]::new))
            .thenAccept(nothing -> offsetResults.entrySet()
                .stream()
                .filter(e -> e.getValue().isCompletedExceptionally())
                .forEach(e -> {
                    org.apache.kafka.common.TopicPartition partition = e.getKey();

                    e.getValue().exceptionally(error -> {
                        String msg = "Unable to reset offset for topic %s, partition %d".formatted(partition.topic(), partition.partition());
                        errors.computeIfAbsent(partition.topic(), k -> new LinkedHashMap<>())
                            .put(partition.partition(), new Error(msg, error.getMessage(), error));
                        return null;
                    });
                }))
            .thenApply(nothing -> errors);
    }

    public CompletionStage<Map<String, Error>> deleteConsumerGroups(String... groupIds) {
        Admin adminClient = clientSupplier.get();
        Map<String, Error> errors = new HashMap<>();

        var pendingDeletes = adminClient.deleteConsumerGroups(Arrays.asList(groupIds))
                .deletedGroups()
                .entrySet()
                .stream()
                .map(entry -> entry.getValue().whenComplete((nothing, thrown) -> {
                    if (thrown != null) {
                        errors.put(entry.getKey(), new Error("Unable to delete consumer group", thrown.getMessage(), thrown));
                    }
                }))
                .map(KafkaFuture::toCompletionStage)
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingDeletes).thenApply(nothing -> errors);
    }

    CompletionStage<List<ConsumerGroup>> augmentList(Admin adminClient, List<ConsumerGroup> list, List<String> includes) {
        Map<String, ConsumerGroup> groups = list.stream().collect(Collectors.toMap(ConsumerGroup::getGroupId, Function.identity()));
        CompletableFuture<Void> describePromise = new CompletableFuture<>();

        if (includes.contains("members") || includes.contains("coordinator") || includes.contains("authorizedOperations")) {
            describeConsumerGroups(adminClient, groups.keySet())
                .thenApply(descriptions -> {
                    descriptions.forEach((name, either) -> mergeDescriptions(groups.get(name), includes, either));
                    return describePromise.complete(null);
                })
                .exceptionally(describePromise::completeExceptionally);
        } else {
            describePromise.complete(null);
        }

        CompletableFuture<Void> offsetsPromise = new CompletableFuture<>();

        if (includes.contains("offsets")) {
            Map<String, ListConsumerGroupOffsetsSpec> groupSpecs = groups.keySet()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), key -> null));

            var offsetListResult = adminClient.listConsumerGroupOffsets(groupSpecs);
            var pendingOffsetOps = groups.keySet()
                .stream()
                .map(groupId ->
                    offsetListResult.partitionsToOffsetAndMetadata(groupId)
                        .whenComplete((offsets, thrown) ->
                            addOffsets(groups.get(groupId), offsets, thrown)))
                .map(KafkaFuture::toCompletionStage)
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(pendingOffsetOps).thenApply(nothing -> offsetsPromise.complete(null));
        } else {
            offsetsPromise.complete(null);
        }

        return CompletableFuture.allOf(describePromise, offsetsPromise).thenApply(nothing -> list);
    }

    void mergeDescriptions(ConsumerGroup group, List<String> includes, Either<ConsumerGroup, Throwable> description) {
        if (description.isPrimaryEmpty()) {
            Throwable thrown = description.getAlternate();
            Error error = new Error("Unable to describe consumer group", thrown.getMessage(), thrown);
            group.setErrors(List.of(error));
        } else {
            ConsumerGroup describedGroup = description.getPrimary();

            if (includes.contains("members")) {
                group.setMembers(describedGroup.getMembers());
            }

            if (includes.contains("coordinator")) {
                group.setCoordinator(describedGroup.getCoordinator());
            }

            if (includes.contains("authorizedOperations")) {
                group.setAuthorizedOperations(describedGroup.getAuthorizedOperations());
            }
        }
    }

    void addOffsets(ConsumerGroup group,
            Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> partitionOffsets,
            Throwable thrown) {

        if (thrown != null) {
            group.addError(new Error("Unable to list consumer group offsets", thrown.getMessage(), thrown));
        } else {
            Map<String, Map<Integer, OffsetAndMetadata>> offsets = new HashMap<>();

            partitionOffsets.forEach((topicPartition, offsetsAndMetadata) -> {
                String topic = topicPartition.topic();
                Integer partition = topicPartition.partition();

                offsets.computeIfAbsent(topic, k -> new HashMap<>())
                    .put(partition, new OffsetAndMetadata(
                            offsetsAndMetadata.offset(),
                            offsetsAndMetadata.metadata(),
                            offsetsAndMetadata.leaderEpoch().orElse(null)));
            });

            group.setOffsets(offsets);
        }
    }

    CompletionStage<Map<String, Either<ConsumerGroup, Throwable>>> describeConsumerGroups(Admin adminClient, Collection<String> groupIds) {
        Map<String, Either<ConsumerGroup, Throwable>> result = new LinkedHashMap<>(groupIds.size());

        var pendingDescribes = adminClient.describeConsumerGroups(groupIds)
                .describedGroups()
                .entrySet()
                .stream()
                .map(entry ->
                    entry.getValue().whenComplete((description, error) ->
                        result.put(
                                entry.getKey(),
                                Either.of(Optional.ofNullable(description).map(ConsumerGroup::fromDescription), error))))
                .map(KafkaFuture::toCompletionStage)
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        var promise = new CompletableFuture<Map<String, Either<ConsumerGroup, Throwable>>>();

        CompletableFuture.allOf(pendingDescribes)
                .thenApply(nothing -> promise.complete(result))
                .exceptionally(promise::completeExceptionally);

        return promise;
    }

}
