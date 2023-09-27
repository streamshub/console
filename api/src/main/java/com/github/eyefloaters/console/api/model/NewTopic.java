package com.github.eyefloaters.console.api.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eyefloaters.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;

import static com.github.eyefloaters.console.api.BlockingSupplier.get;

@Schema(name = "NewTopicAttributes")
@JsonInclude(Include.NON_NULL)
@Expression(
    when = "self.replicasAssignments != null",
    value = "self.numPartitions == null",
    message = "numPartitions may not be used when replicasAssignments is present",
    node = "numPartitions",
    payload = ErrorCategory.InvalidResource.class
)
@Expression(
    when = "self.replicasAssignments != null",
    value = "self.replicationFactor == null",
    message = "replicationFactor may not be used when replicasAssignments is present",
    node = "replicationFactor",
    payload = ErrorCategory.InvalidResource.class
)
public record NewTopic(
        @JsonProperty
        @NotNull(payload = ErrorCategory.InvalidResource.class)
        String name,

        @JsonIgnore
        String topicId,

        @JsonProperty
        @Positive(payload = ErrorCategory.InvalidResource.class)
        Integer numPartitions,

        @JsonProperty
        @Positive(payload = ErrorCategory.InvalidResource.class)
        Short replicationFactor,

        @JsonProperty
        Map<String,
            List<@Min(value = 0, payload = ErrorCategory.InvalidResource.class) Integer>> replicasAssignments,

        @JsonProperty
        Map<String, ConfigEntry> configs) {

    @Schema(name = "NewTopicDocument")
    public static final class NewTopicDocument extends DataResponse<NewTopicResource> {
        @JsonCreator
        public NewTopicDocument(@JsonProperty("data") NewTopicResource data) {
            super(data);
        }

        public NewTopicDocument(NewTopic data) {
            this(new NewTopicResource(data));
        }
    }

    @Schema(name = "NewTopic")
    @Expression(
        when = "self.type != null",
        value = "self.type == 'topics'",
        message = "resource type conflicts with operation",
        node = "type",
        payload = ErrorCategory.ResourceConflict.class
    )
    public static final class NewTopicResource extends Resource<NewTopic> {
        @JsonCreator
        public NewTopicResource(String type, NewTopic attributes) {
            super(null, type, attributes);
        }

        public NewTopicResource(NewTopic attributes) {
            super(attributes.topicId(), "topics", attributes);
        }
    }

    public static NewTopic fromKafkaModel(String topicName, CreateTopicsResult result) {
        return new NewTopic(
                topicName,
                get(() -> result.topicId(topicName)).toString(),
                get(() -> result.numPartitions(topicName)),
                get(() -> result.replicationFactor(topicName)).shortValue(),
                null,
                get(() -> result.config(topicName))
                    .entries()
                    .stream()
                    .collect(Collectors.toMap(
                            org.apache.kafka.clients.admin.ConfigEntry::name,
                            ConfigEntry::fromKafkaModel)));
    }

    // EL does not (yet) support record properties
    public Map<String, List<Integer>> getReplicasAssignments() {
        return replicasAssignments;
    }

    public Integer getNumPartitions() {
        return numPartitions;
    }

    public Short getReplicationFactor() {
        return replicationFactor;
    }
}
