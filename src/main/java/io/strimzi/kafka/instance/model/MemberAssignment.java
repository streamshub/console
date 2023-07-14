package io.strimzi.kafka.instance.model;

import java.util.List;

public class MemberAssignment {

    private List<TopicPartition> topicPartitions;

    public MemberAssignment() {
    }

    public MemberAssignment(List<TopicPartition> topicPartitions) {
        super();
        this.topicPartitions = topicPartitions;
    }

    public static MemberAssignment fromKafkaModel(org.apache.kafka.clients.admin.MemberAssignment assignment) {
        var topicPartitions = assignment.topicPartitions()
                .stream()
                .map(partition -> new TopicPartition(partition.topic(), partition.partition()))
                .toList();

        return new MemberAssignment(topicPartitions);
    }

    public List<TopicPartition> getTopicPartitions() {
        return topicPartitions;
    }

    public void setTopicPartitions(List<TopicPartition> topicPartitions) {
        this.topicPartitions = topicPartitions;
    }

}
