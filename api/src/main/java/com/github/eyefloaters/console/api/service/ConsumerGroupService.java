package com.github.eyefloaters.console.api.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.eclipse.microprofile.context.ThreadContext;

import com.github.eyefloaters.console.api.model.ConsumerGroup;
import com.github.eyefloaters.console.api.model.Either;
import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.MemberDescription;
import com.github.eyefloaters.console.api.model.OffsetAndMetadata;
import com.github.eyefloaters.console.api.model.PartitionKey;
import com.github.eyefloaters.console.api.support.ListRequestContext;

@ApplicationScoped
public class ConsumerGroupService {

    private static final ListConsumerGroupOffsetsSpec ALL_GROUP_PARTITIONS = new ListConsumerGroupOffsetsSpec();
    private static final OffsetSpec LATEST_TOPIC_OFFSETS = OffsetSpec.latest();
    private static final Set<String> REQUIRE_DESCRIBE = Set.of(
            ConsumerGroup.Fields.AUTHORIZED_OPERATIONS,
            ConsumerGroup.Fields.COORDINATOR,
            ConsumerGroup.Fields.MEMBERS,
            ConsumerGroup.Fields.OFFSETS);

    /**
     * ThreadContext of the request thread. This is used to execute asynchronous
     * tasks to allow access to request-scoped beans such as an injected
     * {@linkplain Admin Admin client}
     */
    @Inject
    ThreadContext threadContext;

    @Inject
    Supplier<Admin> clientSupplier;

    public CompletionStage<List<ConsumerGroup>> listConsumerGroups(List<String> includes, ListRequestContext<ConsumerGroup> listSupport) {
        return listConsumerGroups(Collections.emptyList(), includes, listSupport);
    }

    public CompletionStage<List<ConsumerGroup>> listConsumerGroups(String topicId, List<String> includes,
            ListRequestContext<ConsumerGroup> listSupport) {

        Admin adminClient = clientSupplier.get();
        Uuid id = Uuid.fromString(topicId);
        Executor asyncExec = threadContext.currentContextExecutor();

        return adminClient.describeTopics(TopicCollection.ofTopicIds(List.of(id)))
            .topicIdValues()
            .get(id)
            .toCompletionStage()
            .exceptionally(error -> {
                if (error instanceof InvalidTopicException) {
                    // See KAFKA-15373
                    throw new UnknownTopicIdException("No such topic: " + topicId);
                }
                throw new CompletionException(error);
            })
            .thenComposeAsync(unused -> listConsumerGroupMembership(List.of(topicId)), asyncExec)
            .thenComposeAsync(topicGroups -> {
                if (topicGroups.containsKey(topicId)) {
                    return listConsumerGroups(topicGroups.get(topicId), includes, listSupport);
                }
                return CompletableFuture.completedStage(Collections.emptyList());
            }, asyncExec);
    }

    CompletionStage<List<ConsumerGroup>> listConsumerGroups(List<String> groupIds, List<String> includes, ListRequestContext<ConsumerGroup> listSupport) {
        Admin adminClient = clientSupplier.get();

        return adminClient.listConsumerGroups()
            .valid()
            .toCompletionStage()
            .thenApply(groups -> groups.stream()
                    .filter(group -> groupIds.isEmpty() || groupIds.contains(group.groupId()))
                    .map(ConsumerGroup::fromKafkaModel)
                    .toList())
            .thenCompose(groups -> augmentList(adminClient, groups, includes))
            .thenApply(list -> list.stream()
                    .map(listSupport::tally)
                    .filter(listSupport::betweenCursors)
                    .sorted(listSupport.getSortComparator())
                    .dropWhile(listSupport::beforePageBegin)
                    .takeWhile(listSupport::pageCapacityAvailable)
                    .toList());
    }

    public CompletionStage<ConsumerGroup> describeConsumerGroup(String groupId, List<String> includes) {
        Admin adminClient = clientSupplier.get();

        return adminClient.listConsumerGroups()
            .all()
            .toCompletionStage()
            .thenAccept(listing -> {
                if (listing.stream().map(ConsumerGroupListing::groupId).noneMatch(groupId::equals)) {
                    throw new GroupIdNotFoundException("No such consumer group: " + groupId);
                }
            })
            .thenCompose(nothing -> describeConsumerGroups(adminClient, List.of(groupId), includes))
            .thenApply(groups -> groups.get(groupId))
            .thenApply(result -> result.getOrThrow(CompletionException::new));
    }

    public CompletionStage<Map<String, List<String>>> listConsumerGroupMembership(Collection<String> topicIds) {
        Admin adminClient = clientSupplier.get();

        return adminClient.listConsumerGroups()
            .valid()
            .toCompletionStage()
            .thenApply(groups -> groups.stream().map(ConsumerGroup::fromKafkaModel).toList())
            .thenCompose(groups -> augmentList(adminClient, groups, List.of(
                    ConsumerGroup.Fields.MEMBERS,
                    ConsumerGroup.Fields.OFFSETS)))
            .thenApply(list -> list.stream()
                    .map(group -> Map.entry(
                            group.getGroupId(),
                            Stream.concat(
                                Optional.ofNullable(group.getOffsets())
                                    .map(Collection::stream)
                                    .orElseGet(Stream::empty)
                                    .map(OffsetAndMetadata::topicId),
                                Optional.ofNullable(group.getMembers())
                                    .map(Collection::stream)
                                    .orElseGet(Stream::empty)
                                    .map(MemberDescription::getAssignments)
                                    .filter(Objects::nonNull)
                                    .flatMap(Collection::stream)
                                    .map(PartitionKey::topicId))
                                .distinct()
                                .toList()))
                    .filter(groupTopics -> groupTopics.getValue().stream().anyMatch(topicIds::contains))
                    .collect(
                            () -> new HashMap<String, List<String>>(),
                            (map, entry) -> topicIds.stream()
                                    .filter(entry.getValue()::contains)
                                    .forEach(topicId -> map
                                            .computeIfAbsent(topicId, key -> new ArrayList<>())
                                            .add(entry.getKey())),
                            (e1, e2) -> { }));
    }

//    public CompletionStage<Map<String, Map<Integer, Error>>> alterConsumerGroupOffsets(String groupId, Map<String, Map<Integer, OffsetAndMetadata>> topicOffsets) {
//        var offsets = topicOffsets.entrySet()
//                .stream()
//                .flatMap(partitionOffsets -> {
//                    String topicName = partitionOffsets.getKey();
//
//                    return partitionOffsets.getValue().entrySet().stream()
//                        .map(partitionOffset ->
//                            Map.entry(
//                                    new org.apache.kafka.common.TopicPartition(topicName, partitionOffset.getKey()),
//                                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(
//                                            partitionOffset.getValue().getOffset(),
//                                            Optional.ofNullable(partitionOffset.getValue().getLeaderEpoch()),
//                                            partitionOffset.getValue().getMetadata())));
//                })
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//        Admin adminClient = clientSupplier.get();
//        var results = adminClient.alterConsumerGroupOffsets(groupId, offsets);
//        Map<org.apache.kafka.common.TopicPartition, CompletableFuture<Void>> offsetResults;
//
//        offsetResults = offsets.keySet()
//                .stream()
//                .collect(Collectors.toMap(
//                        Function.identity(),
//                        partition -> results.partitionResult(partition).toCompletionStage().toCompletableFuture()));
//
//        Map<String, Map<Integer, Error>> errors = new LinkedHashMap<>();
//
//        return CompletableFuture.allOf(offsetResults.values().toArray(CompletableFuture[]::new))
//            .thenAccept(nothing -> offsetResults.entrySet()
//                .stream()
//                .filter(e -> e.getValue().isCompletedExceptionally())
//                .forEach(e -> {
//                    org.apache.kafka.common.TopicPartition partition = e.getKey();
//
//                    e.getValue().exceptionally(error -> {
//                        String msg = "Unable to reset offset for topic %s, partition %d".formatted(partition.topic(), partition.partition());
//                        errors.computeIfAbsent(partition.topic(), k -> new LinkedHashMap<>())
//                            .put(partition.partition(), new Error(msg, error.getMessage(), error));
//                        return null;
//                    });
//                }))
//            .thenApply(nothing -> errors);
//    }

//    public CompletionStage<Map<String, Error>> deleteConsumerGroups(String... groupIds) {
//        Admin adminClient = clientSupplier.get();
//        Map<String, Error> errors = new HashMap<>();
//
//        var pendingDeletes = adminClient.deleteConsumerGroups(Arrays.asList(groupIds))
//                .deletedGroups()
//                .entrySet()
//                .stream()
//                .map(entry -> entry.getValue().whenComplete((nothing, thrown) -> {
//                    if (thrown != null) {
//                        errors.put(entry.getKey(), new Error("Unable to delete consumer group", thrown.getMessage(), thrown));
//                    }
//                }))
//                .map(KafkaFuture::toCompletionStage)
//                .map(CompletionStage::toCompletableFuture)
//                .toArray(CompletableFuture[]::new);
//
//        return CompletableFuture.allOf(pendingDeletes).thenApply(nothing -> errors);
//    }

    CompletionStage<List<ConsumerGroup>> augmentList(Admin adminClient, List<ConsumerGroup> list, List<String> includes) {
        Map<String, ConsumerGroup> groups = list.stream().collect(Collectors.toMap(ConsumerGroup::getGroupId, Function.identity()));
        CompletableFuture<Void> describePromise;

        if (REQUIRE_DESCRIBE.stream().anyMatch(includes::contains)) {
            describePromise = describeConsumerGroups(adminClient, groups.keySet(), includes)
                .thenAccept(descriptions ->
                    descriptions.forEach((name, either) -> mergeDescriptions(groups.get(name), either)))
                .toCompletableFuture();
        } else {
            describePromise = CompletableFuture.completedFuture(null);
        }

        return describePromise.thenApply(nothing -> list);
    }

    void mergeDescriptions(ConsumerGroup group, Either<ConsumerGroup, Throwable> description) {
        if (description.isPrimaryEmpty()) {
            Throwable thrown = description.getAlternate();
            Error error = new Error("Unable to describe consumer group", thrown.getMessage(), thrown);
            group.addError(error);
        } else {
            ConsumerGroup describedGroup = description.getPrimary();
            group.setMembers(describedGroup.getMembers());
            group.setOffsets(describedGroup.getOffsets());
            group.setCoordinator(describedGroup.getCoordinator());
            group.setAuthorizedOperations(describedGroup.getAuthorizedOperations());
        }
    }

    CompletionStage<Map<String, Either<ConsumerGroup, Throwable>>> describeConsumerGroups(
            Admin adminClient,
            Collection<String> groupIds,
            List<String> includes) {

        Map<String, Either<ConsumerGroup, Throwable>> result = new LinkedHashMap<>(groupIds.size());

        var pendingTopicsIds = adminClient
                .listTopics(new ListTopicsOptions().listInternal(true))
                .listings()
                .thenApply(topics -> topics.stream().collect(Collectors.toMap(TopicListing::name, l -> l.topicId().toString())))
                .toCompletionStage()
                .toCompletableFuture();

        var pendingDescribes = adminClient.describeConsumerGroups(groupIds,
                new DescribeConsumerGroupsOptions()
                    .includeAuthorizedOperations(includes.contains(ConsumerGroup.Fields.AUTHORIZED_OPERATIONS)))
                .describedGroups()
                .entrySet()
                .stream()
                .map(entry ->
                    entry.getValue().toCompletionStage().<Void>handle((description, error) -> {
                        var consumerGroup = Optional.ofNullable(description).map(ConsumerGroup::fromKafkaModel);
                        result.put(entry.getKey(), Either.of(consumerGroup, error));
                        return null;
                    }))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        Supplier<Map<String, ConsumerGroup>> availableGroups = () -> result.entrySet()
                .stream()
                .filter(e -> e.getValue().isPrimaryPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getPrimary()));

        return CompletableFuture.allOf(pendingDescribes)
                .thenCompose(nothing -> pendingTopicsIds)
                .thenCompose(topicIds -> {
                    if (includes.contains(ConsumerGroup.Fields.OFFSETS)) {
                        return fetchOffsets(adminClient, availableGroups.get(), topicIds)
                                .thenApply(nothing -> topicIds);
                    }

                    return CompletableFuture.completedFuture(topicIds);
                })
                .thenApply(topicIds -> {
                    availableGroups.get()
                        .values()
                        .stream()
                        .map(ConsumerGroup::getMembers)
                        .flatMap(Collection::stream)
                        .forEach(member -> addAssignmentTopicIds(member, topicIds));

                    return result;
                });
    }

    CompletableFuture<Void> fetchOffsets(Admin adminClient, Map<String, ConsumerGroup> groups, Map<String, String> topicIds) {
        var groupOffsetsRequest = groups.keySet()
                .stream()
                .collect(Collectors.toMap(Function.identity(), key -> ALL_GROUP_PARTITIONS));

        var groupOffsetsResult = adminClient.listConsumerGroupOffsets(groupOffsetsRequest);

        Map<String, Either<Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata>, Throwable>> groupOffsets = new LinkedHashMap<>();
        Map<TopicPartition, Either<ListOffsetsResultInfo, Throwable>> topicOffsets = new LinkedHashMap<>();

        var pendingGroupOps = groups.keySet()
            .stream()
            .map(groupId -> groupOffsetsResult.partitionsToOffsetAndMetadata(groupId)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .<Void>handle((offsets, thrown) -> {
                        groupOffsets.put(groupId, Either.of(Optional.ofNullable(offsets), thrown));
                        return null;
                    }))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingGroupOps)
            .thenApply(nothing -> {
                var topicOffsetsRequest = groupOffsets.values()
                        .stream()
                        .filter(Either::isPrimaryPresent)
                        .map(Either::getPrimary)
                        .map(Map::keySet)
                        .flatMap(Collection::stream)
                        .distinct()
                        .collect(Collectors.toMap(Function.identity(), key -> LATEST_TOPIC_OFFSETS));
                var topicOffsetsResult = adminClient.listOffsets(topicOffsetsRequest);

                return topicOffsetsRequest.keySet()
                    .stream()
                    .map(partition -> topicOffsetsResult
                        .partitionResult(partition)
                        .toCompletionStage()
                        .toCompletableFuture()
                        .<Void>handle((offset, error) -> {
                            topicOffsets.put(partition, Either.of(Optional.ofNullable(offset), error));
                            return null;
                        }))
                    .toArray(CompletableFuture[]::new);
            })
            .thenCompose(CompletableFuture::allOf)
            .thenRun(() -> groups.forEach((groupId, group) -> {
                var grpOffsets = groupOffsets.get(groupId);
                addOffsets(group, topicIds, topicOffsets, grpOffsets.getOptionalPrimary().orElse(null), grpOffsets.getAlternate());
            }));
    }

    void addOffsets(ConsumerGroup group,
            Map<String, String> topicIds,
            Map<TopicPartition, Either<ListOffsetsResultInfo, Throwable>> topicOffsets,
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> groupOffsets,
            Throwable thrown) {

        if (thrown != null) {
            group.addError(new Error("Unable to list consumer group offsets", thrown.getMessage(), thrown));
        } else {
            List<OffsetAndMetadata> offsets = new ArrayList<>();

            groupOffsets.forEach((topicPartition, offsetsAndMetadata) -> {
                long offset = offsetsAndMetadata.offset();
                offsets.add(new OffsetAndMetadata(
                        topicIds.get(topicPartition.topic()),
                        topicPartition.partition(),
                        offsetsAndMetadata.offset(),
                        Optional.ofNullable(topicOffsets.get(topicPartition))
                            .map(partitionOffsets -> {
                                // Calculate lag
                                if (partitionOffsets.isPrimaryPresent()) {
                                    return partitionOffsets.getPrimary().offset() - offset;
                                }

                                Throwable listOffsetsError = partitionOffsets.getAlternate();
                                String msg = "Unable to list offsets for topic/partition %s-%d"
                                        .formatted(topicPartition.topic(), topicPartition.partition());
                                group.addError(new Error(msg, listOffsetsError.getMessage(), listOffsetsError));
                                return -1L;
                            })
                            .orElse(-1L), // lag
                        offsetsAndMetadata.metadata(),
                        offsetsAndMetadata.leaderEpoch().orElse(null)));
            });

            group.setOffsets(offsets);
        }
    }

    void addAssignmentTopicIds(MemberDescription member, Map<String, String> topicIds) {
        List<PartitionKey> assignmentsWithId = member.getAssignments()
                .stream()
                .map(e -> new PartitionKey(topicIds.get(e.topicName()), e.topicName(), e.partition()))
                .toList();

        member.setAssignments(assignmentsWithId);
    }
}
