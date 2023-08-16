package com.github.eyefloaters.console.api.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Schema(name = "TopicAttributes")
@JsonFilter("fieldFilter")
public class Topic {

    public static final class Fields {
        public static final String NAME = "name";
        public static final String INTERNAL = "internal";
        public static final String PARTITIONS = "partitions";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String CONFIGS = "configs";

        public static final String LIST_DEFAULT = NAME + ", " + INTERNAL;
        public static final String DESCRIBE_DEFAULT =
                NAME + ", " + INTERNAL + ", " + PARTITIONS + ", " + AUTHORIZED_OPERATIONS;
    
        private Fields() {
        }
    }

    @Schema(name = "TopicListResponse")
    public static final class ListResponse extends DataListResponse<TopicResource> {
        public ListResponse(List<Topic> data) {
            super(data.stream().map(TopicResource::new).toList());
        }
    }

    @Schema(name = "TopicResponse")
    public static final class SingleResponse extends DataResponse<TopicResource> {
        public SingleResponse(Topic data) {
            super(new TopicResource(data));
        }
    }

    @Schema(name = "Topic")
    public static final class TopicResource extends Resource<Topic> {
        public TopicResource(Topic data) {
            super(data.id, "topics", data);
        }
    }

    String name;
    boolean internal;
    @JsonIgnore
    String id;

    @Schema(implementation = Object.class, oneOf = { PartitionInfo[].class, Error.class })
    Either<List<PartitionInfo>, Error> partitions;

    @Schema(implementation = Object.class, oneOf = { String[].class, Error.class })
    Either<List<String>, Error> authorizedOperations;

    @Schema(implementation = Object.class, oneOf = { ConfigEntry.ConfigEntryMap.class, Error.class })
    Either<Map<String, ConfigEntry>, Error> configs;

    public Topic() {
    }

    public Topic(String name, boolean internal, String id) {
        super();
        this.name = name;
        this.internal = internal;
        this.id = id;
    }

    public static Topic fromTopicListing(org.apache.kafka.clients.admin.TopicListing listing) {
        return new Topic(listing.name(), listing.isInternal(), listing.topicId().toString());
    }

    public static Topic fromTopicDescription(org.apache.kafka.clients.admin.TopicDescription description) {
        Topic topic = new Topic(description.name(), description.isInternal(), description.topicId().toString());

        topic.partitions = Either.of(description.partitions()
                .stream()
                .map(PartitionInfo::fromKafkaModel)
                .toList());

        topic.authorizedOperations = Either.of(Optional.ofNullable(description.authorizedOperations())
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null));

        return topic;
    }

    public void addPartitions(Either<Topic, Throwable> description) {
        if (description.isPrimaryPresent()) {
            partitions = description.getPrimary().partitions;
        } else {
            Error error = new Error(
                    "Unable to describe topic",
                    description.getAlternate().getMessage(),
                    description.getAlternate());
            partitions = Either.ofAlternate(error);
        }
    }

    public void addAuthorizedOperations(Either<Topic, Throwable> description) {
        if (description.isPrimaryPresent()) {
            authorizedOperations = description.getPrimary().authorizedOperations;
        } else {
            Error error = new Error(
                    "Unable to describe topic",
                    description.getAlternate().getMessage(),
                    description.getAlternate());
            authorizedOperations = Either.ofAlternate(error);
        }
    }

    public void addConfigs(Either<Map<String, ConfigEntry>, Throwable> configs) {
        if (configs.isPrimaryPresent()) {
            this.configs = Either.of(configs.getPrimary());
        } else {
            this.configs = Either.ofAlternate(new Error(
                    "Unable to describe topic configs",
                    configs.getAlternate().getMessage(),
                    configs.getAlternate()));
        }
    }

    public String getName() {
        return name;
    }

    public boolean isInternal() {
        return internal;
    }

    public Either<List<PartitionInfo>, Error> getPartitions() {
        return partitions;
    }

    public Either<List<String>, Error> getAuthorizedOperations() {
        return authorizedOperations;
    }

    public String getId() {
        return id;
    }

    public Either<Map<String, ConfigEntry>, Error> getConfigs() {
        return configs;
    }
}
