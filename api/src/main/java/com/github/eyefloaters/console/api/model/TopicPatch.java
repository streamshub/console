package com.github.eyefloaters.console.api.model;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eyefloaters.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;

@Schema(name = "TopicPatchAttributes")
@JsonInclude(Include.NON_NULL)
public record TopicPatch(
        @JsonIgnore
        String topicId,

        @JsonProperty
        @Positive(payload = ErrorCategory.InvalidResource.class)
        Integer numPartitions,

        @JsonProperty
        @Size(min = 1, message = "must contain at least one entry", payload = ErrorCategory.InvalidResource.class)
        Map<@Pattern(regexp = "\\d+", message = "must be an integer", payload = ErrorCategory.InvalidResource.class)
            @DecimalMin(value = "0", payload = ErrorCategory.InvalidResource.class)
            String,
            // replica list may be empty (but not null), see Admin#alterPartitionReassignments
            @NotNull(payload = ErrorCategory.InvalidResource.class)
            List<@NotNull(payload = ErrorCategory.InvalidResource.class)
                 @Min(value = 0, payload = ErrorCategory.InvalidResource.class)
                 Integer>> replicasAssignments,

        @JsonProperty
        Map<String, ConfigEntry> configs
) implements ReplicaAssignment {

    @Schema(name = "TopicPatchDocument")
    public static final class TopicPatchDocument extends DataSingleton<TopicPatchResource> {
        @JsonCreator
        public TopicPatchDocument(@JsonProperty("data") TopicPatchResource data) {
            super(data);
        }
    }

    @Schema(name = "TopicPatch")
    @Expression(
        value = "self.id != null",
        message = "resource ID is required",
        node = "id",
        payload = ErrorCategory.InvalidResource.class)
    @Expression(
        when = "self.type != null",
        value = "self.type == 'topics'",
        message = "resource type conflicts with operation",
        node = "type",
        payload = ErrorCategory.ResourceConflict.class)
    public static final class TopicPatchResource extends Resource<TopicPatch> {
        @JsonCreator
        public TopicPatchResource(String id, String type, TopicPatch attributes) {
            super(id, type, new TopicPatch(id, attributes));
        }
    }

    private TopicPatch(String id, TopicPatch other) {
        this(id,
            other != null ? other.numPartitions : null,
            other != null ? other.replicasAssignments : null,
            other != null ? other.configs : null);
    }
}
