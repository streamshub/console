package com.github.eyefloaters.console.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import com.github.eyefloaters.console.api.model.Either;
import com.github.eyefloaters.console.api.model.OffsetInfo;
import com.github.eyefloaters.console.api.model.PartitionReplica;
import com.github.eyefloaters.console.api.model.ReplicaLocalStorage;
import com.github.eyefloaters.console.api.model.Topic;
import com.github.eyefloaters.console.api.model.TopicPartition;
import com.github.eyefloaters.console.api.support.KafkaOffsetSpec;
import com.github.eyefloaters.console.api.support.ListRequestContext;

@ApplicationScoped
public class TopicService {

    private static final List<OffsetSpec> DEFAULT_OFFSET_SPECS =
            List.of(OffsetSpec.earliest(), OffsetSpec.latest(), OffsetSpec.maxTimestamp());
    private static final Predicate<String> CONFIG_SORT =
            Pattern.compile("^-?configs\\..+$").asMatchPredicate();

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    ConfigService configService;

//    public CompletionStage<NewTopic> createTopic(NewTopic topic) {
//        Admin adminClient = clientSupplier.get();
//        String topicName = topic.getName();
//        org.apache.kafka.clients.admin.NewTopic newTopic;
//
//        if (topic.getReplicasAssignments() != null) {
//            newTopic = new org.apache.kafka.clients.admin.NewTopic(
//                    topicName,
//                    topic.getReplicasAssignments());
//        } else {
//            newTopic = new org.apache.kafka.clients.admin.NewTopic(
//                    topicName,
//                    Optional.ofNullable(topic.getNumPartitions()),
//                    Optional.ofNullable(topic.getReplicationFactor()));
//        }
//
//        newTopic.configs(topic.getConfigs());
//
//        CreateTopicsResult result = adminClient.createTopics(List.of(newTopic));
//
//        return result.all()
//                .thenApply(nothing -> NewTopic.fromKafkaModel(topicName, result))
//                .toCompletionStage();
//    }

    public CompletionStage<List<Topic>> listTopics(List<String> fields, String offsetSpec, ListRequestContext<Topic> listSupport) {
        List<String> fetchList = new ArrayList<>(fields);

        if (listSupport.getSortEntries().stream().anyMatch(CONFIG_SORT)) {
            fetchList.add(Topic.Fields.CONFIGS);
        }

        Admin adminClient = clientSupplier.get();

        return adminClient.listTopics()
                .listings()
                .thenApply(list -> list.stream().map(Topic::fromTopicListing).toList())
                .toCompletionStage()
                .thenCompose(list -> augmentList(adminClient, list, fetchList, offsetSpec))
                .thenApply(list -> list.stream()
                        .map(listSupport::tally)
                        .filter(listSupport::betweenCursors)
                        .sorted(listSupport.getSortComparator())
                        .dropWhile(listSupport::beforePageBegin)
                        .takeWhile(listSupport::pageCapacityAvailable)
                        .toList());
    }

    public CompletionStage<Topic> describeTopic(String topicId, List<String> fields, String offsetSpec) {
        Admin adminClient = clientSupplier.get();
        Uuid id = Uuid.fromString(topicId);

        return describeTopics(adminClient, List.of(id), fields, offsetSpec)
            .thenApply(result -> result.get(id))
            .thenApply(result -> result.getOptionalPrimary()
                    .orElseThrow(() -> new CompletionException(result.getAlternate())))
            .thenCompose(topic -> {
                CompletableFuture<Topic> promise = new CompletableFuture<>();

                if (fields.contains(Topic.Fields.CONFIGS)) {
                    List<ConfigResource> keys = List.of(new ConfigResource(ConfigResource.Type.TOPIC, topic.getName()));
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

//    public CompletionStage<Map<String, ConfigEntry>> alterConfigs(String topicName, Map<String, ConfigEntry> configs) {
//        return configService.alterConfigs(ConfigResource.Type.TOPIC, topicName, configs);
//    }

//    public CompletionStage<Void> createPartitions(String topicName, NewPartitions partitions) {
//        Admin adminClient = clientSupplier.get();
//
//        int totalCount = partitions.getTotalCount();
//        var newAssignments = partitions.getNewAssignments();
//
//        org.apache.kafka.clients.admin.NewPartitions newPartitions;
//
//        if (newAssignments != null) {
//            newPartitions = increaseTo(totalCount, newAssignments);
//        } else {
//            newPartitions = increaseTo(totalCount);
//        }
//
//        return adminClient.createPartitions(Map.of(topicName, newPartitions))
//                .all()
//                .toCompletionStage();
//    }

//    public CompletionStage<Map<String, Error>> deleteTopics(String... topicNames) {
//        Admin adminClient = clientSupplier.get();
//        Map<String, Error> errors = new HashMap<>();
//
//        var pendingDeletes = adminClient.deleteTopics(Arrays.asList(topicNames))
//                .topicNameValues()
//                .entrySet()
//                .stream()
//                .map(entry -> entry.getValue().whenComplete((nothing, thrown) -> {
//                    if (thrown != null) {
//                        errors.put(entry.getKey(), new Error("Unable to delete topic", thrown.getMessage(), thrown));
//                    }
//                }))
//                .map(KafkaFuture::toCompletionStage)
//                .map(CompletionStage::toCompletableFuture)
//                .toArray(CompletableFuture[]::new);
//
//        return CompletableFuture.allOf(pendingDeletes).thenApply(nothing -> errors);
//    }

    CompletionStage<List<Topic>> augmentList(Admin adminClient, List<Topic> list, List<String> fields, String offsetSpec) {
        Map<Uuid, Topic> topics = list.stream().collect(Collectors.toMap(t -> Uuid.fromString(t.getId()), Function.identity()));
        CompletableFuture<Void> configPromise = maybeDescribeConfigs(adminClient, topics, fields);
        CompletableFuture<Void> describePromise = maybeDescribeTopics(adminClient, topics, fields, offsetSpec);

        return CompletableFuture.allOf(configPromise, describePromise).thenApply(nothing -> list);
    }

    CompletableFuture<Void> maybeDescribeConfigs(Admin adminClient, Map<Uuid, Topic> topics, List<String> fields) {
        CompletableFuture<Void> promise = new CompletableFuture<>();

        if (fields.contains(Topic.Fields.CONFIGS)) {
            Map<String, Uuid> topicIds = new HashMap<>();
            List<ConfigResource> keys = topics.values().stream()
                    .map(topic -> {
                        topicIds.put(topic.getName(), Uuid.fromString(topic.getId()));
                        return topic.getName();
                    })
                    .map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name))
                    .toList();

            configService.describeConfigs(adminClient, keys)
                .thenApply(configs -> {
                    configs.forEach((name, either) -> topics.get(topicIds.get(name)).addConfigs(either));
                    promise.complete(null);
                    return true; // Match `completeExceptionally`
                })
                .exceptionally(promise::completeExceptionally);
        } else {
            promise.complete(null);
        }

        return promise;
    }

    CompletableFuture<Void> maybeDescribeTopics(Admin adminClient, Map<Uuid, Topic> topics, List<String> fields, String offsetSpec) {
        CompletableFuture<Void> promise = new CompletableFuture<>();

        if (fields.contains(Topic.Fields.PARTITIONS)
                || fields.contains(Topic.Fields.AUTHORIZED_OPERATIONS)
                || fields.contains(Topic.Fields.RECORD_COUNT)) {
            describeTopics(adminClient, topics.keySet(), fields, offsetSpec)
                .thenApply(descriptions -> {
                    descriptions.forEach((name, either) -> {
                        if (fields.contains(Topic.Fields.PARTITIONS)
                                || fields.contains(Topic.Fields.RECORD_COUNT)) {
                            topics.get(name).addPartitions(either);
                        }
                        if (fields.contains(Topic.Fields.AUTHORIZED_OPERATIONS)) {
                            topics.get(name).addAuthorizedOperations(either);
                        }
                    });

                    return promise.complete(null);
                })
                .exceptionally(promise::completeExceptionally);
        } else {
            promise.complete(null);
        }

        return promise;
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
                        if (error instanceof InvalidTopicException) {
                            // See KAFKA-15373
                            error = new UnknownTopicOrPartitionException(error);
                        }

                        result.put(
                                entry.getKey(),
                                Either.of(description, error, Topic::fromTopicDescription));
                        return null;
                    }))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        var promise = new CompletableFuture<Map<Uuid, Either<Topic, Throwable>>>();

        CompletableFuture.allOf(pendingDescribes)
                .thenCompose(nothing -> listOffsets(adminClient, result, offsetSpec))
                .thenCompose(nothing -> describeLogDirs(adminClient, result))
                .thenApply(nothing -> promise.complete(result))
                .exceptionally(promise::completeExceptionally);

        return promise;
    }

    CompletionStage<Void> listOffsets(Admin adminClient, Map<Uuid, Either<Topic, Throwable>> topics, String offsetSpec) {
        Map<String, Uuid> topicIds = new HashMap<>(topics.size());

        var pendingOffsets = getRequestOffsetSpecs(offsetSpec)
                .stream()
            .map(reqOffsetSpec -> topicPartitionReplicas(topics, topicIds)
                .keySet()
                .stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> reqOffsetSpec)))
            .flatMap(request -> listOffsets(adminClient, topics, topicIds, request))
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
     * Build of map of {@linkplain TopicPartition}s to the list of replicas where
     * the partitions are placed. Concurrently, a map of topic names to topic
     * identifiers is constructed to support cross referencing the
     * {@linkplain TopicPartition} keys (via {@linkplain TopicPartition#topic()})
     * back to the topic's {@linkplain Uuid}. This allows easy access of the topics
     * located in the topics map provided to this method and is particularly useful
     * for Kafka operations that still require topic name.
     *
     * @param topics   map of topics (keyed by Id)
     * @param topicIds map of topic names to topic Ids, modified by this method
     * @return map of {@linkplain TopicPartition}s to the list of replicas where the
     *         partitions are placed
     */
    Map<TopicPartition, List<Integer>> topicPartitionReplicas(Map<Uuid, Either<Topic, Throwable>> topics, Map<String, Uuid> topicIds) {
        return topics.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isPrimaryPresent())
                .map(entry -> {
                    var topic = entry.getValue().getPrimary();
                    topicIds.put(topic.getName(), entry.getKey());
                    return topic;
                })
                .filter(topic -> topic.getPartitions().isPrimaryPresent())
                .flatMap(topic -> topic.getPartitions().getPrimary()
                        .stream()
                        .map(partition -> {
                            var key = new TopicPartition(topic.getName(), partition.getPartition());
                            List<Integer> value = partition.getReplicas().stream().map(PartitionReplica::nodeId).toList();
                            return Map.entry(key, value);
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
            Map<TopicPartition, OffsetSpec> request) {

        var kafkaRequest = request.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey().toKafkaModel(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var result = adminClient.listOffsets(kafkaRequest);

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
        topic.getPartitions()
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

        var topicPartitionReplicas = topicPartitionReplicas(topics, topicIds);
        var nodeIds = topicPartitionReplicas.values().stream().flatMap(Collection::stream).distinct().toList();
        var logDirs = adminClient.describeLogDirs(nodeIds, new DescribeLogDirsOptions()
                .timeoutMs(5000))
                .descriptions();
        var promise = new CompletableFuture<Void>();

        var pendingInfo = topicPartitionReplicas.entrySet()
            .stream()
            .flatMap(e -> e.getValue().stream().map(node -> Map.entry(e.getKey(), node)))
            .map(e -> {
                var topicPartition = e.getKey().toKafkaModel();
                int nodeId = e.getValue();
                var partitionInfo = topics.get(topicIds.get(topicPartition.topic()))
                        .getPrimary()
                        .getPartitions()
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

        CompletableFuture.allOf(pendingInfo)
            .thenApply(nothing -> promise.complete(null))
            .exceptionally(promise::completeExceptionally);

        return promise;
    }

}
