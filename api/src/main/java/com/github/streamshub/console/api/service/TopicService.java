package com.github.streamshub.console.api.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.api.kafka.model.kafka.Kafka;

import static org.apache.kafka.clients.admin.NewPartitions.increaseTo;

@ApplicationScoped
public class TopicService {

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
    KubernetesClient k8s;

    @Inject
    PermissionService permissionService;

    @Inject
    ConfigService configService;

    @Inject
    TopicDescribeService topicDescribe;

    public NewTopic createTopic(NewTopic topic, boolean validateOnly) {
        permissionService.assertPermitted(Topic.API_TYPE, Privilege.CREATE, topic.name());
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
                .toCompletionStage()
                .toCompletableFuture()
                .join();
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
        Kafka kafka = kafkaContext.resource();

        return describeTopic(topicId, List.of(Topic.Fields.CONFIGS), KafkaOffsetSpec.LATEST)
            .thenApply(topic -> validationService.validate(new TopicValidation.TopicPatchInputs(kafka, topic, patch)))
            .thenApply(TopicValidation.TopicPatchInputs::topic)
            .thenComposeAsync(topic -> topicDescribe.getManagedTopic(topic.name())
                    .map(kafkaTopic -> patchManagedTopic())
                    .orElseGet(() -> patchUnmanagedTopic(topic, patch, validateOnly)),
                threadContext.currentContextExecutor());
    }

    public CompletionStage<Void> deleteTopic(String topicId) {
        Admin adminClient = kafkaContext.admin();
        Uuid id = Uuid.fromString(topicId);

        return topicDescribe.topicNameForId(topicId).thenComposeAsync(topicName -> {
            if (topicName.isPresent()) {
                return adminClient.deleteTopics(TopicCollection.ofTopicIds(List.of(id)))
                        .topicIdValues()
                        .get(id)
                        .toCompletionStage();
            }

            throw new UnknownTopicIdException("No such topic: " + topicId);
        }, threadContext.currentContextExecutor());
    }

    // Modifications disabled for now
    private CompletionStage<Void> patchManagedTopic(/*KafkaTopic topic, TopicPatch patch, boolean validateOnly*/) {
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
}
