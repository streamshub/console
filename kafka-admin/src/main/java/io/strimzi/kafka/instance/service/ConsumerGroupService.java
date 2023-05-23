package io.strimzi.kafka.instance.service;

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

import io.strimzi.kafka.instance.model.ConsumerGroup;
import io.strimzi.kafka.instance.model.Either;
import io.strimzi.kafka.instance.model.Error;
import io.strimzi.kafka.instance.model.OffsetAndMetadata;

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
                CompletableFuture<ConsumerGroup> promise = new CompletableFuture<>();

                if (includes.contains("offsets")) {
                    adminClient.listConsumerGroupOffsets(groupId)
                        .partitionsToOffsetAndMetadata()
                        .whenComplete((offsets, thrown) -> {
                            addOffsets(group, offsets, thrown);
                            promise.complete(group);
                        });
                } else {
                    promise.complete(group);
                }

                return promise;
            });
    }

    CompletionStage<List<ConsumerGroup>> augmentList(Admin adminClient, List<ConsumerGroup> list, List<String> includes) {
        Map<String, ConsumerGroup> groups = list.stream().collect(Collectors.toMap(ConsumerGroup::getGroupId, Function.identity()));
        CompletableFuture<Void> describePromise = new CompletableFuture<>();

        if (includes.contains("members") || includes.contains("coordinator") || includes.contains("authorizedOperations")) {
            describeConsumerGroups(adminClient, groups.keySet())
                .thenApply(descriptions -> {
                    descriptions.forEach((name, either) -> {
                        if (either.isPrimaryEmpty()) {
                            Throwable thrown = either.getAlternate();
                            Error error = new Error("Unable to describe consumer group", thrown.getMessage(), thrown);
                            groups.get(name).setErrors(List.of(error));
                        } else {
                            ConsumerGroup listedGroup = groups.get(name);
                            ConsumerGroup describedGroup = either.getPrimary();

                            if (includes.contains("members")) {
                                listedGroup.setMembers(describedGroup.getMembers());
                            }

                            if (includes.contains("coordinator")) {
                                listedGroup.setCoordinator(describedGroup.getCoordinator());
                            }

                            if (includes.contains("authorizedOperations")) {
                                listedGroup.setAuthorizedOperations(describedGroup.getAuthorizedOperations());
                            }
                        }
                    });
                    describePromise.complete(null);
                    return true; // Match `completeExceptionally`
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
                .toArray(KafkaFuture[]::new);

            KafkaFuture.allOf(pendingOffsetOps).thenApply(nothing -> offsetsPromise.complete(null));
        } else {
            offsetsPromise.complete(null);
        }

        return CompletableFuture.allOf(describePromise, offsetsPromise).thenApply(nothing -> list);
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
