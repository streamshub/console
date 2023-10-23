package com.github.eyefloaters.console.api.model;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.common.TopicPartition;

public record PartitionKey(String topicId, String topicName, int partition) {

    public static Map<TopicPartition, PartitionKey> keyMap(Collection<PartitionKey> keys) {
        return keys.stream().collect(Collectors.toMap(PartitionKey::toKafkaModel, Function.identity()));
    }

    public TopicPartition toKafkaModel() {
        return new TopicPartition(topicName, partition);
    }

}
