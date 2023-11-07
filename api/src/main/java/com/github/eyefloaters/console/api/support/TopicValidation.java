package com.github.eyefloaters.console.api.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import jakarta.validation.Payload;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.ConfigEntry;
import com.github.eyefloaters.console.api.model.NewTopic;
import com.github.eyefloaters.console.api.model.PartitionInfo;
import com.github.eyefloaters.console.api.model.ReplicaAssignment;
import com.github.eyefloaters.console.api.model.Topic;
import com.github.eyefloaters.console.api.model.TopicPatch;

import io.strimzi.api.kafka.model.Kafka;

/**
 * Collection of types responsible for validating the inputs to the createTopic
 * and patchTopic operations.
 */
public class TopicValidation {

    private static final Logger LOGGER = Logger.getLogger(TopicValidation.class);
    private static final String DATA = "data";
    private static final String ATTRIBUTES = "attributes";
    private static final String REPLICAS_ASSIGNMENTS = "replicasAssignments";
    /**
     * Name for "virtual" property representing a map entry within the
     * replicasAssignments map in a create or patch topic request.
     */
    private static final String PARTITION_REPLICAS = "partitionReplicas";

    /**
     * Common constraint for both new and modified topics. Triggers execution of
     * {@linkplain CommonValidator#isValid(TopicModification, ConstraintValidatorContext)}.
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = CommonValidator.class)
    @Documented
    public @interface ValidTopic {
        String message() default ""; // Not used
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String source() default "";
    }

    /**
     * Constraint specific to modified topics. Triggers execution of
     * {@linkplain TopicPatchValidator#isValid(TopicPatchInputs, ConstraintValidatorContext)}.
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = TopicPatchValidator.class)
    @Documented
    public @interface ValidTopicPatch {
        String message() default ""; // Not used
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String source() default "";
    }

    /**
     * Common interface for working with both new and modified topics for
     * validation.
     */
    interface TopicModification extends ReplicaAssignment {
        Kafka kafkaCluster();

        /**
         * Name of the topic
         */
        String name();

        /**
         * Partition ID for the first new partition
         */
        int minNewPartitionId();

        /**
         * Partition ID for the last partition
         */
        int maxPartitionId();

        /**
         * The topic's replication factor, as determined by the number
         * of replica assignments of the first partition.
         */
        OptionalInt replicationFactor();

        /**
         * Configs being created or patched
         */
        Map<String, ConfigEntry> newConfigs();

        /**
         * Configs currently set for the topic, or the defaults for new topics, if
         * available.
         */
        Map<String, ConfigEntry> targetConfigs();
    }

    /**
     * Container record for the inputs to a createTopic operation
     */
    @ValidTopic(payload = ErrorCategory.InvalidResource.class)
    public record NewTopicInputs(
            Kafka kafkaCluster,
            Map<String, ConfigEntry> defaultConfigs,
            NewTopic topic
    ) implements TopicModification {

        @Override
        public String name() {
            return topic.name();
        }

        @Override
        public int minNewPartitionId() {
            return 0;
        }

        @Override
        public int maxPartitionId() {
            return topic.replicasAssignments().keySet().stream().mapToInt(Integer::parseInt).max()
                    .orElse(-1);
        }

        @Override
        public OptionalInt replicationFactor() {
            var firstAssignments = topic.replicaAssignment(0);
            return firstAssignments.isEmpty() ? OptionalInt.empty() : OptionalInt.of(firstAssignments.size());
        }

        @Override
        public Map<String, List<Integer>> replicasAssignments() {
            return topic.replicasAssignments();
        }

        @Override
        public Map<String, ConfigEntry> newConfigs() {
            return topic.configs();
        }

        @Override
        public Map<String, ConfigEntry> targetConfigs() {
            return defaultConfigs;
        }
    }

    /**
     * Container record for the inputs to a patchTopic operation
     */
    @ValidTopic(payload = ErrorCategory.InvalidResource.class)
    @ValidTopicPatch(payload = ErrorCategory.InvalidResource.class)
    public record TopicPatchInputs(
            Kafka kafkaCluster,
            Topic topic,
            TopicPatch patch
    ) implements TopicModification {

        @Override
        public String name() {
            return topic.name();
        }

        @Override
        public int minNewPartitionId() {
            return topic.partitions().getPrimary().size();
        }

        @Override
        public int maxPartitionId() {
            return Optional.ofNullable(patch.numPartitions())
                    .orElseGet(() -> topic.partitions().getPrimary().size()) - 1;
        }

        @Override
        public OptionalInt replicationFactor() {
            List<PartitionInfo> currentPartitions = topic.partitions().getPrimary();
            return OptionalInt.of(currentPartitions.get(0).getReplicas().size());
        }

        @Override
        public Map<String, List<Integer>> replicasAssignments() {
            return patch.replicasAssignments();
        }

        @Override
        public Map<String, ConfigEntry> newConfigs() {
            return patch.configs();
        }

        @Override
        public Map<String, ConfigEntry> targetConfigs() {
            return topic.configs().getOptionalPrimary().orElse(Collections.emptyMap());
        }
    }

    static NodeBuilderCustomizableContext createBuilder(ConstraintValidatorContext context,
            String message,
            String rootProperty,
            String... properties) {

        context.disableDefaultConstraintViolation();

        var customizableContext = context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(rootProperty);

        for (String property : properties) {
            customizableContext = customizableContext.addPropertyNode(property);
        }

        return customizableContext;
    }

    /**
     * Validations common to both new and existing topics
     */
    public static class CommonValidator implements ConstraintValidator<ValidTopic, TopicModification> {

        private static final String ASSIGNMENT_MISSING = "Assignments missing for new partition %d";
        private static final String REPLICATION_FACTOR_INCONSISTENT = "Inconsistent replication factor between partitions, partition 0 has %d while partition %d has replication factor %d";
        private static final String REPLICA_DUPLICATED = "Replica ID %d is duplicated";
        private static final String REPLICA_UNKNOWN = "Replica ID must be a known Kafka node ID (0 through %d); received %d";

        @Override
        public boolean isValid(TopicModification input, ConstraintValidatorContext context) {
            AtomicBoolean valid = new AtomicBoolean(true);

            if (input.replicasAssignments() != null) {
                validReplicasAssignments(valid, input, context);
                validReplicaIds(valid, input, context);
            }

            validConfigs(valid, input, context);

            return valid.get();
        }

        /**
         * Verifies that:
         * <ol>
         * <li>replica assignments are present for all new partitions when present for one of them
         * <li>number of replicas (i.e. replication factor) assigned to each new partition is consistent
         * </ol>
         */
        void validReplicasAssignments(AtomicBoolean valid, TopicModification input, ConstraintValidatorContext context) {
            int maxPartitionId = input.maxPartitionId();
            Set<Integer> withAssignment = new LinkedHashSet<>();
            Set<Integer> withoutAssignment = new LinkedHashSet<>();

            IntStream.rangeClosed(input.minNewPartitionId(), maxPartitionId)
                .forEach(partitionId -> {
                    if (input.hasReplicaAssignment(partitionId)) {
                        withAssignment.add(partitionId);
                    } else {
                        withoutAssignment.add(partitionId);
                    }
                });

            if (!withAssignment.isEmpty()) {
                withoutAssignment.forEach(partitionId -> {
                    valid.set(false);
                    String msg = ASSIGNMENT_MISSING.formatted(partitionId);
                    createBuilder(context, msg, DATA, ATTRIBUTES, REPLICAS_ASSIGNMENTS)
                        .addContainerElementNode(PARTITION_REPLICAS, Map.class, 1)
                            .inIterable()
                            .atKey(String.valueOf(partitionId))
                        .addConstraintViolation();
                });
            }

            input.replicationFactor().ifPresent(replicationFactor -> withAssignment.forEach(partitionId -> {
                int partitionReplicas = input.replicaAssignment(partitionId).size();

                if (partitionReplicas != replicationFactor) {
                    valid.set(false);
                    String msg = REPLICATION_FACTOR_INCONSISTENT.formatted(replicationFactor, partitionId, partitionReplicas);

                    createBuilder(context, msg, DATA, ATTRIBUTES, REPLICAS_ASSIGNMENTS)
                        .addContainerElementNode(PARTITION_REPLICAS, Map.class, 1)
                            .inIterable()
                            .atKey(String.valueOf(partitionId))
                        .addConstraintViolation();
                }
            }));
        }

        /**
         * Verifies that:
         * <ol>
         * <li>no replica is assigned more than once to the same partition
         * <li>no partition assigns an unknown Kafka node ID as a replica
         * </ol>
         */
        void validReplicaIds(AtomicBoolean valid, TopicModification input, ConstraintValidatorContext context) {
            int maxPartitionId = input.maxPartitionId();
            int nodeCount = input.kafkaCluster().getSpec().getKafka().getReplicas();

            IntStream.rangeClosed(0, maxPartitionId)
                .filter(input::hasReplicaAssignment)
                .forEach(partitionId -> {
                    var assignments = input.replicaAssignment(partitionId);
                    Set<Integer> uniqueAssignments = new HashSet<>();

                    for (int i = 0, m = assignments.size(); i < m; i++) {
                        Integer nodeId = assignments.get(i);

                        if (!uniqueAssignments.add(nodeId)) {
                            valid.set(false);
                            String msg = REPLICA_DUPLICATED.formatted(nodeId);
                            createBuilder(context, msg, DATA, ATTRIBUTES, REPLICAS_ASSIGNMENTS)
                                .addContainerElementNode(PARTITION_REPLICAS, Map.class, 1)
                                    .inIterable()
                                    .atKey(String.valueOf(partitionId))
                                .addContainerElementNode("nodeId", List.class, 0)
                                    .inIterable()
                                    .atIndex(i)
                                .addConstraintViolation();
                        }

                        if (nodeCount <= nodeId) {
                            valid.set(false);
                            String msg = REPLICA_UNKNOWN.formatted(nodeCount - 1, nodeId);
                            createBuilder(context, msg, DATA, ATTRIBUTES, REPLICAS_ASSIGNMENTS)
                                .addContainerElementNode(PARTITION_REPLICAS, Map.class, 1)
                                    .inIterable()
                                    .atKey(String.valueOf(partitionId))
                                .addContainerElementNode("nodeId", List.class, 0)
                                    .inIterable()
                                    .atIndex(i)
                                .addConstraintViolation();
                        }
                    }
                });
        }

        /**
         * Validate topic configuration properties using the currently-set configs (or
         * the defaults) as a basis for validation.
         *
         * <p>Verifies that:
         * <ol>
         * <li>configuration properties being set or deleted are known properties. This
         * is done by confirming the config is present in the current or default set.
         * <li>configuration properties being set are convertible to the target data
         * type
         * </ol>
         */
        void validConfigs(AtomicBoolean valid, TopicModification input, ConstraintValidatorContext context) {
            if (Optional.ofNullable(input.newConfigs()).map(Map::isEmpty).orElse(true)) {
                return;
            }

            var targetConfigs = input.targetConfigs();

            if (targetConfigs.isEmpty()) {
                LOGGER.infof("Current/target configurations could not be retrieved for topic %s in cluster %s, skipping local validation",
                        input.name(), input.kafkaCluster().getMetadata().getName());
                return;
            }

            input.newConfigs()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() !=  null)
                .forEach(entry -> {
                    String name = entry.getKey();
                    String value = entry.getValue().getValue();
                    ConfigEntry currentConfig = targetConfigs.get(name);

                    if (currentConfig == null) {
                        valid.set(false);
                        createBuilder(context, "Unknown configuration property", DATA, ATTRIBUTES, "configs")
                            .addContainerElementNode("configEntry", Map.class, 1)
                                .inIterable()
                                .atKey(name)
                            .addConstraintViolation();
                        return;
                    }

                    var type = ConfigDef.Type.valueOf(currentConfig.getType());

                    if (type == ConfigDef.Type.CLASS) {
                        // Convert class types to string to avoid parseType loading the class
                        type = ConfigDef.Type.STRING;
                    }

                    try {
                        ConfigDef.parseType(name, value, type);
                    } catch (ConfigException e) {
                        valid.set(false);
                        createBuilder(context, e.getMessage(), DATA, ATTRIBUTES, "configs")
                            .addContainerElementNode("configEntry", Map.class, 1)
                                .inIterable()
                                .atKey(name)
                            .addPropertyNode("value")
                            .addConstraintViolation();
                    }
                });
        }
    }

    public static class TopicPatchValidator implements ConstraintValidator<ValidTopicPatch, TopicPatchInputs> {

        @Override
        public boolean isValid(TopicPatchInputs inputs, ConstraintValidatorContext context) {
            AtomicBoolean valid = new AtomicBoolean(true);

            validTopicId(valid, inputs, context);

            if (validNumPartitions(valid, inputs, context)) {
                validPartitionIds(valid, inputs, context);
            }

            return valid.get();
        }

        /**
         * Verify that the topic ID in a patchTopic request body matches the ID in the
         * existing topic (retrieved using the URL).
         */
        void validTopicId(AtomicBoolean valid, TopicPatchInputs inputs, ConstraintValidatorContext context) {
            if (!Objects.equals(inputs.topic().getId(), inputs.patch().topicId())) {
                valid.set(false);
                context.buildConstraintViolationWithTemplate("resource ID conflicts with operation URL")
                    .addPropertyNode(DATA)
                    .addPropertyNode("id")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            }
        }

        /**
         * Verify that the requested total number of partitions is not less than the
         * number of partitions already created for the topic.
         */
        boolean validNumPartitions(AtomicBoolean valid, TopicPatchInputs inputs, ConstraintValidatorContext context) {
            Topic topic = inputs.topic();
            TopicPatch patch = inputs.patch();

            int currentNumPartitions = topic.partitions().getPrimary().size();
            int newNumPartitions = Optional.ofNullable(patch.numPartitions()).orElse(currentNumPartitions);

            if (newNumPartitions < currentNumPartitions) {
                valid.set(false);
                context.buildConstraintViolationWithTemplate("reducing number of partitions from %d to %d is not supported"
                        .formatted(currentNumPartitions, newNumPartitions))
                    .addPropertyNode(DATA)
                    .addPropertyNode(ATTRIBUTES)
                    .addPropertyNode("numPartitions")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
                return false;
            }

            return true;
        }

        /**
         * Verify that none of the partitions with replica assignments is for a
         * partition that does not exist or will not exist with the topic being resized.
         */
        void validPartitionIds(AtomicBoolean valid, TopicPatchInputs inputs, ConstraintValidatorContext context) {
            Topic topic = inputs.topic();
            TopicPatch patch = inputs.patch();

            int currentNumPartitions = topic.partitions().getPrimary().size();
            int newNumPartitions = Optional.ofNullable(patch.numPartitions()).orElse(currentNumPartitions);

            Optional.ofNullable(patch.replicasAssignments())
                .map(assignments -> assignments.keySet().stream())
                .orElseGet(Stream::empty)
                .mapToInt(Integer::parseInt)
                .filter(partitionId -> partitionId >= newNumPartitions)
                .forEach(partitionId -> {
                    valid.set(false);
                    context.buildConstraintViolationWithTemplate("partitionId must be less than the total number of partitions (%d); received %d"
                            .formatted(newNumPartitions, partitionId))
                        .addPropertyNode(DATA)
                        .addPropertyNode(ATTRIBUTES)
                        .addPropertyNode(REPLICAS_ASSIGNMENTS)
                        .addContainerElementNode(PARTITION_REPLICAS, Map.class, 1)
                            .inIterable()
                            .atKey(String.valueOf(partitionId))
                        .addConstraintViolation()
                        .disableDefaultConstraintViolation();
                });
        }
    }
}
