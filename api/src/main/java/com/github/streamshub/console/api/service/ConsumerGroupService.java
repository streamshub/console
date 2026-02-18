package com.github.streamshub.console.api.service;

import java.time.Instant;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListGroupsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.eclipse.microprofile.context.ThreadContext;

import com.github.streamshub.console.api.model.ConsumerGroup;
import com.github.streamshub.console.api.model.Either;
import com.github.streamshub.console.api.model.MemberDescription;
import com.github.streamshub.console.api.model.OffsetAndMetadata;
import com.github.streamshub.console.api.model.PartitionId;
import com.github.streamshub.console.api.model.PartitionInfo;
import com.github.streamshub.console.api.model.Topic;
import com.github.streamshub.console.api.model.jsonapi.JsonApiError;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.ConsumerGroupValidation;
import com.github.streamshub.console.api.support.FetchFilterPredicate;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.KafkaOffsetSpec;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.Promises;
import com.github.streamshub.console.api.support.UnknownTopicIdPatch;
import com.github.streamshub.console.api.support.ValidationProxy;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;

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
    PermissionService permissionService;

    @Inject
    TopicDescribeService topicService;

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
            .thenComposeAsync(topic -> {
                permissionService.assertPermitted(ResourceTypes.Kafka.TOPICS, Privilege.GET, topic.name());
                return listConsumerGroupMembership(List.of(topicId));
            }, asyncExec)
            .thenComposeAsync(topicGroups -> {
                if (topicGroups.containsKey(topicId)) {
                    return listConsumerGroups(topicGroups.get(topicId), includes, listSupport);
                }
                return CompletableFuture.completedStage(Collections.emptyList());
            }, asyncExec);
    }

    private CompletionStage<List<ConsumerGroup>> listConsumerGroups(List<String> groupIds,
            List<String> includes, ListRequestContext<ConsumerGroup> listSupport) {

        Admin adminClient = kafkaContext.admin();

        Set<GroupState> states = listSupport.filters(ConsumerGroup.class)
            .stream()
            .filter(FetchFilterPredicate.class::isInstance)
            .map(FetchFilterPredicate.class::cast)
            .filter(filter -> "filter[state]".equals(filter.name()))
            .map(filter -> {
                @SuppressWarnings("unchecked")
                List<String> operands = filter.operands();
                return operands.stream()
                        .map(GroupState::parse)
                        .collect(Collectors.toSet());
            })
            .findFirst()
            .orElse(null);

        permissionService.addPrivileges(listSupport.meta(), ResourceTypes.Kafka.GROUPS, null);

        return adminClient.listGroups(listGroupOptions().inGroupStates(states))
            .valid()
            .toCompletionStage()
            .thenApplyAsync(groups -> groups.stream()
                    .filter(group -> groupIds.isEmpty() || groupIds.contains(group.groupId()))
                    .filter(permissionService.permitted(ResourceTypes.Kafka.GROUPS, Privilege.LIST, GroupListing::groupId))
                    .map(ConsumerGroup::fromKafkaModel),
                    threadContext.currentContextExecutor())
            .thenApplyAsync(groups -> groups
                    .filter(listSupport.filter(ConsumerGroup.class))
                    .map(listSupport::tally)
                    .filter(listSupport::betweenCursors)
                    .sorted(listSupport.getSortComparator())
                    .dropWhile(listSupport::beforePageBegin)
                    .takeWhile(listSupport::pageCapacityAvailable)
                    .map(permissionService.addPrivileges(ResourceTypes.Kafka.GROUPS, ConsumerGroup::groupId))
                    .toList(),
                    threadContext.currentContextExecutor())
            .thenComposeAsync(
                    groups -> augmentList(adminClient, groups, includes),
                    threadContext.currentContextExecutor());
    }

    public CompletionStage<ConsumerGroup> describeConsumerGroup(String requestGroupId, List<String> includes) {
        Admin adminClient = kafkaContext.admin();
        String groupId = ConsumerGroup.decodeGroupId(requestGroupId);

        return assertConsumerGroupExists(adminClient, groupId)
            .thenComposeAsync(
                    nothing -> describeConsumerGroups(adminClient, List.of(groupId), includes),
                    threadContext.currentContextExecutor())
            .thenApply(groups -> groups.get(groupId))
            .thenApply(result -> result.getOrThrow(CompletionException::new))
            .thenApplyAsync(
                    permissionService.addPrivileges(ResourceTypes.Kafka.GROUPS, ConsumerGroup::groupId),
                    threadContext.currentContextExecutor());
    }

    public CompletionStage<Map<String, List<String>>> listConsumerGroupMembership(Collection<String> topicIds) {
        Admin adminClient = kafkaContext.admin();

        return adminClient.listGroups(listGroupOptions()
                .inGroupStates(Set.of(
                        GroupState.STABLE,
                        GroupState.PREPARING_REBALANCE,
                        GroupState.COMPLETING_REBALANCE,
                        GroupState.EMPTY)))
            .valid()
            .toCompletionStage()
            .thenApplyAsync(groups -> groups.stream()
                    .filter(permissionService.permitted(ResourceTypes.Kafka.GROUPS, Privilege.LIST, GroupListing::groupId))
                    .map(ConsumerGroup::fromKafkaModel).toList(),
                    threadContext.currentContextExecutor())
            .thenComposeAsync(groups -> augmentList(adminClient, groups, List.of(
                    ConsumerGroup.Fields.MEMBERS,
                    ConsumerGroup.Fields.OFFSETS)),
                    threadContext.currentContextExecutor())
            .thenApply(list -> list.stream()
                    .map(group -> Map.entry(
                            group.groupId(),
                            Stream.concat(
                                Optional.ofNullable(group.offsets())
                                    .map(Collection::stream)
                                    .orElseGet(Stream::empty)
                                    .map(OffsetAndMetadata::topicId),
                                Optional.ofNullable(group.members())
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

    public CompletionStage<Optional<ConsumerGroup>> patchConsumerGroup(String requestGroupId, ConsumerGroup patch, boolean dryRun) {
        Admin adminClient = kafkaContext.admin();
        String groupId = ConsumerGroup.decodeGroupId(requestGroupId);

        return assertConsumerGroupExists(adminClient, groupId)
            .thenComposeAsync(nothing -> Optional.ofNullable(patch.offsets())
                    .filter(Predicate.not(Collection::isEmpty))
                    .map(patchedOffsets -> alterConsumerGroupOffsets(adminClient, groupId, patch, dryRun))
                    .orElseGet(() -> CompletableFuture.completedStage(Optional.empty())),
                threadContext.currentContextExecutor());
    }

    CompletionStage<Void> assertConsumerGroupExists(Admin adminClient, String groupId) {
        return adminClient.listGroups(listGroupOptions())
            .all()
            .toCompletionStage()
            .thenAcceptAsync(listing -> {
                if (listing.stream()
                        .filter(permissionService.permitted(ResourceTypes.Kafka.GROUPS, Privilege.GET, GroupListing::groupId))
                        .map(GroupListing::groupId)
                        .noneMatch(groupId::equals)) {
                    throw new GroupIdNotFoundException("No such consumer group: " + groupId);
                }
            }, threadContext.currentContextExecutor());
    }

    private ListGroupsOptions listGroupOptions() {
        return new ListGroupsOptions();
    }

    CompletionStage<Optional<ConsumerGroup>> alterConsumerGroupOffsets(Admin adminClient, String groupId, ConsumerGroup patch, boolean dryRun) {
        var topicsToDescribe = patch.offsets()
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
                var offsetModifications = buildOffsetModifications(patch, topics);
                var offsetModificationsByPK = offsetModifications.entrySet().stream()
                        .map(e -> Map.entry(e.getKey().toKafkaModel(), e.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> targetOffsets = new HashMap<>();

                return fetchOffsetsForSpecs(adminClient, offsetModifications, offsetModificationsByPK, targetOffsets)
                    .thenRun(() -> {
                        for (var entry : offsetModifications.entrySet()) {
                            PartitionId id = entry.getKey();
                            OffsetAndMetadata offsetMeta = entry.getValue();

                            if (offsetMeta.hasAbsoluteOffset()) {
                                targetOffsets.put(
                                        id.toKafkaModel(),
                                        new org.apache.kafka.clients.consumer.OffsetAndMetadata(
                                            offsetMeta.absoluteOffset(),
                                            Optional.ofNullable(offsetModifications.get(id).leaderEpoch()),
                                            offsetModifications.get(id).metadata()
                                        )
                                );
                            } else if (offsetMeta.isDeleted()) {
                                targetOffsets.put(id.toKafkaModel(), null);
                            }
                        }
                    })
                    .thenApply(nothing -> targetOffsets);
            })
            .thenComposeAsync(alterRequest -> {
                if (dryRun) {
                    return alterConsumerGroupOffsetsDryRun(adminClient, groupId, alterRequest)
                            .thenApply(Optional::of);
                } else {
                    return alterConsumerGroupOffsets(adminClient, groupId, alterRequest)
                            .thenApply(nothing -> Optional.empty());
                }
            }, threadContext.currentContextExecutor());
    }

    Map<PartitionId, OffsetAndMetadata> buildOffsetModifications(ConsumerGroup patch, Map<Uuid, Either<Topic, Throwable>> topics) {
        return patch.offsets()
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
    }

    /**
     * Find the absolute offsets for the partitions being reset to an offset spec
     * like earliest, latest, max timestamp, or specific timestamp. Results are
     * places in the {@code targetOffsets} map parameter.
     */
    CompletionStage<Void> fetchOffsetsForSpecs(Admin adminClient,
            Map<PartitionId, OffsetAndMetadata> offsetModifications,
            Map<TopicPartition, OffsetAndMetadata> offsetModificationsByPK,
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> targetOffsets) {

        var topicOffsetsRequest = offsetModifications.entrySet()
            .stream()
            .filter(e -> e.getValue().hasOffsetSpec())
            .map(e -> Map.entry(
                e.getKey().toKafkaModel(),
                switch (e.getValue().offsetSpec()) {
                    case KafkaOffsetSpec.EARLIEST -> OffsetSpec.earliest();
                    case KafkaOffsetSpec.LATEST -> OffsetSpec.latest();
                    case KafkaOffsetSpec.MAX_TIMESTAMP -> OffsetSpec.maxTimestamp();
                    default -> OffsetSpec.forTimestamp(Instant.parse(e.getValue().offsetSpec()).toEpochMilli());
                }))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var topicOffsetsResult = adminClient.listOffsets(topicOffsetsRequest);
        var pendingTopicOffsets = getListOffsetsResults(topicOffsetsRequest.keySet(), topicOffsetsResult);

        return Promises.allOf(pendingTopicOffsets.values())
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
                                    offsetModificationsByPK.get(e.getKey()).metadata()))));
    }

    CompletionStage<ConsumerGroup> alterConsumerGroupOffsetsDryRun(Admin adminClient, String groupId,
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> alterRequest) {
        var pendingTopicsIds = fetchTopicIdMap();

        return describeConsumerGroups(adminClient, List.of(groupId), Collections.emptyList())
            .thenApply(groups -> groups.get(groupId))
            .thenApply(result -> result.getOrThrow(CompletionException::new))
            .thenCombine(pendingTopicsIds, (group, topicIds) -> {
                group.offsets(alterRequest.entrySet().stream().map(e -> {
                    var topicPartition = e.getKey();
                    String topicName = topicPartition.topic();
                    String topicId = topicIds.get(topicName);
                    var offsetMeta = e.getValue();

                    if (offsetMeta == null) {
                        return new OffsetAndMetadata(topicId, topicPartition);
                    }

                    return new OffsetAndMetadata(topicId, topicPartition, offsetMeta);
                }).toList());

                return group;
            });
    }

    CompletableFuture<Void> alterConsumerGroupOffsets(Admin adminClient, String groupId,
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> alterRequest) {

        var alteredPartitions = alterRequest.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var deletedPartitions = alterRequest.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        var alterResults = adminClient.alterConsumerGroupOffsets(groupId, alteredPartitions);
        var deleteResults = adminClient.deleteConsumerGroupOffsets(groupId, deletedPartitions);

        var results = new ArrayList<CompletableFuture<Void>>();

        for (var entry : alterRequest.entrySet()) {
            var partition = entry.getKey();

            var promise = entry.getValue() == null
                    ? deleteResults.partitionResult(partition)
                    : alterResults.partitionResult(partition);

            results.add(promise.toCompletionStage()
                    .exceptionally(error -> {
                        if (error instanceof UnknownMemberIdException) {
                            throw GROUP_NOT_EMPTY;
                        }
                        if (error instanceof CompletionException ce) {
                            throw ce;
                        }
                        throw new CompletionException(error);
                    })
                    .toCompletableFuture());
        }

        return Promises.allOf(results);
    }

    private Map<TopicPartition, CompletableFuture<ListOffsetsResultInfo>> getListOffsetsResults(
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

    public CompletionStage<Void> deleteConsumerGroup(String requestGroupId) {
        Admin adminClient = kafkaContext.admin();
        String groupId = ConsumerGroup.decodeGroupId(requestGroupId);

        return adminClient.deleteConsumerGroups(List.of(groupId))
                .deletedGroups()
                .get(groupId)
                .toCompletionStage();
    }

    private CompletionStage<List<ConsumerGroup>> augmentList(Admin adminClient, List<ConsumerGroup> list, List<String> includes) {
        Map<String, ConsumerGroup> groups = list.stream().collect(Collectors.toMap(ConsumerGroup::groupId, Function.identity()));
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

    private void mergeDescriptions(ConsumerGroup group, Either<ConsumerGroup, Throwable> description) {
        if (description.isPrimaryEmpty()) {
            Throwable thrown = description.getAlternate();
            JsonApiError error = new JsonApiError("Unable to describe consumer group", thrown.getMessage(), thrown);
            group.addError(error);
            group.members(null);
            group.offsets(null);
            group.coordinator(null);
            group.authorizedOperations(null);
        } else {
            ConsumerGroup describedGroup = description.getPrimary();
            group.members(describedGroup.members());
            group.offsets(describedGroup.offsets());
            group.coordinator(describedGroup.coordinator());
            group.authorizedOperations(describedGroup.authorizedOperations());
        }
    }

    private CompletionStage<Map<String, Either<ConsumerGroup, Throwable>>> describeConsumerGroups(
            Admin adminClient,
            Collection<String> groupIds,
            List<String> includes) {

        Map<String, Either<ConsumerGroup, Throwable>> result = new LinkedHashMap<>(groupIds.size());

        var pendingTopicsIds = fetchTopicIdMap();

        var pendingDescribes = adminClient.describeConsumerGroups(groupIds,
                new DescribeConsumerGroupsOptions()
                    .includeAuthorizedOperations(includes.contains(ConsumerGroup.Fields.AUTHORIZED_OPERATIONS)))
                .describedGroups()
                .entrySet()
                .stream()
                .map(entry ->
                    entry.getValue()
                        .toCompletionStage()
                        .thenCombineAsync(pendingTopicsIds, (description, topicIds) -> {
                            permissionService.assertPermitted(ResourceTypes.Kafka.GROUPS, Privilege.GET, description.groupId());
                            return ConsumerGroup.fromKafkaModel(description, topicIds);
                        }, threadContext.currentContextExecutor())
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

    private CompletableFuture<Map<String, String>> fetchTopicIdMap() {
        return topicService.listTopics(true, true)
            .thenApply(topics -> topics.stream()
                .collect(Collectors.toMap(TopicListing::name, l -> l.topicId().toString())));
    }

    private CompletableFuture<Void> fetchOffsets(Admin adminClient, Map<String, ConsumerGroup> groups, Map<String, String> topicIds) {
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
                        .filter(topicPartition -> topicIds.containsKey(topicPartition.topic()))
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

    private void addOffsets(ConsumerGroup group,
            Map<String, String> topicIds,
            Map<TopicPartition, Either<ListOffsetsResultInfo, Throwable>> topicOffsets,
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> groupOffsets,
            Throwable thrown) {

        if (thrown != null) {
            group.addError(new JsonApiError("Unable to list consumer group offsets", thrown.getMessage(), thrown));
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
                            group.addError(new JsonApiError(msg, listOffsetsError.getMessage(), listOffsetsError));
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

            group.offsets(offsets);
        }
    }
}
