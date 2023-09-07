package com.github.eyefloaters.console.api.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class PartitionInfo {

    final int partition;
    final Node leader;
    final List<Node> replicas;
    final List<Node> isr;

    @Schema(implementation = Object.class, oneOf = { OffsetInfo.class, Error.class })
    private static final class OffsetInfoOrError {
    }

    @Schema(additionalProperties = OffsetInfoOrError.class)
    Map<String, Either<OffsetInfo, Error>> offsets;

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

    static Either<OffsetInfo, Error> offsetOrError(Either<OffsetInfo, Throwable> offset) {
        return offset.ifPrimaryOrElse(
                Either::of,
                thrown -> Error.forThrowable(thrown, "Unable to fetch partition offset"));
    }

    public void addOffset(String key, Either<OffsetInfo, Throwable> offset) {
        if (this.offsets == null) {
            this.offsets = new LinkedHashMap<>(4);
        }

        this.offsets.put(key, offsetOrError(offset));
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

    public Map<String, Either<OffsetInfo, Error>> getOffsets() {
        return offsets;
    }
}
