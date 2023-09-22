package com.github.eyefloaters.console.api.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.eyefloaters.console.api.support.KafkaOffsetSpec;

@JsonInclude(value = Include.NON_NULL)
public class PartitionInfo {

    final int partition;
    final List<PartitionReplica> replicas;
    final Integer leaderId;

    @Schema(implementation = Object.class, oneOf = { OffsetInfo.class, Error.class })
    private static final class OffsetInfoOrError {
    }

    @Schema(additionalProperties = OffsetInfoOrError.class)
    Map<String, Either<OffsetInfo, Error>> offsets;

    public PartitionInfo(int partition, List<PartitionReplica> replicas, Integer leaderId) {
        super();
        this.partition = partition;
        this.leaderId = leaderId;
        this.replicas = replicas;
    }

    public static PartitionInfo fromKafkaModel(org.apache.kafka.common.TopicPartitionInfo info) {
        List<Integer> isr = info.isr().stream().map(org.apache.kafka.common.Node::id).toList();
        List<PartitionReplica> replicas = info.replicas()
                .stream()
                .map(replica -> PartitionReplica.fromKafkaModel(replica, isr))
                .toList();
        return new PartitionInfo(info.partition(), replicas, info.leader().id());
    }

    static <P> Either<P, Error> primaryOrError(Either<P, Throwable> either, String message) {
        return either.ifPrimaryOrElse(
                Either::of,
                thrown -> Error.forThrowable(thrown, message));
    }

    public void addOffset(String key, Either<OffsetInfo, Throwable> offset) {
        if (this.offsets == null) {
            this.offsets = new LinkedHashMap<>(4);
        }

        this.offsets.put(key, primaryOrError(offset, "Unable to fetch partition offset"));
    }

    public void setReplicaLocalStorage(int nodeId, Either<ReplicaLocalStorage, Throwable> storage) {
        getReplica(nodeId).ifPresent(replica ->
            replica.localStorage(primaryOrError(storage, "Unable to fetch replica log metadata")));
    }

    public int getPartition() {
        return partition;
    }

    public Integer getLeaderId() {
        return leaderId;
    }

    public List<PartitionReplica> getReplicas() {
        return replicas;
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

    @JsonProperty
    @Schema(readOnly = true, description = """
            The total size of the log segments local to the leader replica, in bytes.
            """)
    public Long leaderLocalStorage() {
        return getReplica(leaderId)
            .map(PartitionReplica::localStorage)
            .filter(Objects::nonNull)
            .filter(Either::isPrimaryPresent)
            .map(Either::getPrimary)
            .map(ReplicaLocalStorage::size)
            .orElse(null);
    }

    Optional<Long> getOffset(String key) {
        return Optional.ofNullable(offsets.get(key))
            .flatMap(Either::getOptionalPrimary)
            .map(OffsetInfo::offset);
    }

    Optional<PartitionReplica> getReplica(int nodeId) {
        return replicas.stream()
            .filter(replica -> replica.nodeId() == nodeId)
            .findFirst();
    }
}
