package io.strimzi.kafka.instance.model;

import java.util.List;

public class TopicPartitionInfo {

    int partition;
    Node leader;
    List<Node> replicas;
    List<Node> isr;

    public TopicPartitionInfo() {
    }

    public TopicPartitionInfo(int partition, Node leader, List<Node> replicas, List<Node> isr) {
        super();
        this.partition = partition;
        this.leader = leader;
        this.replicas = replicas;
        this.isr = isr;
    }

    public static TopicPartitionInfo fromKafkaModel(org.apache.kafka.common.TopicPartitionInfo info) {
        Node leader = Node.fromKafkaModel(info.leader());
        List<Node> replicas = info.replicas().stream().map(Node::fromKafkaModel).toList();
        List<Node> isr = info.isr().stream().map(Node::fromKafkaModel).toList();
        return new TopicPartitionInfo(info.partition(), leader, replicas, isr);
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public Node getLeader() {
        return leader;
    }

    public void setLeader(Node leader) {
        this.leader = leader;
    }

    public List<Node> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<Node> replicas) {
        this.replicas = replicas;
    }

    public List<Node> getIsr() {
        return isr;
    }

    public void setIsr(List<Node> isr) {
        this.isr = isr;
    }

}
