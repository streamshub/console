package com.github.streamshub.console.api.model;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.common.TopicPartition;

public record PartitionId(String topicId, String topicName, int partition) {

    public static Map<TopicPartition, PartitionId> keyMap(Collection<PartitionId> keys) {
        return keys.stream().collect(Collectors.toMap(PartitionId::toKafkaModel, Function.identity()));
    }

    public PartitionId {
        Objects.requireNonNull(topicId);
        Objects.requireNonNull(topicName);
    }

    public TopicPartition toKafkaModel() {
        return new TopicPartition(topicName, partition);
    }

}
