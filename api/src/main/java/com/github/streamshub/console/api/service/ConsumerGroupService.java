package com.github.streamshub.console.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListConsumerGroupsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.eclipse.microprofile.context.ThreadContext;

import com.github.streamshub.console.api.model.ConsumerGroup;
import com.github.streamshub.console.api.model.Either;
import com.github.streamshub.console.api.model.Error;
import com.github.streamshub.console.api.model.MemberDescription;
import com.github.streamshub.console.api.model.OffsetAndMetadata;
import com.github.streamshub.console.api.model.PartitionId;
import com.github.streamshub.console.api.model.PartitionInfo;
import com.github.streamshub.console.api.model.Topic;
import com.github.streamshub.console.api.support.ConsumerGroupValidation;
import com.github.streamshub.console.api.support.FetchFilterPredicate;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.KafkaOffsetSpec;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.UnknownTopicIdPatch;
import com.github.streamshub.console.api.support.ValidationProxy;

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
     * Constant exception instance to avoid reporting the same error multiple times
     * (per partition being reset) for a single alterConsumerGroupOffsets call.
     */
    private static final RuntimeException GROUP_NOT_EMPTY = new GroupNotEmptyException("Consumer group not empty");

    /**
     * ThreadContext of the request thread. This is used to execute asynchronous
     * tasks to allow access to request-scoped beans such as an injected
     * {@linkplain Admin Admin client}
     */
    @Inject
    ThreadContext threadContext;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    TopicService topicService;

    @Inject
    ValidationProxy validationService;

    public CompletionStage<List<ConsumerGroup>> listConsumerGroups(List<String> includes, ListRequestContext<ConsumerGroup> listSupport) {
        return listConsumerGroups(Collections.emptyList(), includes, listSupport);
    }

    public CompletionStage<List<ConsumerGroup>> listConsumerGroups(String topicId, List<String> includes,
            ListRequestContext<ConsumerGroup> listSupport) {

        Admin adminClient = kafkaContext.admin();
        Uuid id = Uuid.fromString(topicId);
        Executor asyncExec = threadContext.currentContextExecutor();

        return adminClient.describeTopics(TopicCollection.ofTopicIds(List.of(id)))
            .topicIdValues()
            .get(id)
            .toCompletionStage()
            .exceptionally(error -> {
                throw (RuntimeException) UnknownTopicIdPatch.apply(error, CompletionException::new);
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
        Admin adminClient = kafkaContext.admin();

        Set<ConsumerGroupState> states = listSupport.filters()
            .stream()
            .filter(FetchFilterPredicate.class::isInstance)
            .map(FetchFilterPredicate.class::cast)
            .filter(filter -> "filter[state]".equals(filter.name()))
            .map(filter -> {
                @SuppressWarnings("unchecked")
                List<String> operands = filter.operands();
                return operands.stream()
                        .map(ConsumerGroupState::valueOf)
                        .collect(Collectors.toSet());
            })
            .findFirst()
            .orElse(null);

        return adminClient.listConsumerGroups(new ListConsumerGroupsOptions()
                .inStates(states))
            .valid()
            .toCompletionStage()
            .thenApply(groups -> groups.stream()
                    .filter(group -> groupIds.isEmpty() || groupIds.contains(group.groupId()))
                    .map(ConsumerGroup::fromKafkaModel)
                    .toList())
            .thenApply(list -> list.stream()
                    .filter(listSupport)
                    .map(listSupport::tally)
                    .filter(listSupport::betweenCursors)
                    .sorted(listSupport.getSortComparator())
                    .dropWhile(listSupport::beforePageBegin)
                    .takeWhile(listSupport::pageCapacityAvailable)
                    .toList())
            .thenCompose(groups -> augmentList(adminClient, groups, includes));
    }

    public CompletionStage<ConsumerGroup> describeConsumerGroup(String requestGroupId, List<String> includes) {
        Admin adminClient = kafkaContext.admin();
        String groupId = preprocessGroupId(requestGroupId);

        return assertConsumerGroupExists(adminClient, groupId)
            .thenCompose(nothing -> describeConsumerGroups(adminClient, List.of(groupId), includes))
            .thenApply(groups -> groups.get(groupId))
            .thenApply(result -> result.getOrThrow(CompletionException::new));
    }

    public CompletionStage<Map<String, List<String>>> listConsumerGroupMembership(Collection<String> topicIds) {
        Admin adminClient = kafkaContext.admin();

        return adminClient.listConsumerGroups(new ListConsumerGroupsOptions()
                .inStates(Set.of(
                        ConsumerGroupState.STABLE,
                        ConsumerGroupState.PREPARING_REBALANCE,
                        ConsumerGroupState.COMPLETING_REBALANCE)))
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
                                    .map(PartitionId::topicId))
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

    public CompletionStage<Void> patchConsumerGroup(ConsumerGroup patch) {
        Admin adminClient = kafkaContext.admin();
        String groupId = preprocessGroupId(patch.getGroupId());

        return assertConsumerGroupExists(adminClient, groupId)
            .thenComposeAsync(nothing -> Optional.ofNullable(patch.getOffsets())
                    .filter(Predicate.not(Collection::isEmpty))
                    .map(patchedOffsets -> alterConsumerGroupOffsets(adminClient, groupId, patch))
                    .orElseGet(() -> CompletableFuture.completedStage(null)),
                threadContext.currentContextExecutor());
    }

    CompletionStage<Void> assertConsumerGroupExists(Admin adminClient, String groupId) {
        return adminClient.listConsumerGroups()
            .all()
            .toCompletionStage()
            .thenAccept(listing -> {
                if (listing.stream().map(ConsumerGroupListing::groupId).noneMatch(groupId::equals)) {
                    throw new GroupIdNotFoundException("No such consumer group: " + groupId);
                }
            });
    }

    CompletionStage<Void> alterConsumerGroupOffsets(Admin adminClient, String groupId, ConsumerGroup patch) {
        var topicsToDescribe = patch.getOffsets()
                .stream()
                .map(OffsetAndMetadata::topicId)
                .distinct()
                .map(Uuid::fromString)
                .toList();

        return topicService.describeTopics(
                adminClient,
                topicsToDescribe,
                List.of(Topic.Fields.PARTITIONS),
                KafkaOffsetSpec.LATEST)
            .thenApply(topics -> validationService.validate(new ConsumerGroupValidation.ConsumerGroupPatchInputs(topics, patch)))
            .thenApply(ConsumerGroupValidation.ConsumerGroupPatchInputs::topics)
            .thenCompose(topics -> {
                var offsetModifications = patch.getOffsets()
                    .stream()
                    .flatMap(offset -> {
                        String topicId = offset.topicId();
                        Either<Topic, Throwable> topic = topics.get(Uuid.fromString(topicId));

                        if (topic.isPrimaryEmpty()) {
                            return Stream.empty();
                        }

                        String topicName = topic.getPrimary().name();
                        Integer partition = offset.partition();

                        if (partition != null) {
                            return Stream.of(Map.entry(new PartitionId(topicId, topicName, partition), offset));
                        } else {
                            return topic.getPrimary().partitions().getOptionalPrimary()
                                .map(Collection::stream)
                                .orElseGet(Stream::empty)
                                .map(PartitionInfo::getPartition)
                                .map(p -> Map.entry(new PartitionId(topicId, topicName, p), offset));
                        }
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                var topicOffsetsRequest = offsetModifications.entrySet()
                    .stream()
                    .filter(e -> e.getValue().offset().isPrimaryEmpty())
                    .map(e -> Map.entry(
                        e.getKey().toKafkaModel(),
                        switch (e.getValue().offset().getAlternate()) {
                            case KafkaOffsetSpec.EARLIEST -> OffsetSpec.earliest();
                            case KafkaOffsetSpec.LATEST -> OffsetSpec.latest();
                            case KafkaOffsetSpec.MAX_TIMESTAMP -> OffsetSpec.maxTimestamp();
                            default -> OffsetSpec.forTimestamp(Instant.parse(e.getValue().offset().getAlternate()).toEpochMilli());
                        }))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                var topicOffsetsResult = adminClient.listOffsets(topicOffsetsRequest);

                Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> targetOffsets = new HashMap<>();

                var pendingTopicOffsets = getListOffsetsResults(topicOffsetsRequest.keySet(), topicOffsetsResult);

                var offsetModificationsByPK = offsetModifications.entrySet()
                    .stream()
                    .map(e -> Map.entry(e.getKey().toKafkaModel(), e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                return allOf(pendingTopicOffsets.values())
                    .thenRun(() ->
                        pendingTopicOffsets
                            .entrySet()
                            .stream()
                            .map(e -> Map.entry(e.getKey(), e.getValue().join()))
                            .filter(e -> e.getValue().offset() >= 0)
                            .forEach(e -> targetOffsets.put(
                                    e.getKey(),
                                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(
                                            e.getValue().offset(),
                                            Optional.ofNullable(offsetModificationsByPK.get(e.getKey()).leaderEpoch()),
                                            offsetModificationsByPK.get(e.getKey()).metadata()))))
                    .thenRun(() ->
                        offsetModifications.entrySet()
                            .stream()
                            .filter(e -> e.getValue().offset().isPrimaryPresent())
                            .forEach(e -> {
                                PartitionId id = e.getKey();
                                targetOffsets.put(
                                        id.toKafkaModel(),
                                        new org.apache.kafka.clients.consumer.OffsetAndMetadata(
                                            e.getValue().offset().getPrimary(),
                                            Optional.ofNullable(offsetModifications.get(id).leaderEpoch()),
                                            offsetModifications.get(id).metadata()
                                        )
                                );
                            })
                    )
                    .thenApply(nothing1 -> targetOffsets);
            })
            .thenCompose(alterRequest -> {
                var alterResults = adminClient.alterConsumerGroupOffsets(groupId, alterRequest);

                Map<TopicPartition, CompletableFuture<Void>> offsetResults = alterRequest.keySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                partition -> alterResults.partitionResult(partition)
                                    .toCompletionStage()
                                    .exceptionally(error -> {
                                        if (error instanceof UnknownMemberIdException) {
                                            throw GROUP_NOT_EMPTY;
                                        }
                                        if (error instanceof CompletionException ce) {
                                            throw ce;
                                        }
                                        throw new CompletionException(error);
                                    })
                                    .toCompletableFuture()));

                return allOf(offsetResults.values());
            });
    }

    Map<TopicPartition, CompletableFuture<ListOffsetsResultInfo>> getListOffsetsResults(
            Set<TopicPartition> partitions,
            ListOffsetsResult topicOffsetsResult) {

        return partitions.stream()
                .map(partition -> Map.entry(
                        partition,
                        topicOffsetsResult.partitionResult(partition)
                            .toCompletionStage()
                            .toCompletableFuture()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static <F extends Object> CompletableFuture<Void> allOf(Collection<CompletableFuture<F>> pending) {
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
            .exceptionally(error -> {
                Set<Throwable> suppressed = new LinkedHashSet<>();

                pending.stream()
                    .filter(CompletableFuture::isCompletedExceptionally)
                    .forEach(fut -> fut.exceptionally(ex -> {
                        if (ex instanceof CompletionException ce) {
                            ex = ce.getCause();
                        }
                        suppressed.add(ex);
                        return null;
                    }));

                CompletionException aggregator = new CompletionException(
                        "One or more errors occurred awaiting a collection of pending results",
                        error);
                suppressed.forEach(aggregator::addSuppressed);

                throw aggregator;
            });
    }

    public CompletionStage<Void> deleteConsumerGroup(String requestGroupId) {
        Admin adminClient = kafkaContext.admin();
        String groupId = preprocessGroupId(requestGroupId);

        return adminClient.deleteConsumerGroups(List.of(groupId))
                .deletedGroups()
                .get(groupId)
                .toCompletionStage();
    }

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

        var pendingTopicsIds = topicService.listTopics(true)
                .thenApply(topics -> topics.stream()
                        .collect(Collectors.toMap(TopicListing::name, l -> l.topicId().toString())));

        var pendingDescribes = adminClient.describeConsumerGroups(groupIds,
                new DescribeConsumerGroupsOptions()
                    .includeAuthorizedOperations(includes.contains(ConsumerGroup.Fields.AUTHORIZED_OPERATIONS)))
                .describedGroups()
                .entrySet()
                .stream()
                .map(entry ->
                    entry.getValue()
                        .toCompletionStage()
                        .thenCombine(pendingTopicsIds, ConsumerGroup::fromKafkaModel)
                        .<Void>handle((consumerGroup, error) -> {
                            result.put(entry.getKey(), Either.of(
                                    Optional.ofNullable(consumerGroup),
                                    /*
                                     * If an error exists and has a non-null cause, unwrap it (CompletionException).
                                     * Otherwise, just pass the error, possibly null if no exception raised.
                                     */
                                    Optional.ofNullable(error).map(Throwable::getCause).orElse(error)));
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
                                .thenApply(nothing -> result);
                    }

                    return CompletableFuture.completedFuture(result);
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
                var endOffset = Optional.ofNullable(topicOffsets.get(topicPartition))
                        .map(offsetOrError -> {
                            if (offsetOrError.isPrimaryPresent()) {
                                return offsetOrError.getPrimary().offset();
                            }

                            Throwable listOffsetsError = offsetOrError.getAlternate();
                            String msg = "Unable to list offsets for topic/partition %s-%d"
                                    .formatted(topicPartition.topic(), topicPartition.partition());
                            group.addError(new Error(msg, listOffsetsError.getMessage(), listOffsetsError));
                            return null;
                        });

                offsets.add(new OffsetAndMetadata(
                        topicIds.get(topicPartition.topic()),
                        topicPartition.topic(),
                        topicPartition.partition(),
                        Either.of(offsetsAndMetadata.offset()),
                        endOffset.orElse(null), // log end offset
                        endOffset.map(end -> end - offset).orElse(null), // lag
                        offsetsAndMetadata.metadata(),
                        offsetsAndMetadata.leaderEpoch().orElse(null)));
            });

            group.setOffsets(offsets);
        }
    }

    static String preprocessGroupId(String groupId) {
        return "+".equals(groupId) ? "" : groupId;
    }
}
