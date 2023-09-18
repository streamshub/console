package com.github.eyefloaters.console.api.model;

import java.util.Comparator;

public record TopicPartition(String topic, int partition) implements Comparable<TopicPartition> {

    static final Comparator<TopicPartition> COMPARATOR = Comparator.comparing(TopicPartition::topic)
            .thenComparingInt(TopicPartition::partition);

    @Override
    public int compareTo(TopicPartition other) {
        return COMPARATOR.compare(this, other);
    }

    public org.apache.kafka.common.TopicPartition toKafkaModel() {
        return new org.apache.kafka.common.TopicPartition(topic, partition);
    }
}
