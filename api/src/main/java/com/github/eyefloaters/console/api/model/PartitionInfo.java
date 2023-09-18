package com.github.eyefloaters.console.api.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.eyefloaters.console.api.support.KafkaOffsetSpec;

@JsonInclude(value = Include.NON_NULL)
public class PartitionInfo {

    final int partition;
    final List<Node> replicas;
    final Integer leader;
    final List<Integer> isr;

    @Schema(implementation = Object.class, oneOf = { OffsetInfo.class, Error.class })
    private static final class OffsetInfoOrError {
    }

    @Schema(additionalProperties = OffsetInfoOrError.class)
    Map<String, Either<OffsetInfo, Error>> offsets;

    public PartitionInfo(int partition, List<Node> replicas, Integer leader, List<Integer> isr) {
        super();
        this.partition = partition;
        this.leader = leader;
        this.replicas = replicas;
        this.isr = isr;
    }

    public static PartitionInfo fromKafkaModel(org.apache.kafka.common.TopicPartitionInfo info) {
        List<Node> replicas = info.replicas().stream().map(Node::fromKafkaModel).toList();
        Integer leader = info.leader().id();
        List<Integer> isr = info.isr().stream().map(org.apache.kafka.common.Node::id).toList();
        return new PartitionInfo(info.partition(), replicas, leader, isr);
    }

    static <P> Either<P, Error> primaryOrError(Either<P, Throwable> offset, String message) {
        return offset.ifPrimaryOrElse(
                Either::of,
                thrown -> Error.forThrowable(thrown, message));
    }

    public void addOffset(String key, Either<OffsetInfo, Throwable> offset) {
        if (this.offsets == null) {
            this.offsets = new LinkedHashMap<>(4);
        }

        this.offsets.put(key, primaryOrError(offset, "Unable to fetch partition offset"));
    }

    public void addReplicaInfo(int nodeId, Either<ReplicaInfo, Throwable> log) {
        replicas.stream()
            .filter(node -> node.getId() == nodeId)
            .findFirst()
            .ifPresent(node -> node.setLog(primaryOrError(log, "Unable to fetch replica log metadata")));
    }

    public int getPartition() {
        return partition;
    }

    public Integer getLeader() {
        return leader;
    }

    public List<Node> getReplicas() {
        return replicas;
    }

    public List<Integer> getIsr() {
        return isr;
    }

    public Map<String, Either<OffsetInfo, Error>> getOffsets() {
        return offsets;
    }

    /**
     * Calculates the record count as the latest offset minus the earliest offset
     * when both offsets are available only. When either the latest or the earliest
     * offset if not present, the record count is null.
     *
     * @return the record count for this partition
     */
    public Long getRecordCount() {
        return getOffset(KafkaOffsetSpec.LATEST)
            .map(latestOffset -> getOffset(KafkaOffsetSpec.EARLIEST)
                    .map(earliestOffset -> latestOffset - earliestOffset)
                    .orElse(null))
            .orElse(null);
    }

    public Long getSize() {
        return replicas.stream()
            .filter(r -> r.getId() == leader)
            .map(Node::getLog)
            .filter(Objects::nonNull)
            .filter(Either::isPrimaryPresent)
            .map(Either::getPrimary)
            .map(ReplicaInfo::size)
            .findFirst()
            .orElse(null);
    }

    Optional<Long> getOffset(String key) {
        return Optional.ofNullable(offsets.get(key))
            .flatMap(Either::getOptionalPrimary)
            .map(OffsetInfo::offset);
    }
}
