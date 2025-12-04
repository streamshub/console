package com.github.streamshub.console.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreatePartitionsOptions;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.ConfigEntry;
import com.github.streamshub.console.api.model.NewTopic;
import com.github.streamshub.console.api.model.Topic;
import com.github.streamshub.console.api.model.TopicPatch;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.KafkaOffsetSpec;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.TopicValidation;
import com.github.streamshub.console.api.support.ValidationProxy;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.kafka.Status;
import io.strimzi.api.kafka.model.kafka.entityoperator.EntityOperatorSpec;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;
import io.strimzi.api.kafka.model.topic.KafkaTopicStatus;

import static org.apache.kafka.clients.admin.NewPartitions.increaseTo;

@ApplicationScoped
public class TopicService {

    public static final int TOPIC_OPERATION_LIMIT = 20;

    @Inject
    Logger logger;

    @Inject
    ScheduledExecutorService scheduler;

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
    StrimziResourceService strimziService;

    @Inject
    PermissionService permissionService;

    @Inject
    ConfigService configService;

    @Inject
    TopicDescribeService topicDescribe;

    @Inject
    NodeService nodeService;

    public CompletionStage<NewTopic> createTopic(NewTopic topic, boolean validateOnly) {
        permissionService.assertPermitted(ResourceTypes.Kafka.TOPICS, Privilege.CREATE, topic.name());

        return validate(topic).thenComposeAsync(
                nothing -> createTopicValidated(topic, validateOnly),
                threadContext.currentContextExecutor()
        );
    }

    public CompletionStage<List<Topic>> listTopics(List<String> fields, String offsetSpec, ListRequestContext<Topic> listSupport) {
        return topicDescribe.listTopics(fields, offsetSpec, listSupport);
    }

    public CompletionStage<Topic> describeTopic(String topicId, List<String> fields, String offsetSpec) {
        return topicDescribe.describeTopic(topicId, fields, offsetSpec);
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
        return describeTopic(topicId, List.of(Topic.Fields.CONFIGS), KafkaOffsetSpec.LATEST)
            .thenComposeAsync(
                    topic -> validate(topic, patch),
                    threadContext.currentContextExecutor()
            )
            .thenComposeAsync(
                    topic -> patchTopicValidated(topic, patch, validateOnly),
                    threadContext.currentContextExecutor()
            );
    }

    public CompletionStage<Void> deleteTopic(String topicId) {
        Admin adminClient = kafkaContext.admin();
        Uuid id = Uuid.fromString(topicId);

        return topicDescribe.topicNameForId(topicId)
                .thenApply(topicName -> {
                    if (topicName.isEmpty()) {
                        throw new UnknownTopicIdException("No such topic: " + topicId);
                    }
                    return topicName.get();
                })
                .thenComposeAsync(topicName -> topicDescribe.getManagedTopic(topicName)
                        .map(kafkaTopic -> CompletableFuture.runAsync(() -> strimziService.deleteTopic(kafkaTopic)))
                        .orElseGet(() -> adminClient.deleteTopics(TopicCollection.ofTopicIds(List.of(id)))
                                .topicIdValues()
                                .get(id)
                                .toCompletionStage()
                                .toCompletableFuture()),
                    threadContext.currentContextExecutor());
    }

    private CompletionStage<Void> validate(NewTopic topic) {
        return nodeService.listNodes()
                .thenAccept(nodes -> validationService.validate(new TopicValidation.NewTopicInputs(
                        nodes,
                        Collections.emptyMap(),
                        topic
                )));
    }

    private CompletionStage<NewTopic> createTopicValidated(NewTopic topic, boolean validateOnly) {
        String topicName = topic.name();
        org.apache.kafka.clients.admin.NewTopic newTopic;
        Optional<Short> replicationFactor;

        if (topic.replicasAssignments() != null) {
            replicationFactor = topic.replicasAssignments()
                    .values()
                    .stream()
                    .findFirst()
                    .map(List::size)
                    .map(Integer::shortValue);
            newTopic = new org.apache.kafka.clients.admin.NewTopic(
                    topicName,
                    topic.replicasAssignments()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> Integer.valueOf(e.getKey()), Map.Entry::getValue)));
        } else {
            replicationFactor = Optional.ofNullable(topic.replicationFactor());
            newTopic = new org.apache.kafka.clients.admin.NewTopic(
                    topicName,
                    Optional.ofNullable(topic.numPartitions()),
                    replicationFactor);
        }

        if (topic.configs() != null) {
            newTopic.configs(topic.configs()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue())));
        }

        Optional<Kafka> kafka = Optional.ofNullable(kafkaContext.resource());
        boolean managedTopics = kafka
                .map(Kafka::getSpec)
                .map(KafkaSpec::getEntityOperator)
                .map(EntityOperatorSpec::getTopicOperator)
                .isPresent();

        if (managedTopics && !validateOnly) {
            // Ignore Sonar warning - kafka is present when managedTopics == true
            return createManagedTopic(kafka.get(), newTopic, replicationFactor); // NOSONAR
        } else {
            Admin adminClient = kafkaContext.admin();
            CreateTopicsResult result = adminClient
                    .createTopics(List.of(newTopic), new CreateTopicsOptions().validateOnly(validateOnly));

            return result.all()
                    .thenApply(nothing -> NewTopic.fromKafkaModel(topicName, result))
                    .toCompletionStage();
        }
    }

    private CompletionStage<NewTopic> createManagedTopic(
            Kafka kafka,
            org.apache.kafka.clients.admin.NewTopic newTopic,
            Optional<Short> replicationFactor) {

        Map<String, Object> newConfigs = Optional.ofNullable(newTopic.configs())
                .<Map<String, Object>>map(LinkedHashMap::new)
                .orElse(null);

        KafkaTopic topicResource = new KafkaTopicBuilder()
                .withNewMetadata()
                    .withNamespace(kafka.getMetadata().getNamespace())
                    .withName(newTopic.name())
                    .addToLabels("strimzi.io/cluster", kafka.getMetadata().getName())
                .endMetadata()
                .withNewSpec()
                    .withTopicName(newTopic.name())
                    .withPartitions(newTopic.numPartitions())
                    .withReplicas(replicationFactor.map(Integer::valueOf).orElse(null))
                    .withConfig(newConfigs)
                .endSpec()
                .build();

        class TopicCreationCheck extends TopicCheck<NewTopic> {
            TopicCreationCheck() {
                super(threadContext, scheduler);
            }

            @Override
            void pollTopic() {
                Optional<KafkaTopic> managedTopic = topicDescribe.getManagedTopic(newTopic.name());
                boolean ready = managedTopic
                    .map(KafkaTopic::getStatus)
                    .map(KafkaTopicStatus::getConditions)
                    .map(List::stream)
                    .orElseGet(Stream::empty)
                    .filter(c -> "Ready".equals(c.getType()))
                    .anyMatch(c -> "True".equals(c.getStatus()));

                if (managedTopic.isEmpty() || !ready) {
                    schedule(100);
                    return;
                }

                topicDescribe.describeTopic(
                        managedTopic.get().getStatus().getTopicId(),
                        List.of(Topic.Fields.CONFIGS, Topic.Fields.PARTITIONS),
                        KafkaOffsetSpec.LATEST)
                    .thenAccept(createdTopic -> {
                        if (createdTopic.partitions().isPrimaryEmpty()
                                || createdTopic.configs().isPrimaryEmpty()) {
                            // Topic hasn't fully been created, check again
                            schedule(100);
                        } else {
                            NewTopic response = new NewTopic(
                                newTopic.name(),
                                createdTopic.getId(),
                                createdTopic.partitions().getPrimary().size(),
                                replicationFactor.orElse(null),
                                Collections.emptyMap(),
                                createdTopic.configs().getPrimary()
                            );
                            logger.debugf(
                                    "Managed topic %s created. Resource: %s",
                                    newTopic.name(),
                                    managedTopic.get()
                            );
                            promise.complete(response);
                        }
                    })
                    .exceptionally(e -> {
                        if (e.getCause() instanceof UnknownTopicIdException) {
                            // Topic hasn't fully been created, check again
                            schedule(100);
                        } else {
                            promise.completeExceptionally(e);
                        }
                        return null;
                    });
            }
        }

        final TopicCreationCheck topicCheck = new TopicCreationCheck();
        final CompletableFuture<NewTopic> promise = topicCheck.promise();

        CompletableFuture
            .runAsync(() -> strimziService.createTopic(topicResource))
            .thenRunAsync(() -> topicCheck.schedule(0), threadContext.currentContextExecutor())
            .exceptionally(e -> {
                promise.completeExceptionally(e);
                return null;
            });

        return promise;
    }

    private CompletionStage<Topic> validate(Topic topic, TopicPatch patch) {
        return nodeService.listNodes()
                .thenApply(nodes -> validationService.validate(new TopicValidation.TopicPatchInputs(
                        nodes,
                        topic,
                        patch
                )))
                .thenApply(TopicValidation.TopicPatchInputs::topic);
    }

    private CompletionStage<Void> patchTopicValidated(Topic topic, TopicPatch patch, boolean validateOnly) {
        return topicDescribe.getManagedTopic(topic.name())
                .map(kafkaTopic -> patchManagedTopic(topic, patch, validateOnly, kafkaTopic))
                .orElseGet(() -> patchUnmanagedTopic(topic, patch, validateOnly));
    }

    private CompletionStage<Void> patchManagedTopic(Topic topic, TopicPatch patch, boolean validateOnly, KafkaTopic topicResource) {
        if (validateOnly) {
            return patchUnmanagedTopic(topic, patch, true);
        }

        KafkaTopic modifiedTopic = new KafkaTopicBuilder(topicResource)
            .editSpec()
                .withPartitions(patch.numPartitions())
                .withConfig(getModifiedTopicConfig(topicResource, patch))
                // Replica modification not supported
            .endSpec()
            .build();

        class TopicPatchCheck extends TopicCheck<Void> {
            TopicPatchCheck() {
                super(threadContext, scheduler);
            }

            @Override
            void pollTopic() {
                Optional<KafkaTopic> managedTopic = topicDescribe.getManagedTopic(topic.name());
                long generation = managedTopic.map(KafkaTopic::getMetadata).map(ObjectMeta::getGeneration).orElse(-1L);
                long observedGeneration = managedTopic.map(KafkaTopic::getStatus).map(Status::getObservedGeneration).orElse(-1L);

                if (observedGeneration == generation) {
                    promise.complete(null);
                } else {
                    schedule(100);
                }
            }
        }

        final TopicPatchCheck topicCheck = new TopicPatchCheck();
        final CompletableFuture<Void> promise = topicCheck.promise();

        CompletableFuture
            .runAsync(() -> strimziService.patchTopic(modifiedTopic))
            .thenRunAsync(() -> topicCheck.schedule(0), threadContext.currentContextExecutor())
            .exceptionally(e -> {
                promise.completeExceptionally(e);
                return null;
            });

        return promise;
    }

    private static Map<String, Object> getModifiedTopicConfig(KafkaTopic topicResource, TopicPatch patch) {
        Map<String, Object> topicConfig = Optional
                .ofNullable(topicResource.getSpec().getConfig())
                .orElseGet(Collections::emptyMap);
        Map<String, ConfigEntry> patchConfig = Optional
                .ofNullable(patch.configs())
                .orElseGet(Collections::emptyMap);
        Map<String, Object> modifiedConfig = new LinkedHashMap<>();

        // Updates and removals
        for (var config : topicConfig.entrySet()) {
            String configKey = config.getKey();

            if (patchConfig.containsKey(configKey)) {
                String patchValue = configValue(patchConfig.get(configKey));

                if (patchValue != null) {
                    modifiedConfig.put(configKey, patchValue);
                }
                // when the value is null, it is not added to the modifiedConfig. I.e. it's removed
            } else {
                // Passed through with no changes
                modifiedConfig.put(configKey, config.getValue());
            }
        }

        // Additions
        for (var config : patchConfig.entrySet()) {
            String configKey = config.getKey();
            String patchValue = configValue(config.getValue());

            if (patchValue != null && !topicConfig.containsKey(configKey)) {
                modifiedConfig.put(configKey, patchValue);
            }
        }

        return modifiedConfig;
    }

    private static String configValue(ConfigEntry entry) {
        return entry != null ? entry.getValue() : null;
    }

    private CompletionStage<Void> patchUnmanagedTopic(Topic topic, TopicPatch patch, boolean validateOnly) {
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

    private CompletableFuture<Void> maybeCreatePartitions(Topic topic, TopicPatch topicPatch, boolean validateOnly) {
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

    private CompletionStage<Void> createPartitions(String topicName, int totalCount, List<List<Integer>> newAssignments, boolean validateOnly) {
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

    private List<CompletableFuture<Void>> maybeAlterPartitionAssignments(Topic topic, TopicPatch topicPatch) {
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

    private void logPartitionReassignments(Topic topic,
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

    private CompletableFuture<Void> maybeAlterConfigs(Topic topic, TopicPatch topicPatch, boolean validateOnly) {
        return Optional.ofNullable(topicPatch.configs())
            .filter(Predicate.not(Map::isEmpty))
            .map(configs -> configService.alterConfigs(ConfigResource.Type.TOPIC, topic.name(), configs, validateOnly)
                .toCompletableFuture())
            .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    abstract static class TopicCheck<T> implements Runnable {
        private final ScheduledExecutorService scheduler;
        private final Runnable task;
        private final Instant limit = Instant.now().plusSeconds(TOPIC_OPERATION_LIMIT);
        protected final CompletableFuture<T> promise = new CompletableFuture<>();

        TopicCheck(ThreadContext threadContext, ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            this.task = threadContext.contextualRunnable(this);
        }

        @Override
        public void run() {
            try {
                pollTopic();
            } catch (Exception e) {
                promise.completeExceptionally(e);
            }
        }

        void schedule(long delayMs) {
            if (Instant.now().isAfter(limit)) {
                promise.completeExceptionally(new IllegalStateException(
                    "Operation timed out waiting for Strimzi to act on KafkaTopic"
                ));
            } else {
                scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
            }
        }

        CompletableFuture<T> promise() {
            return promise;
        }

        abstract void pollTopic();
    }
}
