package com.github.eyefloaters.console.api.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class PartitionInfo {

    final int partition;
    final Node leader;
    final List<Node> replicas;
    final List<Node> isr;

    @Schema(implementation = Object.class, oneOf = { OffsetInfo.class, Error.class })
    Either<OffsetInfo, Error> offset;

    public PartitionInfo(int partition, Node leader, List<Node> replicas, List<Node> isr) {
        super();
        this.partition = partition;
        this.leader = leader;
        this.replicas = replicas;
        this.isr = isr;
    }

    public static PartitionInfo fromKafkaModel(org.apache.kafka.common.TopicPartitionInfo info) {
        Node leader = Node.fromKafkaModel(info.leader());
        List<Node> replicas = info.replicas().stream().map(Node::fromKafkaModel).toList();
        List<Node> isr = info.isr().stream().map(Node::fromKafkaModel).toList();
        return new PartitionInfo(info.partition(), leader, replicas, isr);
    }

    public void addOffset(Either<OffsetInfo, Throwable> offset) {
        if (offset.isPrimaryPresent()) {
            this.offset = Either.of(offset.getPrimary());
        } else {
            Error error = new Error(
                    "Unable to fetch partition offset",
                    offset.getAlternate().getMessage(),
                    offset.getAlternate());
            this.offset = Either.ofAlternate(error);
        }
    }

    public int getPartition() {
        return partition;
    }

    public Node getLeader() {
        return leader;
    }

    public List<Node> getReplicas() {
        return replicas;
    }

    public List<Node> getIsr() {
        return isr;
    }

    public Either<OffsetInfo, Error> getOffset() {
        return offset;
    }
}
