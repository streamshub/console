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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.ListOffsetsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.Either;
import com.github.streamshub.console.api.model.OffsetInfo;
import com.github.streamshub.console.api.model.PartitionId;
import com.github.streamshub.console.api.model.PartitionInfo;
import com.github.streamshub.console.api.model.ReplicaLocalStorage;
import com.github.streamshub.console.api.model.Topic;
import com.github.streamshub.console.api.model.jsonapi.Identifier;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.KafkaOffsetSpec;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.UnknownTopicIdPatch;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.support.Identifiers;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.topic.KafkaTopic;

@ApplicationScoped
public class TopicDescribeService {

    private static final List<OffsetSpec> DEFAULT_OFFSET_SPECS =
            List.of(OffsetSpec.earliest(), OffsetSpec.latest(), OffsetSpec.maxTimestamp());
    private static final Predicate<String> CONFIG_SORT =
            Pattern.compile("^-?configs\\..+$").asMatchPredicate();
    private static final Set<String> REQUIRE_DESCRIBE = Set.of(
            Topic.Fields.PARTITIONS,
            Topic.Fields.NUM_PARTITIONS,
            Topic.Fields.AUTHORIZED_OPERATIONS,
            Topic.Fields.TOTAL_LEADER_LOG_BYTES,
            Topic.Fields.STATUS);
    private static final Set<String> REQUIRE_PARTITIONS = Set.of(
            Topic.Fields.PARTITIONS,
            Topic.Fields.NUM_PARTITIONS,
            Topic.Fields.TOTAL_LEADER_LOG_BYTES,
            Topic.Fields.STATUS);

    @Inject
    Logger logger;

    /**
     * ThreadContext of the request thread. This is used to execute asynchronous
     * tasks to allow access to request-scoped beans such as an injected
     * {@linkplain Admin Admin client}
     */
    @Inject
    ThreadContext threadContext;

    @Inject
    KafkaContext kafkaContext;

    /**
     * @see com.github.streamshub.console.api.support.InformerFactory#topics InformerFactory topics field
     */
    @Inject
    @Named("KafkaTopics")
    Map<String, Map<String, Map<String, KafkaTopic>>> managedTopics;

    @Inject
    PermissionService permissionService;

    @Inject
    ConfigService configService;

    @Inject
    GroupService groupService;

    public CompletionStage<List<Topic>> listTopics(List<String> fields, String offsetSpec, ListRequestContext<Topic> listSupport) {
        Set<String> fetchList = new LinkedHashSet<>(fields);
        List<String> sortFields = listSupport.getSortNames();

        if (sortFields.stream().anyMatch(CONFIG_SORT)) {
            fetchList.add(Topic.Fields.CONFIGS);
        }
        sortFields.stream().filter(Predicate.not(CONFIG_SORT)).forEach(fetchList::add);

        Admin adminClient = kafkaContext.admin();
        final Map<String, Integer> statuses = new HashMap<>();
        final AtomicInteger partitionCount = new AtomicInteger(0);

        var meta = listSupport.meta();
        permissionService.addPrivileges(meta, ResourceTypes.Kafka.TOPICS, null);
        meta.put("summary", Map.of(
                "statuses", statuses,
                "totalPartitions", partitionCount));

        return listTopics(true, true)
            .thenApply(list -> list.stream().map(Topic::fromTopicListing).toList())
            .thenComposeAsync(
                    list -> augmentList(adminClient, list, new ArrayList<>(fetchList), offsetSpec),
                    threadContext.currentContextExecutor())
            .thenApply(list -> list.stream()
                    .filter(listSupport.filter(Topic.class))
                    .map(topic -> tallySummary(statuses, partitionCount, topic))
                    .map(listSupport::tally)
                    .filter(listSupport::betweenCursors)
                    .sorted(listSupport.getSortComparator())
                    .dropWhile(listSupport::beforePageBegin)
                    .takeWhile(listSupport::pageCapacityAvailable))
            .thenApplyAsync(
                    topics -> topics
                        .map(this::setManaged)
                        .map(permissionService.addPrivileges(ResourceTypes.Kafka.TOPICS, Topic::name))
                        .toList(),
                    threadContext.currentContextExecutor());
    }

    private Topic tallySummary(Map<String, Integer> statuses, AtomicInteger partitionCount, Topic topic) {
        statuses.compute(topic.status(), (k, v) -> v == null ? 1 : v + 1);

        Integer numPartitions = topic.getAttributes().numPartitions();
        //numPartitions may be null if it was not included in the requested fields
        if (numPartitions != null) {
            partitionCount.addAndGet(numPartitions);
        }

        return topic;
    }

    CompletableFuture<List<TopicListing>> listTopics(boolean listInternal, boolean checkAuthorization) {
        Admin adminClient = kafkaContext.admin();
        Predicate<TopicListing> authorizationFilter;

        if (checkAuthorization) {
            authorizationFilter = permissionService.permitted(ResourceTypes.Kafka.TOPICS, Privilege.LIST, TopicListing::name);
        } else {
            authorizationFilter = x -> true;
        }

        return adminClient
            .listTopics(new ListTopicsOptions().listInternal(listInternal))
            .listings()
            .toCompletionStage()
            .thenApplyAsync(topics -> topics.stream()
                    .filter(authorizationFilter)
                    .toList(), threadContext.currentContextExecutor())
            .toCompletableFuture();
    }

    public CompletionStage<Optional<String>> topicNameForId(String topicId) {
        Uuid kafkaTopicId = Uuid.fromString(topicId);

        return listTopics(true, false)
            .thenApply(listings -> listings.stream()
                    .filter(topic -> kafkaTopicId.equals(topic.topicId()))
                    .findFirst()
                    .map(TopicListing::name));
    }

    public CompletionStage<Topic> describeTopic(String topicId, List<String> fields, String offsetSpec) {
        Admin adminClient = kafkaContext.admin();
        Uuid id = Uuid.fromString(topicId);

        CompletableFuture<Topic> describePromise = describeTopics(adminClient, List.of(id), fields, offsetSpec)
            .thenApply(result -> result.get(id))
            .thenApply(result -> result.getOrThrow(CompletionException::new))
            .thenApplyAsync(this::setManaged, threadContext.currentContextExecutor())
            .thenApplyAsync(
                    permissionService.addPrivileges(ResourceTypes.Kafka.TOPICS, Topic::name),
                    threadContext.currentContextExecutor())
            .toCompletableFuture();

        return describePromise.thenComposeAsync(topic -> {
            var topics = Map.of(id, topic);

            return CompletableFuture.allOf(
                    maybeDescribeConfigs(adminClient, topics, fields),
                    maybeFetchConsumerGroups(topics, fields))
                .thenApply(nothing -> topic);
        }, threadContext.currentContextExecutor());
    }

    Topic setManaged(Topic topic) {
        topic.addMeta("managed", getManagedTopic(topic.name())
                .map(kafkaTopic -> Boolean.TRUE)
                .orElse(Boolean.FALSE));
        return topic;
    }

    Optional<KafkaTopic> getManagedTopic(String topicName) {
        return Optional.ofNullable(kafkaContext.resource())
            .map(Kafka::getMetadata)
            .flatMap(kafkaMeta -> Optional.ofNullable(managedTopics.get(kafkaMeta.getNamespace()))
                    .map(clustersInNamespace -> clustersInNamespace.get(kafkaMeta.getName()))
                    .map(topicsInCluster -> topicsInCluster.get(topicName))
                    // Do not consider topics without a status set by Strimzi as managed
                    .filter(topic -> Objects.nonNull(topic.getStatus()))
                    .filter(this::isManaged));
    }

    boolean isManaged(KafkaTopic topic) {
        return Optional.of(topic)
            .map(KafkaTopic::getMetadata)
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> annotations.getOrDefault("strimzi.io/managed", "true"))
            .map(managed -> !"false".equals(managed))
            .orElse(true);
    }

    CompletionStage<List<Topic>> augmentList(Admin adminClient, List<Topic> list, List<String> fields, String offsetSpec) {
        Map<Uuid, Topic> topics = list.stream().collect(Collectors.toMap(t -> Uuid.fromString(t.getId()), Function.identity()));
        CompletableFuture<Void> configPromise = maybeDescribeConfigs(adminClient, topics, fields);
        CompletableFuture<Void> describePromise = maybeDescribeTopics(adminClient, topics, fields, offsetSpec);
        CompletableFuture<Void> consumerGroupPromise = maybeFetchConsumerGroups(topics, fields);

        return CompletableFuture.allOf(configPromise, describePromise, consumerGroupPromise)
                .thenApply(nothing -> list);
    }

    CompletableFuture<Void> maybeDescribeConfigs(Admin adminClient, Map<Uuid, Topic> topics, List<String> fields) {
        if (fields.contains(Topic.Fields.CONFIGS)) {
            Map<String, Uuid> topicIds = new HashMap<>();
            List<ConfigResource> keys = topics.values().stream()
                    .filter(e -> permissionService.permitted(ResourceTypes.Kafka.TOPICS, Privilege.GET, e.name()))
                    .map(topic -> {
                        topicIds.put(topic.name(), Uuid.fromString(topic.getId()));
                        return topic.name();
                    })
                    .map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name))
                    .toList();

            return configService.describeConfigs(adminClient, keys)
                .thenAccept(configs ->
                    configs.forEach((name, either) -> topics.get(topicIds.get(name)).addConfigs(either)))
                .toCompletableFuture();
        }

        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> maybeDescribeTopics(Admin adminClient, Map<Uuid, Topic> topics, List<String> fields, String offsetSpec) {
        if (REQUIRE_DESCRIBE.stream().anyMatch(fields::contains)) {
            Collection<Uuid> topicIds = topics.entrySet().stream()
                    .filter(e -> permissionService.permitted(ResourceTypes.Kafka.TOPICS, Privilege.GET, e.getValue().name()))
                    .map(Map.Entry::getKey)
                    .toList();

            return describeTopics(adminClient, topicIds, fields, offsetSpec)
                .<Void>thenApply(descriptions -> {
                    descriptions.forEach((id, either) -> {
                        if (REQUIRE_PARTITIONS.stream().anyMatch(fields::contains)) {
                            topics.get(id).addPartitions(either);
                        }
                        if (fields.contains(Topic.Fields.AUTHORIZED_OPERATIONS)) {
                            topics.get(id).addAuthorizedOperations(either);
                        }
                    });

                    return null;
                })
                .toCompletableFuture();
        }

        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> maybeFetchConsumerGroups(Map<Uuid, Topic> topics, List<String> fields) {
        if (!fields.contains(Topic.Fields.GROUPS)) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> searchTopics = topics.entrySet().stream()
                .filter(e -> permissionService.permitted(ResourceTypes.Kafka.TOPICS, Privilege.GET, e.getValue().name()))
                .map(Map.Entry::getKey)
                .map(Uuid::toString)
                .toList();

        return groupService
                .listGroupMembership(searchTopics)
                .thenAccept(groups ->
                    topics.forEach((topicId, topic) -> {
                        String idString = topicId.toString();

                        if (searchTopics.contains(idString)) {
                            var topicGroups = groups.getOrDefault(idString, Collections.emptyList());
                            var identifiers = topicGroups.stream()
                                    .map(Identifiers::encode)
                                    .map(g -> new Identifier("groups", g))
                                    .toList();
                            topic.groups().getData().addAll(identifiers);
                            topic.groups().addMeta("count", identifiers.size());
                        } else {
                            topic.groups(null);
                        }
                    }))
                .toCompletableFuture();
    }

    /* package */ CompletionStage<Map<Uuid, Either<Topic, Throwable>>> describeTopics(
            Admin adminClient,
            Collection<Uuid> topicIds,
            List<String> fields,
            String offsetSpec) {

        Map<Uuid, Either<Topic, Throwable>> result = new LinkedHashMap<>(topicIds.size());
        TopicCollection request = TopicCollection.ofTopicIds(topicIds);
        DescribeTopicsOptions options = new DescribeTopicsOptions()
                .includeAuthorizedOperations(fields.contains(Topic.Fields.AUTHORIZED_OPERATIONS));

        var pendingDescribes = adminClient.describeTopics(request, options)
                .topicIdValues()
                .entrySet()
                .stream()
                .map(entry ->
                    entry.getValue()
                        .toCompletionStage()
                        .<Void>handleAsync((description, error) -> {
                            if (error == null && !permissionService.permitted(ResourceTypes.Kafka.TOPICS, Privilege.GET, description.name())) {
                                error = permissionService.forbidden(ResourceTypes.Kafka.TOPICS, Privilege.GET, description.name());
                            }
                            result.put(
                                entry.getKey(),
                                Either.of(description,
                                        UnknownTopicIdPatch.apply(error, Function.identity()),
                                        Topic::fromTopicDescription));
                            return null;
                        }, threadContext.currentContextExecutor()))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingDescribes)
                .thenCompose(nothing -> CompletableFuture.allOf(
                        listOffsets(adminClient, result, offsetSpec).toCompletableFuture(),
                        describeLogDirs(adminClient, result).toCompletableFuture()
                ))
                .thenApply(nothing -> result);
    }

    private CompletionStage<Void> listOffsets(Admin adminClient, Map<Uuid, Either<Topic, Throwable>> topics, String offsetSpec) {
        Map<String, Uuid> topicIds = new HashMap<>(topics.size());
        var onlineTopics = topics.entrySet()
                .stream()
                .filter(topic -> topic.getValue()
                        .getOptionalPrimary()
                        .map(Topic::partitionsOnline)
                        .orElse(false))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var pendingOffsets = getRequestOffsetSpecs(offsetSpec)
            .stream()
            .map(reqOffsetSpec -> topicPartitionLeaders(onlineTopics, topicIds)
                .keySet()
                .stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> reqOffsetSpec)))
            .flatMap(request -> listOffsets(adminClient, onlineTopics, topicIds, request))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingOffsets);
    }

    private List<OffsetSpec> getRequestOffsetSpecs(String offsetSpec) {
        List<OffsetSpec> specs = new ArrayList<>(DEFAULT_OFFSET_SPECS);

        // Never null, defaults to latest
        switch (offsetSpec) { // NOSONAR
            case KafkaOffsetSpec.EARLIEST, KafkaOffsetSpec.LATEST, KafkaOffsetSpec.MAX_TIMESTAMP:
                break;
            default:
                specs.add(OffsetSpec.forTimestamp(Instant.parse(offsetSpec).toEpochMilli()));
                break;
        }

        return specs;
    }

    /**
     * Build of map of {@linkplain PartitionId}s to the partition leader node ID.
     * Concurrently, a map of topic names to topic identifiers is constructed to
     * support cross referencing the {@linkplain PartitionId} keys (via
     * {@linkplain PartitionId#topicId()}) back to the topic's {@linkplain Uuid}.
     * This allows easy access of the topics located in the topics map provided to
     * this method and is particularly useful for Kafka operations that still
     * require topic name.
     *
     * @param topics   map of topics (keyed by Id)
     * @param topicIds map of topic names to topic Ids, modified by this method
     * @return map of {@linkplain PartitionId}s to the partition leader node ID
     */
    private Map<PartitionId, Integer> topicPartitionLeaders(Map<Uuid, Either<Topic, Throwable>> topics, Map<String, Uuid> topicIds) {
        return topics.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isPrimaryPresent())
                .map(entry -> {
                    var topic = entry.getValue().getPrimary();
                    topicIds.put(topic.name(), entry.getKey());
                    return topic;
                })
                .filter(topic -> topic.partitions().isPrimaryPresent())
                .flatMap(topic -> topic.partitions().getPrimary()
                        .stream()
                        .filter(PartitionInfo::online)
                        .map(partition -> {
                            var key = new PartitionId(topic.getId(), topic.name(), partition.getPartition());
                            return Map.entry(key, partition.getLeaderId());
                        }))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getOffsetKey(OffsetSpec spec) {
        if (spec instanceof OffsetSpec.EarliestSpec) {
            return KafkaOffsetSpec.EARLIEST;
        }
        if (spec instanceof OffsetSpec.LatestSpec) {
            return KafkaOffsetSpec.LATEST;
        }
        if (spec instanceof OffsetSpec.MaxTimestampSpec) {
            return KafkaOffsetSpec.MAX_TIMESTAMP;
        }
        return "timestamp";
    }

    private Stream<CompletionStage<Void>> listOffsets(
            Admin adminClient,
            Map<Uuid, Either<Topic, Throwable>> topics,
            Map<String, Uuid> topicIds,
            Map<PartitionId, OffsetSpec> request) {

        var kafkaRequest = request.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey().toKafkaModel(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var result = adminClient.listOffsets(kafkaRequest, new ListOffsetsOptions()
                .timeoutMs(5000));

        return kafkaRequest.entrySet()
                .stream()
                .map(entry -> result.partitionResult(entry.getKey())
                        .toCompletionStage()
                        .<Void>handle((offsetResult, error) -> {
                            addOffset(topics.get(topicIds.get(entry.getKey().topic())).getPrimary(),
                                        entry.getKey().partition(),
                                        getOffsetKey(entry.getValue()),
                                        offsetResult,
                                        error);
                            return null;
                        }));

    }

    private void addOffset(Topic topic, int partitionNo, String key, ListOffsetsResultInfo result, Throwable error) {
        topic.partitions()
            .getPrimary()
            .stream()
            .filter(partition -> partition.getPartition() == partitionNo)
            .findFirst()
            .ifPresent(partition -> partition.addOffset(key, either(result, error)));
    }

    private Either<OffsetInfo, Throwable> either(ListOffsetsResultInfo result, Throwable error) {
        Function<ListOffsetsResultInfo, OffsetInfo> transformer = offsetInfo -> {
            Instant timestamp = offsetInfo.timestamp() != -1 ? Instant.ofEpochMilli(offsetInfo.timestamp()) : null;
            return new OffsetInfo(offsetInfo.offset(), timestamp, offsetInfo.leaderEpoch().orElse(null));
        };

        return Either.of(result, error, transformer);
    }

    private CompletionStage<Void> describeLogDirs(Admin adminClient, Map<Uuid, Either<Topic, Throwable>> topics) {
        Map<String, Uuid> topicIds = new HashMap<>(topics.size());

        var topicPartitionReplicas = topicPartitionLeaders(topics, topicIds);
        var nodeIds = topicPartitionReplicas.values().stream().distinct().toList();
        var logDirs = adminClient.describeLogDirs(nodeIds, new DescribeLogDirsOptions()
                .timeoutMs(5000))
                .descriptions();

        var pendingInfo = topicPartitionReplicas.entrySet()
            .stream()
            .map(e -> {
                var topicPartition = e.getKey().toKafkaModel();
                int nodeId = e.getValue();
                var partitionInfo = topics.get(topicIds.get(topicPartition.topic()))
                        .getPrimary()
                        .partitions()
                        .getPrimary()
                        .stream()
                        .filter(p -> p.getPartition() == topicPartition.partition())
                        .findFirst();

                return logDirs.get(nodeId).toCompletionStage().<Void>handle((nodeLogDirs, error) -> {
                    if (error != null) {
                        partitionInfo.ifPresent(p -> p.setReplicaLocalStorage(nodeId, Either.ofAlternate(error)));
                    } else {
                        nodeLogDirs.values()
                            .stream()
                            .map(dir -> dir.replicaInfos())
                            .map(replicas -> replicas.get(topicPartition))
                            .filter(Objects::nonNull)
                            .map(org.apache.kafka.clients.admin.ReplicaInfo.class::cast)
                            .map(ReplicaLocalStorage::fromKafkaModel)
                            .forEach(replicaInfo -> partitionInfo.ifPresent(p -> p.setReplicaLocalStorage(nodeId, Either.of(replicaInfo))));
                    }

                    return null;
                });
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingInfo);
    }

}
