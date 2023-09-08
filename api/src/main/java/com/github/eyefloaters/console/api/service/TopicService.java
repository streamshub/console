package com.github.eyefloaters.console.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
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
import com.github.eyefloaters.console.api.model.Topic;
import com.github.eyefloaters.console.api.support.ComparatorBuilder;
import com.github.eyefloaters.console.api.support.KafkaOffsetSpec;
import com.github.eyefloaters.console.api.support.ListRequestContext;

@ApplicationScoped
public class TopicService {

    private static final List<OffsetSpec> DEFAULT_OFFSET_SPECS =
            List.of(OffsetSpec.earliest(), OffsetSpec.latest(), OffsetSpec.maxTimestamp());

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    ConfigService configService;

    final ComparatorBuilder<Topic> comparators =
            new ComparatorBuilder<>(Topic.Fields::comparator, Topic.Fields.defaultComparator());

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

    public CompletionStage<List<Topic>> listTopics(List<String> fields, String offsetSpec, ListRequestContext listSupport) {
        List<String> fetchList = new ArrayList<>(fields);
        String sort = listSupport.getSort();
        if (sort != null && sort.contains("configs[")) {
            fetchList.add(Topic.Fields.CONFIGS);
        }
        Admin adminClient = clientSupplier.get();

        return adminClient.listTopics()
                .listings()
                .thenApply(list -> list.stream().map(Topic::fromTopicListing).toList())
                .toCompletionStage()
                .thenCompose(list -> augmentList(adminClient, list, fetchList, offsetSpec))
                .thenApply(list -> list.stream()
                        .sorted(comparators.fromSort(listSupport.getSort()))
                        .toList());
    }

    public CompletionStage<Topic> describeTopic(String topicId, List<String> fields, String offsetSpec) {
        Admin adminClient = clientSupplier.get();
        Uuid id = Uuid.fromString(topicId);

        return describeTopics(adminClient, List.of(id), fields, offsetSpec)
            .thenApply(result -> result.get(id))
            .thenApply(result -> {
                if (result.isPrimaryPresent()) {
                    return result.getPrimary();
                }
                throw new CompletionException(result.getAlternate());
            })
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
                .thenApply(nothing -> promise.complete(result))
                .exceptionally(promise::completeExceptionally);

        return promise;
    }

    CompletionStage<Void> listOffsets(Admin adminClient, Map<Uuid, Either<Topic, Throwable>> topics, String offsetSpec) {
        Map<String, Uuid> topicIds = new HashMap<>(topics.size());

        var pendingOffsets = getRequestOffsetSpecs(offsetSpec)
                .stream()
            .map(reqOffsetSpec -> getTopicPartitions(topics, topicIds)
                .stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> reqOffsetSpec)))
            .flatMap(request -> listOffsets(adminClient, topics, topicIds, request))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);

        var promise = new CompletableFuture<Void>();

        CompletableFuture.allOf(pendingOffsets)
                .thenApply(nothing -> promise.complete(null))
                .exceptionally(promise::completeExceptionally);

        return promise;
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

    List<org.apache.kafka.common.TopicPartition> getTopicPartitions(Map<Uuid, Either<Topic, Throwable>> topics, Map<String, Uuid> topicIds) {
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
                        .map(partition -> new org.apache.kafka.common.TopicPartition(topic.getName(), partition.getPartition())))
                .toList();
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
            Map<org.apache.kafka.common.TopicPartition, OffsetSpec> request) {

        var result = adminClient.listOffsets(request);

        return request.entrySet()
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
}
