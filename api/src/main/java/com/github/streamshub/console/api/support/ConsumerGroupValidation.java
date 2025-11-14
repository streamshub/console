package com.github.streamshub.console.api.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ValueRange;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import jakarta.validation.Payload;

import org.apache.kafka.common.Uuid;

import com.github.streamshub.console.api.model.ConsumerGroup;
import com.github.streamshub.console.api.model.Either;
import com.github.streamshub.console.api.model.OffsetInfo;
import com.github.streamshub.console.api.model.PartitionInfo;
import com.github.streamshub.console.api.model.Topic;

/**
 * Collection of types responsible for validating the inputs to the
 * patchConsumerGroup operation.
 */
public class ConsumerGroupValidation {

    private static final String DATA = "data";
    private static final String ATTRIBUTES = "attributes";
    private static final String OFFSETS = "offsets";
    private static final String OFFSET_ENTRY = "offsetEntry";

    /**
     * Constraint specific to modified consumer groups. Triggers execution of
     * {@linkplain ConsumerGroupPatchValidator#isValid(ConsumerGroupPatchInputs, ConstraintValidatorContext)}.
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ConsumerGroupPatchValidator.class)
    @Documented
    public @interface ValidConsumerGroupPatch {
        String message() default ""; // Not used
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String source() default "";
    }

    /**
     * Container record for the inputs to a patchTopic operation
     */
    @ValidConsumerGroupPatch(payload = ErrorCategory.InvalidResource.class)
    public record ConsumerGroupPatchInputs(Map<Uuid, Either<Topic, Throwable>> topics, ConsumerGroup patch) { }

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

    public static class ConsumerGroupPatchValidator implements ConstraintValidator<ValidConsumerGroupPatch, ConsumerGroupPatchInputs> {

        @Override
        public boolean isValid(ConsumerGroupPatchInputs inputs, ConstraintValidatorContext context) {
            AtomicBoolean valid = new AtomicBoolean(true);

            validOffsetTopicPartitions(valid, inputs, context);

            return valid.get();
        }

        /**
         * Verify that:
         *
         * <ul>
         * <li>the requested offset topics exist/are accessible
         * <li>the requested offset partitions are valid for the associated topic
         * <li>the requested offsets are within range for the associated partition
         * </ul>
         *
         */
        void validOffsetTopicPartitions(AtomicBoolean valid, ConsumerGroupPatchInputs inputs, ConstraintValidatorContext context) {
            ConsumerGroup patch = inputs.patch();
            Map<Uuid, Either<Topic, Throwable>> topics = inputs.topics();
            AtomicInteger index = new AtomicInteger(0);

            Optional.ofNullable(patch.offsets())
                .orElseGet(Collections::emptyList)
                .stream()
                .forEach(offset -> {
                    int i = index.getAndIncrement();
                    String topicId = offset.topicId();
                    Either<Topic, Throwable> topicOrError = topics.get(Uuid.fromString(topicId));

                    if (topicOrError.isPrimaryEmpty()) {
                        valid.set(false);
                        Throwable cause = topicOrError.getAlternate();
                        String msg = cause.getMessage();
                        createBuilder(context, msg, DATA, ATTRIBUTES, OFFSETS)
                            .addContainerElementNode(OFFSET_ENTRY, List.class, 0)
                                .inIterable()
                                .atIndex(i)
                            .addPropertyNode("topicId")
                            .addConstraintViolation()
                            .disableDefaultConstraintViolation();
                    } else {
                        Topic topic = topicOrError.getPrimary();
                        var partitions = topic.partitions().getPrimary();
                        Integer partition = offset.partition();

                        if (partition != null && partition >= partitions.size()) {
                            valid.set(false);
                            String msg = "requested partition %d not valid for topic %s (name=%s) with %d partitions"
                                    .formatted(partition, topic.getId(), topic.name(), partitions.size());
                            createBuilder(context, msg, DATA, ATTRIBUTES, OFFSETS)
                                .addContainerElementNode(OFFSET_ENTRY, List.class, 0)
                                    .inIterable()
                                    .atIndex(i)
                                .addPropertyNode("partition")
                                .addConstraintViolation()
                                .disableDefaultConstraintViolation();

                            return;
                        }

                        offset.optionalAbsoluteOffset()
                            .ifPresent(absoluteOffset ->
                                verifyOffsetWithinRange(valid, i, topic, partition, absoluteOffset, partitions, context));
                    }
                });
        }

        void verifyOffsetWithinRange(AtomicBoolean valid, int offsetIndex, Topic topic, Integer partition, long offset, List<PartitionInfo> partitions, ConstraintValidatorContext context) {
            partitions.stream()
                .filter(p -> partition == null || p.getPartition() == partition)
                .forEach(partitionInfo -> {
                    long earliestOffset = getOffset(partitionInfo, KafkaOffsetSpec.EARLIEST).orElse(-1L);
                    long latestOffset = getOffset(partitionInfo, KafkaOffsetSpec.LATEST).orElse(Long.MAX_VALUE);
                    ValueRange offsetRange = ValueRange.of(earliestOffset, latestOffset);

                    if (!offsetRange.isValidValue(offset)) {
                        valid.set(false);
                        String msg = "requested offset %d must be between the earliest offset (%d) and the latest offset (%d) of topic %s (name=%s), partition %d"
                                .formatted(offset, earliestOffset, latestOffset, topic.getId(), topic.name(), partition);
                        createBuilder(context, msg, DATA, ATTRIBUTES, OFFSETS)
                            .addContainerElementNode(OFFSET_ENTRY, List.class, 0)
                                .inIterable()
                                .atIndex(offsetIndex)
                            .addPropertyNode("offset")
                            .addConstraintViolation()
                            .disableDefaultConstraintViolation();
                    }
                });
        }

        Optional<Long> getOffset(PartitionInfo partition, String offsetName) {
            return partition.getOffsets()
                .get(offsetName)
                .getOptionalPrimary()
                .map(OffsetInfo::offset);
        }
    }
}
