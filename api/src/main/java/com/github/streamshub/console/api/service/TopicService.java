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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreatePartitionsOptions;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.ListOffsetsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.Either;
import com.github.streamshub.console.api.model.Identifier;
import com.github.streamshub.console.api.model.NewTopic;
import com.github.streamshub.console.api.model.OffsetInfo;
import com.github.streamshub.console.api.model.PartitionId;
import com.github.streamshub.console.api.model.PartitionInfo;
import com.github.streamshub.console.api.model.ReplicaLocalStorage;
import com.github.streamshub.console.api.model.Topic;
import com.github.streamshub.console.api.model.TopicPatch;
import com.github.streamshub.console.api.support.AdminDecorator;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.KafkaOffsetSpec;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.TopicValidation;
import com.github.streamshub.console.api.support.UnknownTopicIdPatch;
import com.github.streamshub.console.api.support.ValidationProxy;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.topic.KafkaTopic;

import static org.apache.kafka.clients.admin.NewPartitions.increaseTo;

@ApplicationScoped
public class TopicService {

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
    ValidationProxy validationService;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    AdminDecorator admin;

    @Inject
    @Named("KafkaTopics")
    Map<String, Map<String, Map<String, KafkaTopic>>> managedTopics;

    @Inject
    KubernetesClient k8s;

    @Inject
    ConfigService configService;

    @Inject
    ConsumerGroupService consumerGroupService;

    public CompletionStage<NewTopic> createTopic(NewTopic topic, boolean validateOnly) {
        Kafka kafka = kafkaContext.resource();
        Admin adminClient = kafkaContext.admin();

        validationService.validate(new TopicValidation.NewTopicInputs(kafka, Collections.emptyMap(), topic));

        String topicName = topic.name();
        org.apache.kafka.clients.admin.NewTopic newTopic;

        if (topic.replicasAssignments() != null) {
            newTopic = new org.apache.kafka.clients.admin.NewTopic(
                    topicName,
                    topic.replicasAssignments()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> Integer.valueOf(e.getKey()), Map.Entry::getValue)));
        } else {
            newTopic = new org.apache.kafka.clients.admin.NewTopic(
                    topicName,
                    Optional.ofNullable(topic.numPartitions()),
                    Optional.ofNullable(topic.replicationFactor()));
        }

        if (topic.configs() != null) {
            newTopic.configs(topic.configs()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue())));
        }

        CreateTopicsResult result = adminClient
                .createTopics(List.of(newTopic), new CreateTopicsOptions().validateOnly(validateOnly));

        return result.all()
                .thenApply(nothing -> NewTopic.fromKafkaModel(topicName, result))
                .toCompletionStage();
    }

    public CompletionStage<List<Topic>> listTopics(List<String> fields, String offsetSpec, ListRequestContext<Topic> listSupport) {
        List<String> fetchList = new ArrayList<>(fields);

        if (listSupport.getSortEntries().stream().anyMatch(CONFIG_SORT)) {
            fetchList.add(Topic.Fields.CONFIGS);
        }

        Admin adminClient = kafkaContext.admin();
        final Map<String, Integer> statuses = new HashMap<>();
        listSupport.meta().put("summary", Map.of("statuses", statuses));

        return listTopics(true)
            .thenApply(list -> list.stream().map(Topic::fromTopicListing).toList())
            .thenComposeAsync(
                    list -> augmentList(adminClient, list, fetchList, offsetSpec),
                    threadContext.currentContextExecutor())
            .thenApply(list -> list.stream()
                    .filter(listSupport)
                    .map(topic -> tallyStatus(statuses, topic))
                    .map(listSupport::tally)
                    .filter(listSupport::betweenCursors)
                    .sorted(listSupport.getSortComparator())
                    .dropWhile(listSupport::beforePageBegin)
                    .takeWhile(listSupport::pageCapacityAvailable))
            .thenApplyAsync(
                    topics -> topics.map(this::setManaged).toList(),
                    threadContext.currentContextExecutor());
    }

    Topic tallyStatus(Map<String, Integer> statuses, Topic topic) {
        statuses.compute(topic.status(), (k, v) -> v == null ? 1 : v + 1);
        return topic;
    }

    CompletableFuture<List<TopicListing>> listTopics(boolean listInternal) {
        return this.admin
            .listTopics(new ListTopicsOptions().listInternal(listInternal))
            .toCompletableFuture();
    }

    public CompletionStage<Topic> describeTopic(String topicId, List<String> fields, String offsetSpec) {
        Admin adminClient = kafkaContext.admin();
        Uuid id = Uuid.fromString(topicId);

        CompletableFuture<Topic> describePromise = describeTopics(adminClient, List.of(id), fields, offsetSpec)
            .thenApply(result -> result.get(id))
            .thenApply(result -> result.getOrThrow(CompletionException::new))
            .thenApplyAsync(this::setManaged, threadContext.currentContextExecutor())
            .toCompletableFuture();

        return describePromise.thenComposeAsync(topic -> {
            var topics = Map.of(id, topic);

            return CompletableFuture.allOf(
                    maybeDescribeConfigs(adminClient, topics, fields),
                    maybeFetchConsumerGroups(topics, fields))
                .thenApply(nothing -> topic);
        }, threadContext.currentContextExecutor());
    }

    /**
     * Apply the provided topic patch request to an existing topic, its configurations,
     * and its replica assignments. The following operations may be performed depending on
     * the request.
     *
     * <ul>
     * <li>Create new partitions with or without replicas assignments
     * <li>Alter partition assignments for existing partitions
     * <li>Alter (modify or delete/revert to default) topic configurations
     * </ul>
     */
    public CompletionStage<Void> patchTopic(String topicId, TopicPatch patch, boolean validateOnly) {
        Kafka kafka = kafkaContext.resource();

        return describeTopic(topicId, List.of(Topic.Fields.CONFIGS), KafkaOffsetSpec.LATEST)
            .thenApply(topic -> validationService.validate(new TopicValidation.TopicPatchInputs(kafka, topic, patch)))
            .thenApply(TopicValidation.TopicPatchInputs::topic)
            .thenComposeAsync(topic -> getManagedTopic(topic.name())
                    .map(kafkaTopic -> patchManagedTopic())
                    .orElseGet(() -> patchUnmanagedTopic(topic, patch, validateOnly)),
                    threadContext.currentContextExecutor());
    }

    // Modifications disabled for now
    CompletionStage<Void> patchManagedTopic(/*KafkaTopic topic, TopicPatch patch, boolean validateOnly*/) {
        return CompletableFuture.completedStage(null);
//        if (validateOnly) { // NOSONAR
//            return CompletableFuture.completedStage(null);
//        }
//
//        Map<String, Object> modifiedConfig = Optional.ofNullable(patch.configs())
//            .map(Map::entrySet)
//            .map(Collection::stream)
//            .orElseGet(Stream::empty)
//            .map(e -> Map.entry(e.getKey(), e.getValue().getValue()))
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//        KafkaTopic modifiedTopic = new KafkaTopicBuilder(topic)
//            .editSpec()
//                .withPartitions(patch.numPartitions())
//                .withReplicas(patch.replicasAssignments()
//                        .values()
//                        .stream()
//                        .findFirst()
//                        .map(Collection::size)
//                        .orElseGet(() -> topic.getSpec().getReplicas()))
//                .addToConfig(modifiedConfig)
//            .endSpec()
//            .build();
//
//        return CompletableFuture.runAsync(() -> k8s.resource(modifiedTopic).serverSideApply());
    }

    CompletionStage<Void> patchUnmanagedTopic(Topic topic, TopicPatch patch, boolean validateOnly) {
        List<CompletableFuture<Void>> pending = new ArrayList<>();

        pending.add(maybeCreatePartitions(topic, patch, validateOnly));

        if (!validateOnly) {
            pending.addAll(maybeAlterPartitionAssignments(topic, patch));
        }

        pending.add(maybeAlterConfigs(topic, patch, validateOnly));

        return CompletableFuture.allOf(pending.stream().toArray(CompletableFuture[]::new))
            .whenComplete((nothing, error) -> {
                if (error != null) {
                    pending.stream()
                        .filter(CompletableFuture::isCompletedExceptionally)
                        .forEach(fut -> fut.exceptionally(ex -> {
                            if (ex instanceof CompletionException ce) {
                                ex = ce.getCause();
                            }
                            error.addSuppressed(ex);
                            return null;
                        }));
                }
            });
    }

    CompletableFuture<Void> maybeCreatePartitions(Topic topic, TopicPatch topicPatch, boolean validateOnly) {
        int currentNumPartitions = topic.partitions().getPrimary().size();
        int newNumPartitions = Optional.ofNullable(topicPatch.numPartitions()).orElse(currentNumPartitions);

        if (newNumPartitions > currentNumPartitions) {
            List<List<Integer>> newAssignments = IntStream.range(currentNumPartitions, newNumPartitions)
                    .filter(topicPatch::hasReplicaAssignment)
                    .mapToObj(topicPatch::replicaAssignment)
                    .toList();

            return createPartitions(topic.name(), newNumPartitions, newAssignments, validateOnly)
                    .toCompletableFuture();
        }

        return CompletableFuture.completedFuture(null);
    }

    CompletionStage<Void> createPartitions(String topicName, int totalCount, List<List<Integer>> newAssignments, boolean validateOnly) {
        Admin adminClient = kafkaContext.admin();

        org.apache.kafka.clients.admin.NewPartitions newPartitions;

        if (newAssignments.isEmpty()) {
            logger.infof("Increasing numPartitions for topic %s to %d", topicName, totalCount);
            newPartitions = increaseTo(totalCount);
        } else {
            logger.infof("Increasing numPartitions for topic %s to %d with new assignments %s", topicName, totalCount, newAssignments);
            newPartitions = increaseTo(totalCount, newAssignments);
        }

        return adminClient.createPartitions(Map.of(topicName, newPartitions), new CreatePartitionsOptions()
                .validateOnly(validateOnly))
                .all()
                .toCompletionStage();
    }

    List<CompletableFuture<Void>> maybeAlterPartitionAssignments(Topic topic, TopicPatch topicPatch) {
        int currentNumPartitions = topic.partitions().getPrimary().size();

        var alteredAssignments = IntStream.range(0, currentNumPartitions)
                .filter(topicPatch::hasReplicaAssignment)
                .mapToObj(partitionId -> {
                    List<Integer> reassignments = topicPatch.replicaAssignment(partitionId);
                    var key = new TopicPartition(topic.name(), partitionId);

                    if (reassignments.isEmpty()) {
                        return Map.entry(key, Optional.<NewPartitionReassignment>empty());
                    }

                    return Map.entry(key, Optional.of(new NewPartitionReassignment(reassignments)));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (alteredAssignments.isEmpty()) {
            return Collections.emptyList();
        }

        Admin adminClient = kafkaContext.admin();

        if (logger.isDebugEnabled()) {
            logPartitionReassignments(topic, alteredAssignments);
        }

        return adminClient.alterPartitionReassignments(alteredAssignments)
                .values()
                .values()
                .stream()
                .map(f -> f.toCompletionStage())
                .map(CompletionStage::toCompletableFuture)
                .toList();
    }

    void logPartitionReassignments(Topic topic,
            Map<org.apache.kafka.common.TopicPartition, Optional<NewPartitionReassignment>> alteredAssignments) {

        StringBuilder changes = new StringBuilder();
        Map<Integer, List<String>> currentAssignments = topic.partitions()
                .getPrimary()
                .stream()
                .map(p -> Map.entry(p.getPartition(), p.getReplicas().stream().map(r -> Integer.toString(r.nodeId())).toList()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        alteredAssignments.entrySet().stream().forEach(alterEntry -> {
            int partition = alterEntry.getKey().partition();

            changes.append("{ partition=%d: ".formatted(partition));
            changes.append("from=[ ");
            changes.append(String.join(", ", currentAssignments.get(partition)));
            changes.append(" ] to=");

            changes.append(alterEntry.getValue()
                .map(reassignment -> reassignment.targetReplicas()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "[ ", " ]")))
                .orElse("[]"));

            changes.append(" }");
        });

        logger.debugf("Altering partition reassignments for cluster %s[%s], topic %s[%s], changes=[ %s ]",
                kafkaContext.resource().getMetadata().getName(),
                kafkaContext.resource().getStatus().getClusterId(),
                topic.name(),
                topic.getId(),
                changes);
    }

    CompletableFuture<Void> maybeAlterConfigs(Topic topic, TopicPatch topicPatch, boolean validateOnly) {
        return Optional.ofNullable(topicPatch.configs())
            .filter(Predicate.not(Map::isEmpty))
            .map(configs -> configService.alterConfigs(ConfigResource.Type.TOPIC, topic.name(), configs, validateOnly)
                .toCompletableFuture())
            .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    public CompletionStage<Void> deleteTopic(String topicId) {
        Admin adminClient = kafkaContext.admin();
        Uuid id = Uuid.fromString(topicId);

        return adminClient.deleteTopics(TopicCollection.ofTopicIds(List.of(id)))
                .topicIdValues()
                .get(id)
                .toCompletionStage();
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

    CompletableFuture<Void> maybeDescribeTopics(Admin adminClient, Map<Uuid, Topic> topics, List<String> fields, String offsetSpec) {
        if (REQUIRE_DESCRIBE.stream().anyMatch(fields::contains)) {
            return describeTopics(adminClient, topics.keySet(), fields, offsetSpec)
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

    CompletableFuture<Void> maybeFetchConsumerGroups(Map<Uuid, Topic> topics, List<String> fields) {
        CompletionStage<Map<String, List<String>>> pendingConsumerGroups;

        if (fields.contains(Topic.Fields.CONSUMER_GROUPS)) {
            var topicIds = topics.keySet().stream().map(Uuid::toString).toList();
            pendingConsumerGroups = consumerGroupService.listConsumerGroupMembership(topicIds);
        } else {
            pendingConsumerGroups = CompletableFuture.completedStage(Collections.emptyMap());
        }

        return pendingConsumerGroups.thenAccept(consumerGroups ->
                    consumerGroups.entrySet()
                        .stream()
                        .forEach(e -> {
                            Topic topic = topics.get(Uuid.fromString(e.getKey()));
                            var identifiers = e.getValue().stream().map(g -> new Identifier("consumerGroups", g)).toList();
                            topic.consumerGroups().data().addAll(identifiers);
                            topic.consumerGroups().addMeta("count", identifiers.size());
                        }))
                .toCompletableFuture();
    }

    CompletionStage<Map<Uuid, Either<Topic, Throwable>>> describeTopics(
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
                    entry.getValue().toCompletionStage().<Void>handle((description, error) -> {
                        error = UnknownTopicIdPatch.apply(error, Function.identity());

                        result.put(
                                entry.getKey(),
                                Either.of(description, error, Topic::fromTopicDescription));
                        return null;
                    }))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingDescribes)
                .thenCompose(nothing -> CompletableFuture.allOf(
                        listOffsets(adminClient, result, offsetSpec).toCompletableFuture(),
                        describeLogDirs(adminClient, result).toCompletableFuture()
                ))
                .thenApply(nothing -> result);
    }

    CompletionStage<Void> listOffsets(Admin adminClient, Map<Uuid, Either<Topic, Throwable>> topics, String offsetSpec) {
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

    List<OffsetSpec> getRequestOffsetSpecs(String offsetSpec) {
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
    Map<PartitionId, Integer> topicPartitionLeaders(Map<Uuid, Either<Topic, Throwable>> topics, Map<String, Uuid> topicIds) {
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

    String getOffsetKey(OffsetSpec spec) {
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

    Stream<CompletionStage<Void>> listOffsets(
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

    void addOffset(Topic topic, int partitionNo, String key, ListOffsetsResultInfo result, Throwable error) {
        topic.partitions()
            .getPrimary()
            .stream()
            .filter(partition -> partition.getPartition() == partitionNo)
            .findFirst()
            .ifPresent(partition -> partition.addOffset(key, either(result, error)));
    }

    Either<OffsetInfo, Throwable> either(ListOffsetsResultInfo result, Throwable error) {
        Function<ListOffsetsResultInfo, OffsetInfo> transformer = offsetInfo -> {
            Instant timestamp = offsetInfo.timestamp() != -1 ? Instant.ofEpochMilli(offsetInfo.timestamp()) : null;
            return new OffsetInfo(offsetInfo.offset(), timestamp, offsetInfo.leaderEpoch().orElse(null));
        };

        return Either.of(result, error, transformer);
    }

    CompletionStage<Void> describeLogDirs(Admin adminClient, Map<Uuid, Either<Topic, Throwable>> topics) {
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
