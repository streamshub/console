package io.strimzi.kafka.instance.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class Topic {

    String kind = "Topic";
    String name;
    boolean internal;
    String topicId;

    Either<List<TopicPartitionInfo>, Error> partitions;

    Either<List<String>, Error> authorizedOperations;

    Either<Map<String, ConfigEntry>, Error> configs;

    public Topic() {
    }

    public Topic(String name, boolean internal, String topicId) {
        super();
        this.name = name;
        this.internal = internal;
        this.topicId = topicId;
    }

    public static Topic fromTopicListing(org.apache.kafka.clients.admin.TopicListing listing) {
        return new Topic(listing.name(), listing.isInternal(), listing.topicId().toString());
    }

    public static Topic fromTopicDescription(org.apache.kafka.clients.admin.TopicDescription description) {
        Topic topic = new Topic(description.name(), description.isInternal(), description.topicId().toString());

        topic.partitions = Either.of(description.partitions()
                .stream()
                .map(TopicPartitionInfo::fromKafkaModel)
                .toList());

        topic.authorizedOperations = Either.of(Optional.ofNullable(description.authorizedOperations())
                .map(Collection::stream)
                .map(ops -> ops.map(Object::toString).toList())
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

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public boolean isInternal() {
        return internal;
    }

    public Either<List<TopicPartitionInfo>, Error> getPartitions() {
        return partitions;
    }

    public Either<List<String>, Error> getAuthorizedOperations() {
        return authorizedOperations;
    }

    public String getTopicId() {
        return topicId;
    }

    public Either<Map<String, ConfigEntry>, Error> getConfigs() {
        return configs;
    }
}
